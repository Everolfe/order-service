package com.github.everolfe.orderservice.dto.event;

public record PaymentEventDto(
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status
) {}
