package com.example.saga.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private String status;
}
