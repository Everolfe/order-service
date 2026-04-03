package com.github.everolfe.orderservice.service.client;

import com.github.everolfe.orderservice.dto.user.GetUserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserClientService {

    private final UserClient userClient;

    private static final String UNKNOWN_USER = "Unknown";
    private static final String USER_SERVICE = "userService";

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackGetUserByEmail")
    public GetUserDto getUserByEmail(String email) {
        return userClient.getUserByEmail(email);
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackGetUserById")
    public GetUserDto getUserById(Long id) {
        return userClient.getUserById(id);
    }

    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "fallbackGetAllById")
    public List<GetUserDto> getAllById(List<Long> ids) {
        return userClient.getAllById(ids);
    }

    private GetUserDto fallbackGetUserByEmail(String email) {
        return new GetUserDto(
                -1L,
                UNKNOWN_USER,
                UNKNOWN_USER,
                null,
                email,
                false,
                Collections.emptyList(),
                null,
                null
        );
    }

    private GetUserDto fallbackGetUserById() {
        return new GetUserDto(
                -1L,
                UNKNOWN_USER,
                UNKNOWN_USER,
                null,
                null,
                false,
                Collections.emptyList(),
                null,
                null
        );
    }

    private List<GetUserDto> fallbackGetAllById() {
        return Collections.emptyList();
    }
}