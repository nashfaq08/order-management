package com.order.dto.response;

import lombok.Data;
import java.util.UUID;

@Data
public class OrderItemResponse {
    private UUID productId;
    private int quantity;
    private double unitPrice;
    private double discountApplied;
    private double totalPrice;
}
