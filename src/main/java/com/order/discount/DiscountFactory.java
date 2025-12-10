package com.order.discount;

import com.order.discount.impl.PremiumUserDiscountStrategy;
import com.order.discount.impl.UserDiscountStrategy;
import org.springframework.stereotype.Component;

@Component
public class DiscountFactory {
    public DiscountStrategy getStrategy(String role) {
        return switch (role) {
            case "PREMIUM_USER" -> new PremiumUserDiscountStrategy();
            default -> new UserDiscountStrategy();
        };
    }
}
