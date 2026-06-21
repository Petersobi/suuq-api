package com.peter.suuq.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.peter.suuq.exception.AccessDeniedException;
import com.peter.suuq.exception.BadRequestException;
import com.peter.suuq.exception.PaymentException;
import com.peter.suuq.exception.ResourceNotFoundException;
import com.peter.suuq.order.dto.OrderResponse;
import com.peter.suuq.order.entity.Order;
import com.peter.suuq.order.entity.OrderStatus;
import com.peter.suuq.order.repository.OrderRepository;
import com.peter.suuq.order.service.OrderService;
import com.peter.suuq.payment.dto.PaymentInitResponse;
import com.peter.suuq.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final WebClient.Builder webClientBuilder;

    @Value("${paystack.secret.key}")
    private String paystackSecretKey;

    @Value("${paystack.base.url}")
    private String paystackBaseUrl;

    @Transactional
    public PaymentInitResponse initializePayment(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in a payable state");
        }

        // Convert to kobo (Paystack expects amount in smallest currency unit)
        BigDecimal amountInKobo = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100));

        Map<String, Object> body = new HashMap<>();
        body.put("email", user.getEmail());
        body.put("amount", amountInKobo.longValue());
        body.put("reference", "SUUQ-" + orderId + "-" + System.currentTimeMillis());

        JsonNode response = webClientBuilder.build()
                .post()
                .uri(paystackBaseUrl + "/transaction/initialize")
                .header("Authorization", "Bearer " + paystackSecretKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.path("status").asBoolean()) {
            throw new PaymentException("Failed to initialize payment");
        }

        JsonNode data = response.path("data");
        String reference = data.path("reference").asText();
        String authorizationUrl = data.path("authorization_url").asText();

        order.setPaystackReference(reference);
        orderRepository.save(order);

        return new PaymentInitResponse(
                authorizationUrl,
                reference,
                orderId.toString()
        );
    }

    @Transactional
    public void handleWebhook(JsonNode payload) {
        String event = payload.path("event").asText();

        if (!"charge.success".equals(event)) {
            return;
        }

        String reference = payload.path("data")
                .path("reference").asText();

        orderService.updateOrderAfterPayment(reference);
    }
    @Transactional
    public OrderResponse verifyPayment(User user, String reference) {
        JsonNode response = webClientBuilder.build()
                .get()
                .uri(paystackBaseUrl + "/transaction/verify/" + reference)
                .header("Authorization", "Bearer " + paystackSecretKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.path("status").asBoolean()) {
            throw new PaymentException("Failed to verify payment");
        }

        JsonNode data = response.path("data");
        String status = data.path("status").asText();

        if (!"success".equals(status)) {
            throw new PaymentException("Payment not successful, status: " + status);
        }

        Order order = orderRepository.findByPaystackReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        return orderService.getOrderById(user, order.getId());
    }
}