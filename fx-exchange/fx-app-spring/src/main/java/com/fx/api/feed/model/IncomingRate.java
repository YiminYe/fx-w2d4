package com.fx.api.feed.model;

/** One rate inside a batch: {"base":"EUR","quote":"USD","rate":1.0837} */
public record IncomingRate(String base, String quote, double rate) {}
