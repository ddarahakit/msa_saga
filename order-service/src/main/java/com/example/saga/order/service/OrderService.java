package com.example.saga.order.service;

import com.example.saga.common.event.OrderEvent;
import com.example.saga.order.entity.Order;
import com.example.saga.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final StreamBridge streamBridge;

    @Transactional
    public Order createOrder(Order order) {
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        // Publish OrderCreatedEvent
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getId())
                .productId(savedOrder.getProductId())
                .userId(savedOrder.getUserId())
                .price(savedOrder.getPrice())
                .status(savedOrder.getStatus())
                .build();
        
        streamBridge.send("orderCreated-out-0", event);
        return savedOrder;
    }
}
