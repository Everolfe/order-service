package com.github.everolfe.orderservice.service;

import com.github.everolfe.orderservice.dto.StatusDto;
import com.github.everolfe.orderservice.dto.order.CreateOrderDto;
import com.github.everolfe.orderservice.dto.order.GetOrderDto;
import com.github.everolfe.orderservice.entity.Status;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing orders.
 * Provides operations for creating, retrieving, updating, and soft-deleting orders.
 */
public interface OrderService {

    /**
     * Creates a new order.
     *
     * @param createOrderDto the DTO containing order creation data
     * @return the DTO of the created order
     */
    GetOrderDto create(CreateOrderDto createOrderDto);

    /**
     * Retrieves an order by its ID.
     *
     * @param id the ID of the order
     * @return the DTO of the order
     * @throws EntityNotFoundException if the order is not found or is deleted
     */
    GetOrderDto getOrderById(Long id);

    /**
     * Retrieves orders created within a specific date range.
     *
     * @param start the start date and time
     * @param end the end date and time
     * @param pageable pagination information
     * @return a page of order DTOs
     */
    Page<GetOrderDto> getOrdersByCreationDate(LocalDateTime start,
                                              LocalDateTime end,
                                              Pageable pageable);

    /**
     * Retrieves orders by user ID.
     *
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return a page of order DTOs
     */
    Page<GetOrderDto> getOrdersByUserId(Long userId, Pageable pageable);

    /**
     * Retrieves orders by their status.
     *
     * @param statuses the list of order statuses
     * @param pageable pagination information
     * @return a page of order DTOs
     */
    Page<GetOrderDto> getOrdersByStatus(List<Status> statuses, Pageable pageable);

    /**
     * Retrieves orders by statuses and creation date
     *
     * @param start start date and time
     * @param end end date and time
     * @param status list of order statuses
     * @return a page of order DTOs
     */
    Page<GetOrderDto> getOrdersByStatusAndCreationDate(
            LocalDateTime start,
            LocalDateTime end,
            List<Status> status,
            Pageable pageable);

    /**
     * Retrieves all orders with pagination.
     *
     * @param pageable pagination information
     * @return a page of order DTOs
     */
    Page<GetOrderDto> getAllOrders(Pageable pageable);

    /**
     * Updates the status of an order.
     *
     * @param id the ID of the order
     * @param status the new status
     * @return the DTO of the updated order
     * @throws EntityNotFoundException if the order is not found
     */
    GetOrderDto updateOrderStatus(Long id, StatusDto status);

    /**
     * Updates an existing order.
     *
     * @param id the ID of the order to update
     * @param createOrderDto the DTO containing updated order data
     * @return the DTO of the updated order
     * @throws EntityNotFoundException if the order is not found
     */
    GetOrderDto updateOrder(Long id, CreateOrderDto createOrderDto);

    /**
     * Soft-deletes an order by setting its deleted flag to true.
     *
     * @param id the ID of the order to delete
     * @return true if order successfully delete
     * @throws EntityNotFoundException if the order is not found or already deleted
     */
    boolean deleteOrder(Long id);
}