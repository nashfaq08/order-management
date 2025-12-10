package com.order.controller;

import com.order.dto.*;
import com.order.dto.response.OrderResponse;
import com.order.dto.response.PagedResponse;
import com.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody OrderRequest request
    ) {
        String token = authHeader.substring(7);
        String username = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(orderService.placeOrder(
                username,
                role,
                token,
                request
        ));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<PagedResponse<OrderResponse>> getPagedOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Authentication authentication
    ) {
        String username = authentication.getName();
        return ResponseEntity.ok(orderService.getAllOrders(username, page, size, sortBy, sortDir));
    }
}