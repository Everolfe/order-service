package com.github.everolfe.orderservice.service.impl;

import com.github.everolfe.orderservice.dao.ItemRepository;
import com.github.everolfe.orderservice.dto.item.CreateItemDto;
import com.github.everolfe.orderservice.dto.item.GetItemDto;
import com.github.everolfe.orderservice.entity.Item;
import com.github.everolfe.orderservice.mapper.itemmapper.CreateItemMapper;
import com.github.everolfe.orderservice.mapper.itemmapper.GetItemMapper;
import com.github.everolfe.orderservice.service.ItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final GetItemMapper getItemMapper;
    private final CreateItemMapper createItemMapper;
    private static final String ITEM_NOT_FOUND_MESSAGE = "Item not found by id: ";

    @Override
    @Transactional
    public GetItemDto create(CreateItemDto createItemDto) {
        Item item = createItemMapper.toEntity(createItemDto);
        Item savedItem = itemRepository.save(item);
        return getItemMapper.toDto(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public GetItemDto getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .filter(i -> !i.getDeleted())
                .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND_MESSAGE + id));
        return getItemMapper.toDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GetItemDto> getAllItems(Pageable pageable) {
        Page<Item> items = itemRepository.findAll(pageable);
        return items.map(getItemMapper::toDto);
    }

    @Override
    @Transactional
    public GetItemDto updateItem(Long id, CreateItemDto createItemDto) {
        Item item = itemRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND_MESSAGE + id));
        createItemMapper.merge(item, createItemDto);
        Item savedItem = itemRepository.save(item);
        return getItemMapper.toDto(savedItem);
    }

    @Override
    @Transactional
    public GetItemDto deleteItem(Long id) {
        Item item = itemRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND_MESSAGE + id));

        if (item.getDeleted()) {
            throw new EntityNotFoundException("Item already deleted by id: " + id);
        }

        item.setDeleted(true);
        Item savedItem = itemRepository.save(item);
        return getItemMapper.toDto(savedItem);
    }
}
