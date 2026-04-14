package com.github.everolfe.orderservice.service;

import com.github.everolfe.orderservice.dto.item.CreateItemDto;
import com.github.everolfe.orderservice.dto.item.GetItemDto;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing items.
 * Provides operations for creating, retrieving, updating, and soft-deleting items.
 */
public interface ItemService {

    /**
     * Creates a new item.
     *
     * @param createItemDto the DTO containing item creation data (name and price)
     * @return the DTO of the created item with generated ID and timestamps
     * @throws IllegalArgumentException if createItemDto is null or contains invalid data
     */
    GetItemDto create(CreateItemDto createItemDto);

    /**
     * Retrieves an item by its ID.
     * Only non-deleted items are returned.
     *
     * @param id the ID of the item to retrieve
     * @return the DTO of the item
     * @throws EntityNotFoundException if no active (non-deleted) item is found with the given ID
     */
    GetItemDto getItemById(Long id);

    /**
     * Retrieves all items with pagination.
     * Includes both deleted and non-deleted items.
     *
     * @param pageable pagination information (page number, size, sorting)
     * @return a page of item DTOs
     */
    Page<GetItemDto> getAllItems(Pageable pageable);

    /**
     * Updates an existing item with new data.
     * Can update both deleted and non-deleted items.
     *
     * @param id the ID of the item to update
     * @param createItemDto the DTO containing updated item data
     * @return the DTO of the updated item
     * @throws EntityNotFoundException if no item is found with the given ID
     */
    GetItemDto updateItem(Long id, CreateItemDto createItemDto);

    /**
     * Soft-deletes an item by setting its deleted flag to true.
     * The item remains in the database but will not be returned by getItemById().
     * Already deleted items cannot be deleted again.
     *
     * @param id the ID of the item to delete
     * @return the DTO of the deleted item (with deleted flag set to true)
     * @throws EntityNotFoundException if no item is found with the given ID
     * @throws EntityNotFoundException if the item is already deleted
     */
    GetItemDto deleteItem(Long id);
}
