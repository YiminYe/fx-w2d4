package com.fx.api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Row of the fx_rate table.
 *
 * Activity 5 added `capturedAt`: with a live feed there are many rows per pair per DAY,
 * so `rateDate` can no longer answer "which one is newest?".
 */
public record Rate(int id, String baseCode, String quoteCode, double rate,
                   LocalDate rateDate, LocalDateTime capturedAt) {
    public String pair() { return baseCode + "/" + quoteCode; }
}
