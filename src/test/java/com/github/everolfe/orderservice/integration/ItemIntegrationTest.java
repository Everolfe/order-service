package com.github.everolfe.orderservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.everolfe.orderservice.dao.ItemRepository;
import com.github.everolfe.orderservice.dto.item.CreateItemDto;
import com.github.everolfe.orderservice.dto.item.GetItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
class ItemIntegrationTest extends WireMockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "/api/items";
        itemRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createItem_AsAdmin_ShouldReturnCreatedItem() throws Exception {
        CreateItemDto createDto = new CreateItemDto(
                "Test Item",
                new BigDecimal("99.99")
        );

        MvcResult result = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GetItemDto.class
        );

        assertThat(response.name()).isEqualTo("Test Item");
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(response.id()).isNotNull();
        assertThat(response.deleted()).isFalse();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }


    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createItem_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CreateItemDto invalidDto = new CreateItemDto("", new BigDecimal("99.99"));

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Item name must not be empty")));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createItem_WithNegativePrice_ShouldReturnBadRequest() throws Exception {
        CreateItemDto invalidDto = new CreateItemDto("Test Item", new BigDecimal("-10.00"));

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Price must be greater than 0")));
    }

    @Test
    void getItemById_ShouldReturnItem() throws Exception {
        CreateItemDto createDto = new CreateItemDto("Existing Item", new BigDecimal("49.99"));

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto))
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetItemDto.class
        );
        Long itemId = created.id();

        mockMvc.perform(get(baseUrl + "/{id}", itemId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("Existing Item"))
                .andExpect(jsonPath("$.price").value(49.99))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void getItemById_WhenNotFound_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get(baseUrl + "/{id}", 99999L)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Item not found by id: 99999")));
    }

    @Test
    void getItemById_WhenDeleted_ShouldReturnNotFound() throws Exception {
        CreateItemDto createDto = new CreateItemDto("Item to Delete", new BigDecimal("25.00"));

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto))
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetItemDto.class
        );
        Long itemId = created.id();

        mockMvc.perform(delete(baseUrl + "/{id}", itemId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk());

        mockMvc.perform(get(baseUrl + "/{id}", itemId)
                    . with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getAllItems_AsAdmin_ShouldReturnPageOfItems() throws Exception {
        createTestItem("Item 1", new BigDecimal("10.00"));
        createTestItem("Item 2", new BigDecimal("20.00"));
        createTestItem("Item 3", new BigDecimal("30.00"));

        MvcResult result = mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseJson);

        JsonNode contentNode = root.get("content");
        List<GetItemDto> items = objectMapper.readValue(
                contentNode.toString(),
                new com.fasterxml.jackson.core.type.TypeReference<List<GetItemDto>>() {}
        );

        long totalElements = root.get("totalElements").asLong();
        int number = root.get("number").asInt();
        int size = root.get("size").asInt();

        assertThat(items).hasSize(3);
        assertThat(totalElements).isEqualTo(3);
        assertThat(number).isZero();
        assertThat(size).isEqualTo(10);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getAllItems_WithPagination_ShouldReturnCorrectPage() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createTestItem("Item " + i, new BigDecimal(i * 10.00));
        }

        MvcResult firstPageResult = mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(firstPageResult.getResponse().getContentAsString());
        List<GetItemDto> firstPageItems = objectMapper.readValue(
                root.get("content").toString(),
                new com.fasterxml.jackson.core.type.TypeReference<List<GetItemDto>>() {}
        );
        long totalElements = root.get("totalElements").asLong();
        int number = root.get("number").asInt();
        boolean hasNext = !root.get("last").asBoolean();

        assertThat(firstPageItems).hasSize(2);
        assertThat(totalElements).isEqualTo(5);
        assertThat(number).isZero();
        assertThat(hasNext).isTrue();

        MvcResult secondPageResult = mockMvc.perform(get(baseUrl)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andReturn();

        root = objectMapper.readTree(secondPageResult.getResponse().getContentAsString());
        List<GetItemDto> secondPageItems = objectMapper.readValue(
                root.get("content").toString(),
                new com.fasterxml.jackson.core.type.TypeReference<List<GetItemDto>>() {}
        );
        number = root.get("number").asInt();

        assertThat(secondPageItems).hasSize(2);
        assertThat(number).isEqualTo(1);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateItem_AsAdmin_ShouldReturnUpdatedItem() throws Exception {
        CreateItemDto createDto = new CreateItemDto("Original Name", new BigDecimal("100.00"));

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetItemDto.class
        );
        Long itemId = created.id();

        String updateJson = "{\"name\":\"Updated Name\",\"price\":150.00}";

        MvcResult updateResult = mockMvc.perform(put(baseUrl + "/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto updated = objectMapper.readValue(
                updateResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        assertThat(updated.id()).isEqualTo(itemId);
        assertThat(updated.name()).isEqualTo("Updated Name");
        assertThat(updated.price()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(updated.deleted()).isFalse();

        mockMvc.perform(get(baseUrl + "/{id}", itemId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.price").value(150.00));
    }


    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateItem_WhenNotFound_ShouldReturnNotFound() throws Exception {
        CreateItemDto updateDto = new CreateItemDto("Updated Name", new BigDecimal("150.00"));

        mockMvc.perform(put(baseUrl + "/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Item not found by id: 99999")));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void deleteItem_AsAdmin_ShouldSoftDeleteItem() throws Exception {
        CreateItemDto createDto = new CreateItemDto("Item To Delete", new BigDecimal("10.00"));

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetItemDto.class
        );
        Long itemId = created.id();

        MvcResult deleteResult = mockMvc.perform(delete(baseUrl + "/{id}", itemId))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto deleted = objectMapper.readValue(
                deleteResult.getResponse().getContentAsString(),
                GetItemDto.class
        );

        assertThat(deleted.id()).isEqualTo(itemId);
        assertThat(deleted.deleted()).isTrue();

        mockMvc.perform(get(baseUrl + "/{id}", itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void deleteItem_WhenAlreadyDeleted_ShouldReturnNotFound() throws Exception {
        CreateItemDto createDto = new CreateItemDto("Already Deleted", new BigDecimal("5.00"));

        MvcResult createResult = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GetItemDto.class
        );
        Long itemId = created.id();

        mockMvc.perform(delete(baseUrl + "/{id}", itemId))
                .andExpect(status().isOk());

        mockMvc.perform(delete(baseUrl + "/{id}", itemId))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createItem_WithDuplicateName_ShouldSucceed() throws Exception {
        CreateItemDto createDto1 = new CreateItemDto("Same Name", new BigDecimal("10.00"));
        CreateItemDto createDto2 = new CreateItemDto("Same Name", new BigDecimal("20.00"));

        MvcResult result1 = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto1)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto2)))
                .andExpect(status().isOk())
                .andReturn();

        GetItemDto item1 = objectMapper.readValue(result1.getResponse().getContentAsString(), GetItemDto.class);
        GetItemDto item2 = objectMapper.readValue(result2.getResponse().getContentAsString(), GetItemDto.class);

        assertThat(item1.id()).isNotEqualTo(item2.id());
        assertThat(item1.name()).isEqualTo(item2.name());
    }

    private void createTestItem(String name, BigDecimal price) throws Exception {
        CreateItemDto dto = new CreateItemDto(name, price);
        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}