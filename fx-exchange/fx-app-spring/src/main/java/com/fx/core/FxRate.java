package com.fx.core;

/**
 * Task 5 solution — built test-first (see src/test/java/com/fx/core/FxRateTest).
 * The Java shape of Day 3's `fx_rate` table: base_code / quote_code / rate /
 * rate_date. Week 2 serves exactly these rows over REST — the checkpoint value
 * EUR/USD 1.0818 (2026-01-12) comes straight from fxdb-seed.sql.
 */
public class FxRate {
    private final Currency base;       // mirrors fx_rate.base_code
    private final Currency quote;      // mirrors fx_rate.quote_code
    private final double rate;         // mirrors fx_rate.rate
    private final String rateDate;     // mirrors fx_rate.rate_date (ISO yyyy-MM-dd)

    public FxRate(Currency base, Currency quote, double rate, String rateDate) {
        if (rate <= 0) throw new InvalidRateException(rate);
        this.base = base;
        this.quote = quote;
        this.rate = rate;
        this.rateDate = rateDate;
    }

    public Currency getBase() { return base; }
    public Currency getQuote() { return quote; }
    public double getRate() { return rate; }
    public String getRateDate() { return rateDate; }

    /** 100 base becomes how much quote? */
    public double convert(double amount) { return amount * rate; }

    /** The same market fact read the other way: EUR/USD 1.0818 -> USD/EUR (1/1.0818). */
    public FxRate inverted() { return new FxRate(quote, base, 1.0 / rate, rateDate); }

    @Override public String toString() {
        return String.format("%s/%s %.4f (%s)", base, quote, rate, rateDate);
    }
}
