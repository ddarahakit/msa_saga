package com.example.saga.inventory.repository;

import com.example.saga.inventory.entity.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    Optional<InventoryHistory> findByOrderId(Long orderId);
}
