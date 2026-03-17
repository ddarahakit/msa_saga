package com.example.saga.order.service;

import com.example.saga.common.event.InventoryEvent;
import com.example.saga.common.event.OrderEvent;
import com.example.saga.common.event.PaymentEvent;
import com.example.saga.order.entity.Order;
import com.example.saga.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {
    private final OrderRepository orderRepository;
    private final StreamBridge streamBridge;

    @Bean
    public Consumer<PaymentEvent> paymentConsumer() {
        return event -> {
            log.info("Consuming PaymentEvent for OrderId: {}, Status: {}", event.getOrderId(), event.getStatus());
            if ("SUCCESS".equals(event.getStatus())) {
                log.info("Payment SUCCESS, no further action for order status update yet (Waiting for Inventory)");
            } else if ("FAILED".equals(event.getStatus())) {
                cancelOrder(event.getOrderId());
            }
        };
    }

    @Bean
    public Consumer<InventoryEvent> inventoryConsumer() {
        return event -> {
            log.info("Consuming InventoryEvent for OrderId: {}, Status: {}", event.getOrderId(), event.getStatus());
            if ("RESERVED".equals(event.getStatus())) {
                completeOrder(event.getOrderId());
            } else if ("OUT_OF_STOCK".equals(event.getStatus())) {
                cancelOrder(event.getOrderId());
                // Trigger compensation for payment
                OrderEvent orderCancelledEvent = OrderEvent.builder()
                        .orderId(event.getOrderId())
                        .status("CANCELLED")
                        .build();
                streamBridge.send("orderCancelled-out-0", orderCancelledEvent);
            }
        };
    }

    @Transactional
    public void completeOrder(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus("COMPLETED");
            orderRepository.save(order);
            log.info("Order COMPLETED: {}", orderId);
        });
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.info("Order CANCELLED: {}", orderId);
        });
    }
}
