package com.github.everolfe.orderservice.service.client;

import com.github.everolfe.orderservice.config.FeignClientConfig;
import com.github.everolfe.orderservice.dto.user.GetUserDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        url = "${user.service.url}",
        path = "/api/users",
        configuration = FeignClientConfig.class
)
public interface UserClient {

    @GetMapping("/email")
    GetUserDto getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/{id}")
    GetUserDto getUserById(@PathVariable("id") Long id);

    @PostMapping("/batch/id")
    List<GetUserDto> getAllById(@RequestBody List<Long> ids);
}
