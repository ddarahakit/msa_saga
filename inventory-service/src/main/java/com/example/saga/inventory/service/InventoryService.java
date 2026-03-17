package com.example.saga.inventory.service;

import com.example.saga.common.event.InventoryEvent;
import com.example.saga.common.event.OrderEvent;
import com.example.saga.common.event.PaymentEvent;
import com.example.saga.inventory.entity.Inventory;
import com.example.saga.inventory.entity.InventoryHistory;
import com.example.saga.inventory.repository.InventoryHistoryRepository;
import com.example.saga.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Bean
    public Function<PaymentEvent, InventoryEvent> paymentProcessor() {
        return event -> {
            if ("SUCCESS".equals(event.getStatus())) {
                log.info("Processing Inventory for OrderId: {}", event.getOrderId());
                return reserveInventory(event);
            }
            return null;
        };
    }

    @Bean
    public Consumer<OrderEvent> orderCancelledConsumer() {
        return event -> {
            if ("CANCELLED".equals(event.getStatus())) {
                log.info("Releasing Inventory for OrderId: {}", event.getOrderId());
                releaseInventory(event.getOrderId());
            }
        };
    }

    @Transactional
    public InventoryEvent reserveInventory(PaymentEvent event) {
        // Hardcoded product logic for simplicity
        Long productId = 1L; // Assuming productId 1 for all orders in this demo
        int quantity = 1;

        return inventoryRepository.findById(productId)
                .map(inventory -> {
                    if (inventory.getStockCount() >= quantity) {
                        inventory.setStockCount(inventory.getStockCount() - quantity);
                        inventoryRepository.save(inventory);

                        InventoryHistory history = InventoryHistory.builder()
                                .orderId(event.getOrderId())
                                .productId(productId)
                                .quantity(quantity)
                                .status("RESERVED")
                                .build();
                        inventoryHistoryRepository.save(history);

                        return InventoryEvent.builder()
                                .orderId(event.getOrderId())
                                .productId(productId)
                                .quantity(quantity)
                                .status("RESERVED")
                                .build();
                    } else {
                        return InventoryEvent.builder()
                                .orderId(event.getOrderId())
                                .status("OUT_OF_STOCK")
                                .build();
                    }
                }).orElseGet(() -> InventoryEvent.builder()
                        .orderId(event.getOrderId())
                        .status("OUT_OF_STOCK")
                        .build());
    }

    @Transactional
    public void releaseInventory(Long orderId) {
        inventoryHistoryRepository.findByOrderId(orderId).ifPresent(history -> {
            if ("RESERVED".equals(history.getStatus())) {
                inventoryRepository.findById(history.getProductId()).ifPresent(inventory -> {
                    inventory.setStockCount(inventory.getStockCount() + history.getQuantity());
                    inventoryRepository.save(inventory);
                });
                history.setStatus("RELEASED");
                inventoryHistoryRepository.save(history);
                log.info("Inventory RELEASED for OrderId: {}", orderId);
            }
        });
    }
}
