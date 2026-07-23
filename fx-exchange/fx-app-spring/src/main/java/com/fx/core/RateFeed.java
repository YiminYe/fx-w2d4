package com.fx.core;

/** Live market rates come from outside — in tests, you'll mock this (Day 1 Ex 02). */
public interface RateFeed {
    /** @return current rate for a pair like "EUR/USD"; throws IllegalArgumentException if unknown. */
    double rateFor(String pair);
}
