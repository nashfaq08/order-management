package com.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private boolean available;
    private boolean deleted;
    private String createdAt;
    private String updatedAt;
}

