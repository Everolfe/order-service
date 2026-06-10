package com.github.everolfe.orderservice.service.event;


import com.github.everolfe.orderservice.dao.OrderRepository;
import com.github.everolfe.orderservice.dto.event.PaymentEventDto;
import com.github.everolfe.orderservice.dto.event.PaymentStatus;
import com.github.everolfe.orderservice.entity.Order;
import com.github.everolfe.orderservice.entity.Status;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderConsumer {

    private final OrderRepository orderRepository;

    @Transactional
    @KafkaListener(topics = "${kafka.topics.consumer}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleCreateOrderEvent(PaymentEventDto eventDto) {

        log.info("Received CreatePaymentEvent from Kafka: {}", eventDto);

        if (eventDto.status() == PaymentStatus.SUCCESS) {

            Long orderId = eventDto.orderId();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order for payment not found: " + orderId));

            order.setStatus(Status.PROCESSING);

            orderRepository.save(order);
        }
    }
}
