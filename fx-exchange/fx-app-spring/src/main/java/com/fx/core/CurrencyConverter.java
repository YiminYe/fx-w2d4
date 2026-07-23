package com.fx.core;

/**
 * Tasks 2 + 3 solution. Task 3 adds the InvalidRateException guards — an
 * unchecked exception for what can only be a programming/data bug.
 */
public class CurrencyConverter {
    private static int conversionCount = 0;   // shared across ALL converters -> static
    private final double rate;

    public CurrencyConverter() { this(1.0); }             // constructor chaining
    public CurrencyConverter(double rate) {
        if (rate <= 0) throw new InvalidRateException(rate);   // Task 3 guard
        this.rate = rate;
    }

    public double convert(double amount) { conversionCount++; return amount * rate; }
    public double convert(double amount, double overrideRate) {
        if (overrideRate <= 0) throw new InvalidRateException(overrideRate);   // Task 3 guard
        conversionCount++;
        return amount * overrideRate;
    }
    public double[] convert(double[] amounts) {
        double[] out = new double[amounts.length];
        for (int i = 0; i < amounts.length; i++) out[i] = convert(amounts[i]);
        return out;
    }
    public double[] convertAll(double... amounts) { return convert(amounts); } // varargs delegates to array

    public static int getConversionCount() { return conversionCount; }

    @Override public String toString() { return "Converter[rate=" + rate + "]"; }
}
