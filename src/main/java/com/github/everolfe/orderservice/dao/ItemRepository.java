package com.github.everolfe.orderservice.dao;

import com.github.everolfe.orderservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
