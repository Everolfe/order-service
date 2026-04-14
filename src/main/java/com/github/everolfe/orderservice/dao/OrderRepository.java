package com.github.everolfe.orderservice.dao;

import com.github.everolfe.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.deleted = false")
    Page<Order> findActiveOrdersByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.deleted = false")
    Page<Order> findAllActive(Pageable pageable);
}
