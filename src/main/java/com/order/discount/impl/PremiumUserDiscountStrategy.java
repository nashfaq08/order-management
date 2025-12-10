package com.order.discount.impl;

import com.order.discount.DiscountStrategy;

public class PremiumUserDiscountStrategy implements DiscountStrategy {
    @Override
    public double applyDiscount(double amount) {
        return amount * 0.90;
    }
}

