package com.github.everolfe.orderservice.service.impl;

import com.github.everolfe.orderservice.dao.ItemRepository;
import com.github.everolfe.orderservice.dao.OrderRepository;
import com.github.everolfe.orderservice.dao.OrderSpecification;
import com.github.everolfe.orderservice.dto.StatusDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;

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
            throw new ResourceAccessException("User Service is unavailable, cannot create order");
        }

        Order order = new Order();
        order.setUserId(getUserDto.id());
        order.setStatus(Status.PENDING);

        List<OrderItem> orderItems = processOrderItems(order, createOrderDto.items());
        order.getOrderItems().addAll(orderItems);
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
            throw new ResourceAccessException("User Service is unavailable, cannot create order");
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
    public Page<GetOrderDto> getOrdersByStatusAndCreationDate(
            LocalDateTime start,
            LocalDateTime end,
            List<Status> statuses,
            Pageable pageable
    ){
        Specification<Order> spec = OrderSpecification.combineFilters(
                start,end,statuses
        );
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
    public GetOrderDto updateOrderStatus(Long id, StatusDto statusDto) {
        Status status = statusDto.status();
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

        List<OrderItem> orderItems = processOrderItems(order, createOrderDto.items());
        order.getOrderItems().addAll(orderItems);
        Order savedOrder = orderRepository.save(order);
        GetUserDto getUserDto = userClient.getUserById(savedOrder.getUserId());
        return new GetOrderDto(
                getOrderWithoutUserMapper.toDto(savedOrder),
                getUserDto
        );
    }

    @Override
    @Transactional
    public boolean deleteOrder(Long id) {
        Order order = orderRepository
                .findById(id)
                .filter(o -> !o.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        order.setDeleted(true);
        orderRepository.save(order);
        return true;
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

        return orders.map(order -> new GetOrderDto(
                getOrderWithoutUserMapper.toDto(order),
                getUserDtosMap.get(order.getUserId())
        ));
    }
    private List<OrderItem> processOrderItems(Order order, List<CreateOrderItemDto> itemsDto) {
        List<Long> itemIds = itemsDto.stream()
                .map(CreateOrderItemDto::itemId)
                .toList();

        List<Item> items = itemRepository.findAllById(itemIds);

        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, Function.identity()));

        long totalPrice = 0L;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderItemDto itemDto : itemsDto) {
            Long itemId = itemDto.itemId();
            Item item = itemMap.get(itemId);

            if (item == null) {
                throw new EntityNotFoundException("Item not found: " + itemId);
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setQuantity(itemDto.quantity());
            orderItem.setItem(item);
            orderItem.setOrder(order);
            orderItems.add(orderItem);

            totalPrice += item.getPrice() * itemDto.quantity();
        }

        order.setTotalPrice(totalPrice);
        return orderItems;
    }
}
