package com.github.everolfe.orderservice.unit;

import com.github.everolfe.orderservice.dao.ItemRepository;
import com.github.everolfe.orderservice.dto.item.CreateItemDto;
import com.github.everolfe.orderservice.dto.item.GetItemDto;
import com.github.everolfe.orderservice.entity.Item;
import com.github.everolfe.orderservice.mapper.itemmapper.CreateItemMapper;
import com.github.everolfe.orderservice.mapper.itemmapper.GetItemMapper;
import com.github.everolfe.orderservice.service.impl.ItemServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private GetItemMapper getItemMapper;

    @Mock
    private CreateItemMapper createItemMapper;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    @WithMockUser("ADMIN")
    void createItem_success(){
        Item item = new Item();
        Item savedItem = new Item();
        CreateItemDto createItemDto = new CreateItemDto(
                "Test",
                BigDecimal.ONE );
        GetItemDto getItemDto = new GetItemDto(
                1L,
                "Test",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now());
        when(createItemMapper.toEntity(createItemDto)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(savedItem);
        when(getItemMapper.toDto(savedItem)).thenReturn(getItemDto);

        itemService.create(createItemDto);

        assertEquals(1L, getItemDto.id());
        verify(createItemMapper,times(1)).toEntity(createItemDto);
        verify(itemRepository,times(1)).save(item);
        verify(getItemMapper,times(1)).toDto(savedItem);
    }

    @Test
    @WithMockUser("ADMIN")
    void createItem_WhenNameIsNull_ShouldThrowDataIntegrityViolationException() {
        CreateItemDto createDto = new CreateItemDto(null, BigDecimal.valueOf(100.0));
        Item itemWithNullName = new Item();
        itemWithNullName.setName(null);
        itemWithNullName.setPrice(100L);

        when(createItemMapper.toEntity(createDto)).thenReturn(itemWithNullName);
        when(itemRepository.save(itemWithNullName))
                .thenThrow(new DataIntegrityViolationException("Column 'name' cannot be null"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            itemService.create(createDto);
        });
    }

    @Test
    @WithMockUser("ADMIN")
    void getItemById_success(){
        Item item = new Item();
        item.setId(1L);
        GetItemDto getItemDto = new GetItemDto(
                1L,
                "Test",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now());
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(getItemMapper.toDto(item)).thenReturn(getItemDto);

        itemService.getItemById(1L);

        assertEquals(1L, getItemDto.id());
        verify(itemRepository,times(1)).findById(1L);
        verify(getItemMapper,times(1)).toDto(item);
    }

    @Test
    @WithMockUser("ADMIN")
    void getItemById_WhenIdIsInvalid_ThrowEntityNotFoundException() {
        Item item = new Item();
        item.setId(1L);


        assertThrows(EntityNotFoundException.class, () -> itemService.getItemById(10L));
        verify(itemRepository,times(1)).findById(10L);
        verify(getItemMapper,times(0)).toDto(item);
    }

    @Test
    @WithMockUser("ADMIN")
    void getAllItems_success(){
        Pageable pageable = PageRequest.of(0, 10);

        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("Item 1");
        item1.setPrice(100L);
        item1.setDeleted(false);

        Item item2 = new Item();
        item2.setId(2L);
        item2.setName("Item 2");
        item2.setPrice(200L);
        item2.setDeleted(false);

        Page<Item> itemPage = new PageImpl<>(List.of(item1, item2), pageable, 2);

        GetItemDto dto1 = new GetItemDto(
                1L, "Item 1", BigDecimal.valueOf(100.0), false, LocalDateTime.now(), LocalDateTime.now());
        GetItemDto dto2 = new GetItemDto(
                2L, "Item 2", BigDecimal.valueOf(200.0), false, LocalDateTime.now(), LocalDateTime.now());

        when(itemRepository.findAll(pageable)).thenReturn(itemPage);
        when(getItemMapper.toDto(item1)).thenReturn(dto1);
        when(getItemMapper.toDto(item2)).thenReturn(dto2);

        Page<GetItemDto> result = itemService.getAllItems(pageable);

        assertAll(
                () -> Assertions.assertNotNull(result),
                () -> assertEquals(2, result.getTotalElements()),
                () -> assertEquals(2, result.getContent().size()),
                () -> assertEquals(1L, result.getContent().getFirst().id()),
                () -> assertEquals("Item 1", result.getContent().getFirst().name()),
                () -> assertEquals(2L, result.getContent().get(1).id()),
                () -> assertEquals("Item 2", result.getContent().get(1).name())
        );

        verify(itemRepository).findAll(pageable);
        verify(getItemMapper, times(2)).toDto(any(Item.class));

    }

    @Test
    @WithMockUser("ADMIN")
    void updateItem_success(){
        Item item = new Item();
        item.setId(1L);
        item.setName("Item 1");

        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setName("Test");
        updatedItem.setPrice(100L);

        CreateItemDto createItemDto = new CreateItemDto(
                "Test",
                BigDecimal.ONE );

        GetItemDto getItemDto = new GetItemDto(
                1L,
                "Test",
                BigDecimal.ONE,
                false,
                LocalDateTime.now(),
                LocalDateTime.now());

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(updatedItem);
        when(getItemMapper.toDto(updatedItem)).thenReturn(getItemDto);

        itemService.updateItem(1L, createItemDto);

        assertAll(
                () -> assertEquals(1L, getItemDto.id()),
                () -> assertEquals("Test", getItemDto.name()),
                () -> assertEquals(BigDecimal.ONE, getItemDto.price())
        );

        verify(itemRepository,times(1)).save(item);
        verify(itemRepository,times(1)).findById(1L);
        verify(getItemMapper,times(1)).toDto(updatedItem);
    }

    @Test
    @WithMockUser("ADMIN")
    void updateItem_withInvalidId_ThrowEntityNotFoundException(){
        Item item = new Item();
        item.setId(1L);
        CreateItemDto createItemDto = new CreateItemDto(
                "Test",
                BigDecimal.ONE );

        assertThrows(EntityNotFoundException.class, () -> itemService.updateItem(10L, createItemDto));
        verify(itemRepository,times(1)).findById(10L);
        verify(getItemMapper,times(0)).toDto(item);
        verify(createItemMapper, times(0)).merge(any(),any());
        verify(itemRepository,times(0)).save(any(Item.class));
    }

    @Test
    @WithMockUser("ADMIN")
    void updateItem_withNullField__ShouldThrowDataIntegrityViolationException(){
        CreateItemDto createDto = new CreateItemDto(null, BigDecimal.valueOf(100.0));
        Item itemWithNullName = new Item();
        itemWithNullName.setId(1L);
        itemWithNullName.setName(null);
        itemWithNullName.setPrice(100L);


        when(itemRepository.findById(1L)).thenReturn(Optional.of(itemWithNullName));
        when(itemRepository.save(itemWithNullName))
                .thenThrow(new DataIntegrityViolationException("Column 'name' cannot be null"));

        assertThrows(DataIntegrityViolationException.class, () -> {
            itemService.updateItem(1L, createDto);
        });
    }

    @Test
    @WithMockUser("ADMIN")
    void deleteItem_success(){
        Item item = new Item();
        item.setId(1L);
        item.setName("Item 1");
        item.setPrice(100L);
        item.setDeleted(false);

        Item savedItem = new Item();
        savedItem.setId(1L);
        savedItem.setName("Item 1");
        savedItem.setPrice(100L);
        savedItem.setDeleted(true);

        GetItemDto getItemDto = new GetItemDto(
                savedItem.getId(),
                savedItem.getName(),
                BigDecimal.valueOf(savedItem.getPrice()),
                savedItem.getDeleted(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(savedItem);
        when(getItemMapper.toDto(savedItem)).thenReturn(getItemDto);

        itemService.deleteItem(1L);

        assertAll(
                () -> assertEquals("Item 1", getItemDto.name()),
                () -> Assertions.assertTrue(getItemDto.deleted()),
                () -> assertEquals(item.getPrice(), getItemDto.price().longValue())
        );

        verify(itemRepository,times(1)).findById(1L);
        verify(itemRepository,times(1)).save(item);
        verify(getItemMapper,times(1)).toDto(savedItem);
    }

    @Test
    @WithMockUser("ADMIN")
    void deleteItem_withInvalidId_shouldThrowEntityNotFoundException(){
        assertThrows(EntityNotFoundException.class, () -> itemService.getItemById(10L));
        verify(itemRepository,times(1)).findById(any());
    }
}