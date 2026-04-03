package com.github.everolfe.orderservice.dto.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GetUserDto(
        Long id,
        String name,
        String surname,
        LocalDate birthday,
        String email,
        Boolean active,
        List<GetPaymentCardDto> paymentCards,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
