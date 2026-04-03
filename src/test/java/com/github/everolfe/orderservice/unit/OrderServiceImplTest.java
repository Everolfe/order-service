package com.github.everolfe.orderservice.unit;


import com.github.everolfe.orderservice.dao.ItemRepository;
import com.github.everolfe.orderservice.dao.OrderRepository;
import com.github.everolfe.orderservice.dto.item.GetItemDto;
import com.github.everolfe.orderservice.dto.order.CreateOrderDto;
import com.github.everolfe.orderservice.dto.order.GetOrderDto;
import com.github.everolfe.orderservice.dto.order.GetOrderDtoWithoutUser;
import com.github.everolfe.orderservice.dto.orderitem.CreateOrderItemDto;
import com.github.everolfe.orderservice.dto.orderitem.GetOrderItemDto;
import com.github.everolfe.orderservice.dto.user.GetPaymentCardDto;
import com.github.everolfe.orderservice.dto.user.GetUserDto;
import com.github.everolfe.orderservice.entity.Item;
import com.github.everolfe.orderservice.entity.Order;
import com.github.everolfe.orderservice.entity.Status;
import com.github.everolfe.orderservice.mapper.ordermapper.CreateOrderMapper;
import com.github.everolfe.orderservice.mapper.ordermapper.GetOrderWithoutUserMapper;
import com.github.everolfe.orderservice.service.client.UserClientService;
import com.github.everolfe.orderservice.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserClientService userClientService;

    @Mock
    private GetOrderWithoutUserMapper getOrderWithoutUserMapper;

    @Mock
    private CreateOrderMapper createOrderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @WithMockUser("ADMIN")
    void createOrder_success() {
        Order savedOrder = new Order();
        savedOrder.setId(1L);

        List<GetPaymentCardDto> cards = new ArrayList<>();

        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CreateOrderItemDto item = new CreateOrderItemDto(1L,1);

        List<CreateOrderItemDto> items = new ArrayList<>();
        items.add(item);

        CreateOrderDto createOrderDto = new CreateOrderDto(
                "email@gmail.com",
                items
        );

        Item itemEntity = new Item();
        itemEntity.setPrice(100L);

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetOrderItemDto orderItemDto = new GetOrderItemDto(
                1L,
                2,
                getItemDto,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Set<GetOrderItemDto> orderItemDtoSet = new HashSet<>();
        orderItemDtoSet.add(orderItemDto);
        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
            1L,
            "pending",
            BigDecimal.ONE,
            false,
                orderItemDtoSet
        );

        when(userClientService.getUserByEmail(createOrderDto.userEmail())).thenReturn(user);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(itemEntity));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(getOrderWithoutUserMapper.toDto(savedOrder)).thenReturn(getOrderDtoWithoutUser);

        GetOrderDto res = orderService.create(createOrderDto);

        assertAll(
                () -> Assertions.assertNotNull(res),
                () -> Assertions.assertEquals(getOrderDtoWithoutUser, res.getOrderDtoWithoutUser())
        );

        verify(userClientService, times(1)).getUserByEmail(createOrderDto.userEmail());
        verify(itemRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(getOrderWithoutUserMapper, times(1)).toDto(savedOrder);
    }


    @Test
    @WithMockUser("ADMIN")
    void createOrder_WithInvalidUserEmail_ThrowRuntimeException() {
        List<GetPaymentCardDto> cards = new ArrayList<>();

        GetUserDto user = new GetUserDto(
                -1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CreateOrderItemDto item = new CreateOrderItemDto(1L,1);

        List<CreateOrderItemDto> items = new ArrayList<>();
        items.add(item);

        CreateOrderDto createOrderDto = new CreateOrderDto(
                "email@gmail.com",
                items
        );

        when(userClientService.getUserByEmail(createOrderDto.userEmail())).thenReturn(user);

        assertThrows(ResourceAccessException.class, () -> orderService.create(createOrderDto));
        verify(userClientService,times(1)).getUserByEmail(createOrderDto.userEmail());
        verify(itemRepository,times(0)).save(any(Item.class));
    }


    @Test
    @WithMockUser("ADMIN")
    void createOrder_WithInvalidItemId_ThrowEntityNotFoundException() {
        List<GetPaymentCardDto> cards = new ArrayList<>();
        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        CreateOrderItemDto item = new CreateOrderItemDto(1L,1);
        List<CreateOrderItemDto> items = new ArrayList<>();
        items.add(item);
        CreateOrderDto createOrderDto = new CreateOrderDto(
                "email@gmail.com",
                items
        );
        when(userClientService.getUserByEmail(createOrderDto.userEmail())).thenReturn(user);
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.create(createOrderDto));

        verify(userClientService,times(1)).getUserByEmail(createOrderDto.userEmail());
        verify(itemRepository,times(0)).save(any(Item.class));
    }


    @Test
    @WithMockUser("ADMIN")
    void getOrderById_success() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        List<GetPaymentCardDto> cards = new ArrayList<>();
        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetOrderItemDto orderItemDto = new GetOrderItemDto(
                1L,
                2,
                getItemDto,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Set<GetOrderItemDto> orderItemDtoSet = new HashSet<>();
        orderItemDtoSet.add(orderItemDto);
        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "pending",
                BigDecimal.ONE,
                false,
                orderItemDtoSet
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userClientService.getUserById(order.getUserId())).thenReturn(user);
        when(getOrderWithoutUserMapper.toDto(order)).thenReturn(getOrderDtoWithoutUser);

        GetOrderDto res = orderService.getOrderById(1L);

        assertAll(
                () -> Assertions.assertNotNull(res),
                () -> Assertions.assertEquals(1L, res.getOrderDtoWithoutUser().id())
        );

        verify(userClientService,times(1)).getUserById(order.getUserId());
        verify(getOrderWithoutUserMapper,times(1)).toDto(order);
        verify(getOrderWithoutUserMapper,times(1)).toDto(order);
    }

    @Test
    @WithMockUser("ADMIN")
    void getOrderById_withInvalidIt_ThrowEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    @WithMockUser("ADMIN")
    void getOrdersByUserId_success() {
        Pageable pageable = PageRequest.of(0, 10);

        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);

        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        List<GetPaymentCardDto> cards = new ArrayList<>();
        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetOrderItemDto orderItemDto = new GetOrderItemDto(
                1L,
                2,
                getItemDto,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Set<GetOrderItemDto> orderItemDtoSet = new HashSet<>();
        orderItemDtoSet.add(orderItemDto);
        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "pending",
                BigDecimal.ONE,
                false,
                orderItemDtoSet
        );

        List<GetUserDto> users = new ArrayList<>();
        users.add(user);

        List userIds = new ArrayList();
        userIds.add(1L);

        when(userClientService.getUserById(1L)).thenReturn(user);
        when(orderRepository.findActiveOrdersByUserId(user.id(),pageable)).thenReturn(orderPage);
        when(userClientService.getAllById(userIds)).thenReturn(users);
        when(getOrderWithoutUserMapper.toDto(order)).thenReturn(getOrderDtoWithoutUser);

        Page<GetOrderDto> res = orderService.getOrdersByUserId(1L, pageable);
        assertAll(
                () -> Assertions.assertNotNull(res),
                () -> assertEquals(10 ,res.getSize()),
                () -> Assertions.assertEquals(1L, res.getContent().getFirst().getOrderDtoWithoutUser().id())
        );

        verify(userClientService,times(1)).getUserById(1L);
        verify(orderRepository,times(1)).findActiveOrdersByUserId(user.id(),pageable);
        verify(userClientService,times(1)).getAllById(userIds);
        verify(getOrderWithoutUserMapper,times(1)).toDto(order);
    }

    @Test
    @WithMockUser
    void getOrdersByUserId_withInvalidUserId_ThrowRuntimeException() {
        List<GetPaymentCardDto> cards = new ArrayList<>();
        GetUserDto user = new GetUserDto(
                -1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userClientService.getUserById(-1L)).thenReturn(user);
        Pageable pageable = PageRequest.of(0, 10);
        assertThrows(ResourceAccessException.class,
                () -> orderService.getOrdersByUserId(-1L,pageable ));
    }

    @Test
    @WithMockUser("ADMIN")
    void getOrdersByStatus_success() {
        Pageable pageable = PageRequest.of(0, 10);

        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Status.PENDING);

        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        List<GetPaymentCardDto> cards = new ArrayList<>();
        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetOrderItemDto orderItemDto = new GetOrderItemDto(
                1L,
                2,
                getItemDto,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Set<GetOrderItemDto> orderItemDtoSet = new HashSet<>();
        orderItemDtoSet.add(orderItemDto);
        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "pending",
                BigDecimal.ONE,
                false,
                orderItemDtoSet
        );

        List<GetUserDto> users = List.of(user);
        List<Long> userIds = List.of(1L);
        List<Status> statuses = List.of(Status.PENDING);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userClientService.getAllById(userIds)).thenReturn(users);
        when(getOrderWithoutUserMapper.toDto(any(Order.class))).thenReturn(getOrderDtoWithoutUser);

        Page<GetOrderDto> res = orderService.getOrdersByStatus(statuses, pageable);

        assertAll(
                () -> Assertions.assertNotNull(res),
                () -> assertEquals(10, res.getSize()),
                () -> Assertions.assertEquals(1L, res.getContent().getFirst().getOrderDtoWithoutUser().id())
        );

        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        verify(userClientService, times(1)).getAllById(userIds);
        verify(getOrderWithoutUserMapper, times(1)).toDto(any(Order.class));
    }

    @Test
    @WithMockUser("ADMIN")
    void getOrdersByCreationDate_success() {
        Pageable pageable = PageRequest.of(0, 10);

        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Status.PENDING);

        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        List<GetPaymentCardDto> cards = new ArrayList<>();
        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetOrderItemDto orderItemDto = new GetOrderItemDto(
                1L,
                2,
                getItemDto,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Set<GetOrderItemDto> orderItemDtoSet = new HashSet<>();
        orderItemDtoSet.add(orderItemDto);
        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "pending",
                BigDecimal.ONE,
                false,
                orderItemDtoSet
        );

        List<GetUserDto> users = List.of(user);
        List<Long> userIds = List.of(1L);


        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);
        when(userClientService.getAllById(userIds)).thenReturn(users);
        when(getOrderWithoutUserMapper.toDto(any(Order.class))).thenReturn(getOrderDtoWithoutUser);

        Page<GetOrderDto> res = orderService.getOrdersByCreationDate(
                LocalDateTime.now(), LocalDateTime.MAX, pageable);

        assertAll(
                () -> Assertions.assertNotNull(res),
                () -> assertEquals(10, res.getSize()),
                () -> Assertions.assertEquals(1L, res.getContent().getFirst().getOrderDtoWithoutUser().id())
        );

        verify(orderRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        verify(userClientService, times(1)).getAllById(userIds);
        verify(getOrderWithoutUserMapper, times(1)).toDto(any(Order.class));
    }

    @Test
    @WithMockUser("ADMIN")
    void getAllOrders_success() {
        Pageable pageable = PageRequest.of(0, 10);

        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Status.PENDING);

        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        List<GetPaymentCardDto> cards = new ArrayList<>();
        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetOrderItemDto orderItemDto = new GetOrderItemDto(
                1L,
                2,
                getItemDto,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Set<GetOrderItemDto> orderItemDtoSet = new HashSet<>();
        orderItemDtoSet.add(orderItemDto);
        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "pending",
                BigDecimal.ONE,
                false,
                orderItemDtoSet
        );

        List<GetUserDto> users = List.of(user);
        List<Long> userIds = List.of(1L);


        when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        when(userClientService.getAllById(userIds)).thenReturn(users);
        when(getOrderWithoutUserMapper.toDto(any(Order.class))).thenReturn(getOrderDtoWithoutUser);

        Page<GetOrderDto> res = orderService.getAllOrders(pageable);

        assertAll(
                () -> Assertions.assertNotNull(res),
                () -> assertEquals(10, res.getSize()),
                () -> Assertions.assertEquals(1L, res.getContent().getFirst().getOrderDtoWithoutUser().id())
        );

        verify(orderRepository, times(1)).findAll( pageable);
        verify(userClientService, times(1)).getAllById(userIds);
        verify(getOrderWithoutUserMapper, times(1)).toDto(any(Order.class));
    }

    @Test
    @WithMockUser("ADMIN")
    void updateOrderStatus_success() {

        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Status.PENDING);
        order.setTotalPrice(10000L);

        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                new ArrayList<>(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setUserId(1L);
        updatedOrder.setStatus(Status.PROCESSING);
        updatedOrder.setTotalPrice(10000L);

        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "processing",
                BigDecimal.valueOf(100.00),
                false,
                Set.of()
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(userClientService.getUserById(updatedOrder.getUserId())).thenReturn(user);
        when(getOrderWithoutUserMapper.toDto(updatedOrder)).thenReturn(getOrderDtoWithoutUser);

        GetOrderDto result = orderService.updateOrderStatus(1L, Status.PROCESSING);

        assertAll(
                () -> Assertions.assertNotNull(result),
                () -> assertEquals(Status.PROCESSING.name().toLowerCase(),
                        result.getOrderDtoWithoutUser().status())
        );

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userClientService, times(1)).getUserById(1L);
        verify(getOrderWithoutUserMapper, times(1)).toDto(updatedOrder);
    }

    @Test
    @WithMockUser("ADMIN")
    void updateOrderStatus_withInvalidId_throwEntityNotFoundException(){
        assertThrows(EntityNotFoundException.class, () -> orderService.updateOrderStatus(1L, Status.PENDING));
    }

    @Test
    @WithMockUser("ADMIN")
    void updateOrder_success() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Status.PENDING);
        order.setTotalPrice(10000L);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(1L);
        List<GetPaymentCardDto> cards = new ArrayList<>();

        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CreateOrderItemDto item = new CreateOrderItemDto(1L, 1);

        List<CreateOrderItemDto> items = new ArrayList<>();
        items.add(item);

        CreateOrderDto createOrderDto = new CreateOrderDto(
                "email@gmail.com",
                items
        );

        Item itemEntity = new Item();
        itemEntity.setPrice(100L);
        itemEntity.setId(1L);

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        GetOrderItemDto orderItemDto = new GetOrderItemDto(
                1L,
                2,
                getItemDto,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Set<GetOrderItemDto> orderItemDtoSet = new HashSet<>();
        orderItemDtoSet.add(orderItemDto);

        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "pending",
                BigDecimal.ONE,
                false,
                orderItemDtoSet
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        when(userClientService.getUserById(order.getUserId())).thenReturn(user);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(itemEntity));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(getOrderWithoutUserMapper.toDto(savedOrder)).thenReturn(getOrderDtoWithoutUser);

        GetOrderDto res = orderService.updateOrder(1L, createOrderDto);

        assertAll(
                () -> Assertions.assertNotNull(res),
                () -> Assertions.assertEquals(getOrderDtoWithoutUser, res.getOrderDtoWithoutUser()),
                () -> Assertions.assertEquals(user, res.getUserDto())
        );

        verify(userClientService, times(2)).getUserById(order.getUserId());
        verify(itemRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(getOrderWithoutUserMapper, times(1)).toDto(savedOrder);
    }


    @Test
    @WithMockUser("ADMIN")
    void updateOrder_withInvalidID_throwEntityNotFoundException(){

        CreateOrderItemDto item = new CreateOrderItemDto(1L,1);

        List<CreateOrderItemDto> items = new ArrayList<>();
        items.add(item);

        CreateOrderDto createOrderDto = new CreateOrderDto(
                "email@gmail.com",
                items
        );

        assertThrows(EntityNotFoundException.class, () -> orderService.updateOrder(1L, createOrderDto));
    }


    @Test
    @WithMockUser("ADMIN")
    void updateOrder_withDTO_throwIllegalArgumentException(){
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Status.PENDING);
        order.setTotalPrice(10000L);

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(1L);
        List<GetPaymentCardDto> cards = new ArrayList<>();

        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email1@gmail.com",
                true,
                cards,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        CreateOrderItemDto item = new CreateOrderItemDto(1L, 1);

        List<CreateOrderItemDto> items = new ArrayList<>();
        items.add(item);

        CreateOrderDto createOrderDto = new CreateOrderDto(
                "email@gmail.com",
                items
        );

        Item itemEntity = new Item();
        itemEntity.setPrice(100L);
        itemEntity.setId(1L);

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "name",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );



        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userClientService.getUserById(1L)).thenReturn(user);
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(1L, createOrderDto));
    }

    @Test
    @WithMockUser("ADMIN")
    void deleteOrder_success(){

        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(Status.PENDING);
        order.setTotalPrice(10000L);
        order.setDeleted(false);

        GetUserDto user = new GetUserDto(
                1L,
                "Test",
                "TestN",
                LocalDate.now(),
                "email@gmail.com",
                true,
                new ArrayList<>(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setUserId(1L);
        updatedOrder.setStatus(Status.PROCESSING);
        updatedOrder.setTotalPrice(10000L);
        updatedOrder.setDeleted(true);

        GetOrderDtoWithoutUser getOrderDtoWithoutUser = new GetOrderDtoWithoutUser(
                1L,
                "processing",
                BigDecimal.valueOf(100.00),
                true,
                Set.of()
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(userClientService.getUserById(updatedOrder.getUserId())).thenReturn(user);
        when(getOrderWithoutUserMapper.toDto(updatedOrder)).thenReturn(getOrderDtoWithoutUser);

        GetOrderDto result = orderService.updateOrderStatus(1L, Status.PROCESSING);

        assertAll(
                () -> Assertions.assertNotNull(result),
                () -> Assertions.assertTrue(
                        result.getOrderDtoWithoutUser().deleted())
        );

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userClientService, times(1)).getUserById(1L);
        verify(getOrderWithoutUserMapper, times(1)).toDto(updatedOrder);
    }

    @Test
    @WithMockUser
    void deleteOrder_withInvalidID_throwEntityNotFoundException(){
        assertThrows(EntityNotFoundException.class, () -> orderService.deleteOrder(1L));
    }
}
