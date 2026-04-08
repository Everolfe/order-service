package com.github.everolfe.orderservice.controller;

import com.github.everolfe.orderservice.dto.StatusDto;
import com.github.everolfe.orderservice.dto.order.CreateOrderDto;
import com.github.everolfe.orderservice.dto.order.GetOrderDto;
import com.github.everolfe.orderservice.entity.Status;
import com.github.everolfe.orderservice.service.OrderService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetOrderDto> createOrder(@Valid @RequestBody CreateOrderDto order) {
        GetOrderDto createdOrder = orderService.create(order);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetOrderDto> getOrderById(@PathVariable Long id) {
        GetOrderDto getOrderDto = orderService.getOrderById(id);
        return new ResponseEntity<>(getOrderDto, HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetOrderDto>> getAllOrders(Pageable pageable) {
        Page<GetOrderDto> orders = orderService.getAllOrders(pageable);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping(params = "userId")
    public ResponseEntity<Page<GetOrderDto>> getOrdersByUserId(
            @RequestParam("userId") Long userId,
            Pageable pageable) {
        Page<GetOrderDto> orders = orderService.getOrdersByUserId(userId,pageable);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping(params = "statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetOrderDto>> getOrdersByStatuses(
            @RequestParam("statuses") List<Status> statuses,
            Pageable pageable) {
        Page<GetOrderDto> orders = orderService.getOrdersByStatus(statuses, pageable);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping(params = {"startDate","endDate"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetOrderDto>> getOrdersByCreationDate(
            @RequestParam(name = "startDate", required = false) LocalDateTime startDate,
            @RequestParam(name = "endDate", required = false) LocalDateTime endDate,
            Pageable pageable){
        Page<GetOrderDto> orders = orderService.getOrdersByCreationDate(startDate, endDate, pageable);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetOrderDto> changeStatus(
            @PathVariable("id") Long id,
            @RequestBody StatusDto status){
        GetOrderDto orderDto = orderService.updateOrderStatus(id, status);
        return new ResponseEntity<>(orderDto, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetOrderDto> updateOrder(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateOrderDto createOrderDto){
        GetOrderDto updatedOrder = orderService.updateOrder(id,createOrderDto);
        return new ResponseEntity<>(updatedOrder,HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") Long id){
        orderService.deleteOrder(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
