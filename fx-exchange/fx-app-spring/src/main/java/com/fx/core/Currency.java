package com.fx.core;

/**
 * Task 3 solution — rich enum. One constant per row of Day 3's `currency`
 * table (code, name, symbol): the enum IS that table, compiled.
 */
public enum Currency {
    EUR("€", "Euro"), USD("$", "US Dollar"), GBP("£", "Pound Sterling"),
    JPY("¥", "Japanese Yen"), CHF("Fr", "Swiss Franc"),
    AUD("A$", "Australian Dollar"), CAD("C$", "Canadian Dollar"), SGD("S$", "Singapore Dollar");

    private final String symbol;
    private final String fullName;

    Currency(String symbol, String fullName) { this.symbol = symbol; this.fullName = fullName; }
    public String getSymbol() { return symbol; }
    public String getFullName() { return fullName; }
}
