package com.order.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class OrderRequest {

    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        private UUID productId;
        private int quantity;
    }
}

