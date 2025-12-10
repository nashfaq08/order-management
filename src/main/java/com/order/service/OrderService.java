package com.order.service;

import com.order.client.ProductServiceClient;
import com.order.discount.DiscountFactory;
import com.order.discount.impl.LargeOrderDiscountStrategy;
import com.order.dto.OrderRequest;
import com.order.dto.StockCheckRequest;
import com.order.dto.StockDeductRequest;
import com.order.dto.response.OrderItemResponse;
import com.order.dto.response.OrderResponse;
import com.order.dto.response.PagedResponse;
import com.order.dto.response.ProductResponse;
import com.order.entities.Order;
import com.order.entities.OrderItem;
import com.order.exception.OrderProcessingException;
import com.order.exception.ProductServiceException;
import com.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final ProductServiceClient productClient;
    private final DiscountFactory discountFactory;
    private final OrderRepository orderRepository;

    public OrderResponse placeOrder(String username, String role, String jwtToken, OrderRequest request) {

        log.info("User '{}' is placing an order with {} item(s)", username, request.getItems().size());

        try {
            List<StockCheckRequest> stockCheckList = request.getItems().stream()
                    .map(i -> new StockCheckRequest(i.getProductId(), i.getQuantity()))
                    .toList();

            log.info("Sending stock validation request to Product Service. Items: {}", stockCheckList);
            productClient.validateStock(stockCheckList, jwtToken);
            log.info("Stock validation successful");
        } catch (Exception ex) {
            log.error("Stock validation failed: {}", ex.getMessage());
            throw new ProductServiceException("Stock validation failed", ex);
        }

        double subtotal = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        try {
            for (OrderRequest.ItemRequest i : request.getItems()) {

                log.info("Fetching product details for productId={}", i.getProductId());
                ProductResponse product = productClient.getProduct(i.getProductId(), jwtToken);

                if (product == null) {
                    log.error("Product {} not found", i.getProductId());
                    throw new ProductServiceException("Product not found: " + i.getProductId());
                }

                double total = product.getPrice() * i.getQuantity();
                subtotal += total;

                orderItems.add(
                        OrderItem.builder()
                                .productId(product.getId())
                                .unitPrice(product.getPrice())
                                .quantity(i.getQuantity())
                                .totalPrice(total)
                                .build()
                );

                log.info("Computed order item: product={}, qty={}, unitPrice={}, total={}",
                        product.getId(), i.getQuantity(), product.getPrice(), total);
            }

        } catch (Exception ex) {
            log.error("Failed to fetch product details: {}", ex.getMessage());
            throw new ProductServiceException("Failed to fetch product details", ex);
        }

        log.info("Calculating discount for role '{}' with subtotal={}", role, subtotal);

        double discounted = discountFactory.getStrategy(role).applyDiscount(subtotal);

        if (discounted > 500) {
            log.info("Applying extra 5% discount for large order");
            discounted = new LargeOrderDiscountStrategy().applyDiscount(discounted);
        }

        log.info("Final discounted total: {}", discounted);

        List<StockDeductRequest> stockDeductList = request.getItems().stream()
                .map(i -> new StockDeductRequest(i.getProductId(), i.getQuantity()))
                .toList();

        try {
            log.info("Sending stock deduction request to Product Service: {}", stockDeductList);
            productClient.deductStock(stockDeductList, jwtToken);
            log.info("Stock deduction successful");
        } catch (Exception ex) {
            log.error("Stock deduction failed, ORDER WILL NOT BE CREATED: {}", ex.getMessage());
            throw new ProductServiceException("Stock deduction failed", ex);
        }

        try {
            Order order = Order.builder()
                    .username(username)
                    .orderTotal(discounted)
                    .items(orderItems)
                    .build();

            orderItems.forEach(item -> item.setOrder(order));

            Order savedOrder = orderRepository.save(order);
            log.info("Order successfully saved. OrderId={}", savedOrder.getId());

            return toResponse(savedOrder);
        } catch (Exception ex) {
            log.error("Failed to save order: {}", ex.getMessage());
            try {
                productClient.restoreStock(
                        stockDeductList,
                        jwtToken
                );
                log.info("Rollback completed: Stock restored successfully");
            } catch (Exception rollbackEx) {
                log.error("Rollback failed! Manual intervention required. Reason: {}",
                        rollbackEx.getMessage(), rollbackEx);
            }

            throw new OrderProcessingException("Could not save order (stock restored)", ex);
        }
    }

    @Transactional
    public PagedResponse<OrderResponse> getAllOrders(
            String username,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Fetching orders for user={} page={} size={}", username, page, size);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Order> ordersPage =
                username == null
                        ? orderRepository.findAll(pageable)
                        : orderRepository.findByUsername(username, pageable);

        List<OrderResponse> content = ordersPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                ordersPage.getNumber(),
                ordersPage.getSize(),
                ordersPage.getTotalElements(),
                ordersPage.getTotalPages(),
                ordersPage.isLast()
        );
    }

    private OrderResponse toResponse(Order order) {

        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setUsername(order.getUsername());
        response.setOrderTotal(order.getOrderTotal());

        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            OrderItemResponse dto = new OrderItemResponse();
            dto.setProductId(item.getProductId());
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setDiscountApplied(item.getDiscountApplied());
            dto.setTotalPrice(item.getTotalPrice());
            return dto;
        }).toList();

        response.setItems(items);
        return response;
    }
}