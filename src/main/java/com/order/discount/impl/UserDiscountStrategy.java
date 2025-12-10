package com.order.discount.impl;

import com.order.discount.DiscountStrategy;

public class UserDiscountStrategy implements DiscountStrategy {
    @Override
    public double applyDiscount(double amount) {
        return amount;
    }
}
