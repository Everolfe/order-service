package com.github.everolfe.orderservice.dto.user;

import java.time.LocalDateTime;

public record GetPaymentCardDto(
        Long id,
        String number,
        String holder,
        String expirationDate,
        Boolean active,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) { }
