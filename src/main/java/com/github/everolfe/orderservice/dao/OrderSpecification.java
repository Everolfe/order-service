package com.github.everolfe.orderservice.dao;

import com.github.everolfe.orderservice.entity.Order;
import com.github.everolfe.orderservice.entity.Status;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class OrderSpecification {
    public static Specification<Order> createdBetween(LocalDateTime start, LocalDateTime end) {
        return ((root, query, criteriaBuilder) ->{
         if(start == null && end == null) {
             return criteriaBuilder.conjunction();
         } else if(end == null) {
             return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start);
         } else if(start == null) {
             return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), end);
         }  else {
             return criteriaBuilder.between(root.get("createdAt"), start, end);
         }
        } );
    }

    public static Specification<Order> statusIn(List<Status> statuses) {
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Order> combineFilters(
            LocalDateTime start,
            LocalDateTime end,
            List<Status> statuses) {
        Specification<Order> spec = OrderSpecification.createdBetween(start, end);
        spec = spec.and(OrderSpecification.statusIn(statuses));
        return spec;
    }
}
