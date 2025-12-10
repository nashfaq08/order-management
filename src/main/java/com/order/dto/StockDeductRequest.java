package com.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class StockDeductRequest {
    private UUID productId;
    private int quantity;
}

