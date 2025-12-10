package com.order.discount.impl;

import com.order.discount.DiscountStrategy;

public class LargeOrderDiscountStrategy implements DiscountStrategy {
    @Override
    public double applyDiscount(double amount) {
        return amount * 0.95;
    }
}

