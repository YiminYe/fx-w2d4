package com.fx.api.feed.model;

/** What we post back to fx-orchestrator: {"batchId":12,"status":"ACCEPTED"} */
public record Ack(long batchId, String status) {}
