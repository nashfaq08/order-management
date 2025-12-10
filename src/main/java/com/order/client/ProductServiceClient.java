package com.order.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.dto.StockCheckRequest;
import com.order.dto.StockDeductRequest;
import com.order.dto.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    public void validateStock(List<StockCheckRequest> request, String jwtToken) {
        callPost("/validate-stock", request, jwtToken);
    }

    public void deductStock(List<StockDeductRequest> request, String jwtToken) {
        callPost("/deduct-stock", request, jwtToken);
    }

    public void restoreStock(List<StockDeductRequest> request, String jwtToken) {
        callPost("/restore-stock", request, jwtToken);
    }

    private void callPost(String endpoint, Object body, String jwtToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        String url = productServiceUrl + endpoint;

        try {
            restTemplate.postForEntity(url, entity, Void.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed calling Product Service endpoint: " + endpoint, e);
        }
    }

    public ProductResponse getProduct(UUID id, String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                productServiceUrl + "/" + id,
                HttpMethod.GET,
                entity,
                ProductResponse.class
        );

        return response.getBody();
    }

    private String extractMessage(String jsonBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonBody);

            if (root.has("message")) {
                return root.get("message").asText();
            }
        } catch (Exception e) {}

        return jsonBody;
    }
}
