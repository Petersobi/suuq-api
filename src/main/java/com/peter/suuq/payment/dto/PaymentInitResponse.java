package com.peter.suuq.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponse {
    private String authorizationUrl;
    private String reference;
    private String orderId;
}