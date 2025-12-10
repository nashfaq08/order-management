package com.order.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderResponse {

    private UUID orderId;
    private String username;
    private double orderTotal;
    private List<OrderItemResponse> items;
}

