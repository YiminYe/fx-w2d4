package com.fx.api.feed.model;

import java.time.Instant;
import java.util.List;

/** The body fx-orchestrator POSTs to /api/feed/rates. Field names must match exactly. */
public record IncomingBatch(long batchId, Instant generatedAt, List<IncomingRate> rates) {}
