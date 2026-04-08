package com.github.everolfe.orderservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.everolfe.orderservice.dao.ItemRepository;
import com.github.everolfe.orderservice.dao.OrderRepository;
import com.github.everolfe.orderservice.dto.item.CreateItemDto;
import com.github.everolfe.orderservice.dto.item.GetItemDto;
import com.github.everolfe.orderservice.dto.order.CreateOrderDto;
import com.github.everolfe.orderservice.dto.StatusDto;
import com.github.everolfe.orderservice.dto.order.GetOrderDto;
import com.github.everolfe.orderservice.dto.orderitem.CreateOrderItemDto;
import com.github.everolfe.orderservice.dto.orderitem.GetOrderItemDto;
import com.github.everolfe.orderservice.entity.Status;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderIntegrationTest extends BaseIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @LocalServerPort
    protected int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "/api/orders";
        orderRepository.deleteAll();
        itemRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createOrder_AsAdmin_ShouldReturnCreatedOrder() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("99.99"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 2);
        CreateOrderDto createOrderDto = new CreateOrderDto(
                "test@example.com",
                List.of(orderItemDto)
        );

        MvcResult result = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderDto)))
                .andExpect(status().isCreated())
                .andReturn();

        GetOrderDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GetOrderDto.class
        );

        assertThat(response.getOrderDtoWithoutUser().id()).isNotNull();
        assertThat(response.getOrderDtoWithoutUser().status()).isEqualTo(Status.PENDING.name());
        assertThat(response.getOrderDtoWithoutUser().totalPrice())
                .isEqualByComparingTo(new BigDecimal("199.98"));
        assertThat(response.getOrderDtoWithoutUser().deleted()).isFalse();
        assertThat(response.getOrderDtoWithoutUser().orderItems()).hasSize(1);

        GetOrderItemDto orderItem = response.getOrderDtoWithoutUser().orderItems().iterator().next();
        assertThat(orderItem.quantity()).isEqualTo(2);
        assertThat(orderItem.item().id()).isEqualTo(item.id());
        assertThat(orderItem.item().name()).isEqualTo("Test Item");
        assertThat(orderItem.item().price()).isEqualByComparingTo(new BigDecimal("99.99"));

        assertThat(response.getUserDto().email()).isEqualTo("test@example.com");
    }


    @Test
    void getOrderById_ShouldReturnOrder() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("99.99"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto))
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 1);
        CreateOrderDto createOrderDto = new CreateOrderDto(
                "test@example.com",
                List.of(orderItemDto)
        );

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderDto))
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isCreated())
                .andReturn();

        GetOrderDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetOrderDto.class
        );
        Long orderId = created.getOrderDtoWithoutUser().id();

        mockMvc.perform(get(baseUrl + "/{id}", orderId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.getOrderDtoWithoutUser.id").value(orderId))
                .andExpect(jsonPath("$.getOrderDtoWithoutUser.status").value(Status.PENDING.name()))
                .andExpect(jsonPath("$.getUserDto.email").value("test@example.com"));
    }

    @Test
    void getOrderById_WhenNotFound_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get(baseUrl + "/{id}", 99999L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getAllOrders_AsAdmin_ShouldReturnPageOfOrders() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("50.00"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 1);

        for (int i = 1; i <= 3; i++) {
            CreateOrderDto createOrderDto = new CreateOrderDto(
                    "test" + i + "@example.com",
                    List.of(orderItemDto)
            );
            mockMvc.perform(post(baseUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createOrderDto)))
                    .andExpect(status().isCreated());
        }

        MvcResult result = mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseJson);

        JsonNode contentNode = root.get("content");
        List<GetOrderDto> orders = objectMapper.readValue(
                contentNode.toString(),
                new com.fasterxml.jackson.core.type.TypeReference<List<GetOrderDto>>() {}
        );

        long totalElements = root.get("totalElements").asLong();
        int number = root.get("number").asInt();
        int size = root.get("size").asInt();

        assertThat(orders).hasSize(3);
        assertThat(totalElements).isEqualTo(3);
        assertThat(number).isZero();
        assertThat(size).isEqualTo(10);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getOrdersByUserId_ShouldReturnOrders() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("30.00"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 2);

        for (int i = 1; i <= 2; i++) {
            CreateOrderDto createOrderDto = new CreateOrderDto(
                    "test@example.com",
                    List.of(orderItemDto)
            );
            mockMvc.perform(post(baseUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createOrderDto)))
                    .andExpect(status().isCreated());
        }

        MvcResult result = mockMvc.perform(get(baseUrl)
                        .param("userId", "1")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode contentNode = root.get("content");
        List<GetOrderDto> orders = objectMapper.readValue(
                contentNode.toString(),
                new com.fasterxml.jackson.core.type.TypeReference<List<GetOrderDto>>() {}
        );

        assertThat(orders).hasSize(2);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateOrderStatus_AsAdmin_ShouldUpdateStatus() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("100.00"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 1);
        CreateOrderDto createOrderDto = new CreateOrderDto(
                "test@example.com",
                List.of(orderItemDto)
        );

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderDto)))
                .andExpect(status().isCreated())
                .andReturn();

        GetOrderDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetOrderDto.class
        );
        Long orderId = created.getOrderDtoWithoutUser().id();

        StatusDto statusDto = new StatusDto(Status.PROCESSING);

        mockMvc.perform(patch(baseUrl + "/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.getOrderDtoWithoutUser.status")
                        .value(Status.PROCESSING.name()));

       }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void deleteOrder_AsAdmin_ShouldSoftDeleteOrder() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("100.00"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 1);
        CreateOrderDto createOrderDto = new CreateOrderDto(
                "test@example.com",
                List.of(orderItemDto)
        );

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderDto)))
                .andExpect(status().isCreated())
                .andReturn();

        GetOrderDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetOrderDto.class
        );
        Long orderId = created.getOrderDtoWithoutUser().id();

        MvcResult deleteResult = mockMvc.perform(delete(baseUrl + "/{id}", orderId))
                .andExpect(status().isNoContent())
                .andReturn();


        mockMvc.perform(get(baseUrl + "/{id}", orderId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getOrdersByStatuses_ShouldReturnFilteredOrders() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("50.00"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 1);

        CreateOrderDto createOrderDto = new CreateOrderDto(
                "test@example.com",
                List.of(orderItemDto)
        );
        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderDto)))
                .andExpect(status().isCreated())
                .andReturn();

        GetOrderDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetOrderDto.class
        );
        Long orderId = created.getOrderDtoWithoutUser().id();

        StatusDto statusDto = new StatusDto(Status.PROCESSING);

        mockMvc.perform(patch(baseUrl + "/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.getOrderDtoWithoutUser.status")
                        .value(Status.PROCESSING.name()));


        MvcResult result = mockMvc.perform(get(baseUrl)
                        .param("statuses", Status.PROCESSING.name())
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode contentNode = root.get("content");
        List<GetOrderDto> orders = objectMapper.readValue(
                contentNode.toString(),
                new com.fasterxml.jackson.core.type.TypeReference<List<GetOrderDto>>() {}
        );

        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderDtoWithoutUser().status())
                .isEqualTo(Status.PROCESSING.name());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createOrder_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CreateOrderDto invalidDto = new CreateOrderDto(
                "invalid-email",
                List.of()
        );

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createOrder_WithNonExistentItem_ShouldReturnNotFound() throws Exception {
        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(99999L, 1);
        CreateOrderDto createOrderDto = new CreateOrderDto(
                "test@example.com",
                List.of(orderItemDto)
        );

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getOrdersByStatusesAndCreationDate_shouldReturnFilteredOrders() throws Exception {
        CreateItemDto createItemDto = new CreateItemDto("Test Item", new BigDecimal("50.00"));
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createItemDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item = objectMapper.readValue(
                itemResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        CreateOrderItemDto orderItemDto = new CreateOrderItemDto(item.id(), 1);

        CreateOrderDto createOrderDto = new CreateOrderDto(
                "test@example.com",
                List.of(orderItemDto)
        );
        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderDto)))
                .andExpect(status().isCreated())
                .andReturn();

        GetOrderDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetOrderDto.class
        );
        Long orderId = created.getOrderDtoWithoutUser().id();

        StatusDto statusDto = new StatusDto(Status.PROCESSING);

        mockMvc.perform(patch(baseUrl + "/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.getOrderDtoWithoutUser.status")
                        .value(Status.PROCESSING.name()));


        MvcResult result =mockMvc.perform(get(baseUrl)
                        .param("statuses", Status.PROCESSING.name())
                        .param("startDate", "2026-01-01T00:00:00")
                        .param("endDate", "2026-12-31T23:59:59")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode contentNode = root.get("content");
        List<GetOrderDto> orders = objectMapper.readValue(
                contentNode.toString(),
                new com.fasterxml.jackson.core.type.TypeReference<List<GetOrderDto>>() {}
        );

        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderDtoWithoutUser().status())
                .isEqualTo(Status.PROCESSING.name());
    }
}