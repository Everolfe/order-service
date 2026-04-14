package com.github.everolfe.orderservice.controller;

import com.github.everolfe.orderservice.dto.item.CreateItemDto;
import com.github.everolfe.orderservice.dto.item.GetItemDto;
import com.github.everolfe.orderservice.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetItemDto> create(
            @Valid @RequestBody CreateItemDto createItemDto) {
        GetItemDto item = itemService.create(createItemDto);
        return ResponseEntity.ok(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GetItemDto> getItemById(@PathVariable Long id) {
        GetItemDto item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<GetItemDto>> getAllItems(Pageable pageable) {
        Page<GetItemDto> items = itemService.getAllItems(pageable);
        return ResponseEntity.ok(items);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetItemDto> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateItemDto createItemDto) {
        GetItemDto item = itemService.updateItem(id, createItemDto);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetItemDto> deleteItem(@PathVariable Long id) {
        GetItemDto item = itemService.deleteItem(id);
        return ResponseEntity.ok(item);
    }
}
