package com.github.everolfe.orderservice.service.impl;

import com.github.everolfe.orderservice.dao.ItemRepository;
import com.github.everolfe.orderservice.dao.OrderRepository;
import com.github.everolfe.orderservice.dao.OrderSpecification;
import com.github.everolfe.orderservice.dto.order.CreateOrderDto;
import com.github.everolfe.orderservice.dto.order.GetOrderDto;
import com.github.everolfe.orderservice.dto.orderitem.CreateOrderItemDto;
import com.github.everolfe.orderservice.entity.Item;
import com.github.everolfe.orderservice.entity.OrderItem;
import com.github.everolfe.orderservice.entity.Status;
import com.github.everolfe.orderservice.dto.user.GetUserDto;
import com.github.everolfe.orderservice.entity.Order;
import com.github.everolfe.orderservice.mapper.ordermapper.CreateOrderMapper;
import com.github.everolfe.orderservice.mapper.ordermapper.GetOrderWithoutUserMapper;
import com.github.everolfe.orderservice.service.OrderService;
import com.github.everolfe.orderservice.service.client.UserClientService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserClientService userClient;
    private final GetOrderWithoutUserMapper getOrderWithoutUserMapper;
    private final CreateOrderMapper createOrderMapper;

    @Override
    @Transactional
    public GetOrderDto create(CreateOrderDto createOrderDto) {
        GetUserDto getUserDto = userClient.getUserByEmail(createOrderDto.userEmail());
        if (getUserDto.id() == -1L) {
            throw new RuntimeException("User Service is unavailable, cannot create order");
        }

        Order order = new Order();
        order.setUserId(getUserDto.id());
        order.setStatus(Status.PENDING);

        long totalPrice = 0L;

        for(CreateOrderItemDto itemDto : createOrderDto.items()) {
            Item item = itemRepository
                    .findById(itemDto.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemDto.itemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setQuantity(itemDto.quantity());
            orderItem.setItem(item);
            orderItem.setOrder(order);
            order.getOrderItems().add(orderItem);


            long itemTotalCents = item.getPrice() * itemDto.quantity();
            totalPrice += itemTotalCents;
        }

        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);

        return new GetOrderDto(
                getOrderWithoutUserMapper.toDto(savedOrder),
                getUserDto
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GetOrderDto getOrderById(Long id) {
        Order order = orderRepository
                .findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Order not found" + id));
        GetUserDto getUserDto = userClient.getUserById(order.getUserId());
        return new GetOrderDto(
                getOrderWithoutUserMapper.toDto(order),
                getUserDto
        );
    }

    @Override
    @Transactional
    public Page<GetOrderDto> getOrdersByUserId(Long userId, Pageable pageable){
        GetUserDto getUserDto = userClient.getUserById(userId);
        if (getUserDto.id() == -1L) {
            throw new RuntimeException("User Service is unavailable, cannot create order");
        }
        Page<Order> orders = orderRepository.findActiveOrdersByUserId(getUserDto.id(), pageable);
        return mapOrdersToGetDtos(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetOrderDto> getOrdersByStatus(List<Status> statuses, Pageable pageable) {
        Specification<Order> spec = OrderSpecification.statusIn(statuses);

        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return mapOrdersToGetDtos(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetOrderDto> getOrdersByCreationDate(LocalDateTime start,
                                                     LocalDateTime end,
                                                     Pageable pageable) {
        Specification<Order> spec = OrderSpecification.createdBetween(start, end);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return mapOrdersToGetDtos(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetOrderDto> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return mapOrdersToGetDtos(orders);
    }

    @Override
    @Transactional
    public GetOrderDto updateOrderStatus(Long id, Status status) {
        Order order = orderRepository.findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        order.setStatus(status);

        Order savedOrder = orderRepository.save(order);

        GetUserDto getUserDto = userClient.getUserById(savedOrder.getUserId());

        return new GetOrderDto(
                getOrderWithoutUserMapper.toDto(savedOrder),
                getUserDto
        );
    }
    @Override
    @Transactional
    public GetOrderDto updateOrder(Long id, CreateOrderDto createOrderDto) {
        Order order = orderRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
        GetUserDto userDto = userClient.getUserById(order.getUserId());
        if(!userDto.email().equals(createOrderDto.userEmail())) {
            throw new IllegalArgumentException(
                    String.format("User email mismatch: order belongs to user with email %s, but update requested with email %s",
                    userDto.email(), createOrderDto.userEmail()));
        }
        createOrderMapper.merge(order,createOrderDto);
        long totalPriceCents = 0L;

        for (CreateOrderItemDto itemDto : createOrderDto.items()) {
            Item item = itemRepository
                    .findById(itemDto.itemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemDto.itemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setQuantity(itemDto.quantity());
            orderItem.setItem(item);
            orderItem.setOrder(order);
            order.getOrderItems().add(orderItem);

            long itemTotalCents = item.getPrice() * itemDto.quantity();
            totalPriceCents += itemTotalCents;
        }

        order.setTotalPrice(totalPriceCents);
        Order savedOrder = orderRepository.save(order);
        GetUserDto getUserDto = userClient.getUserById(savedOrder.getUserId());
        return new GetOrderDto(
                getOrderWithoutUserMapper.toDto(savedOrder),
                getUserDto
        );
    }

    @Override
    @Transactional
    public GetOrderDto deleteOrder(Long id) {
        Order order = orderRepository
                .findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        GetUserDto userDto = userClient.getUserById(order.getUserId());
        order.setDeleted(true);
        Order savedOrder = orderRepository.save(order);
        return new GetOrderDto(
                getOrderWithoutUserMapper.toDto(savedOrder),
                userDto
        );
    }

    private Page<GetOrderDto> mapOrdersToGetDtos(Page<Order> orders) {
        if (orders.isEmpty()) return Page.empty(orders.getPageable());

        List<GetUserDto> getUserDtos = userClient.getAllById(
                orders.getContent().stream()
                        .map(Order::getUserId)
                        .collect(Collectors.toList()));

        Map<Long, GetUserDto> getUserDtosMap = getUserDtos.stream()
                .collect(Collectors.toMap(
                        GetUserDto::id, dto -> dto
                ));

        return orders.map(order -> {
            return new GetOrderDto(
                    getOrderWithoutUserMapper.toDto(order),
                    getUserDtosMap.get(order.getUserId())
            );
        });
    }
}
