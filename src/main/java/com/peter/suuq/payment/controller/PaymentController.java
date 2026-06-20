package com.peter.suuq.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.peter.suuq.order.dto.OrderResponse;
import com.peter.suuq.payment.dto.PaymentInitResponse;
import com.peter.suuq.payment.service.PaymentService;
import com.peter.suuq.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentInitResponse> initializePayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.initializePayment(user, orderId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody JsonNode payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify/{reference}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> verifyPayment(
            @AuthenticationPrincipal User user,
            @PathVariable String reference) {
        return ResponseEntity.ok(paymentService.verifyPayment(user, reference));
    }
}