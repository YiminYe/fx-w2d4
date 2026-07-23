package com.fx.api.feed;

import com.fx.api.admin.AcceptingState;
import com.fx.api.feed.model.IncomingBatch;
import com.fx.api.feed.model.IncomingRate;
import com.fx.api.repo.RateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The rule the whole loop turns on:
 *
 *   ACCEPTING ON  → store every rate in the batch, answer ACCEPTED  → feed speeds up (2s)
 *   ACCEPTING OFF → store nothing,                answer DECLINED  → feed backs off (10s)
 */
@Service
public class FeedService {

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);

    private final RateRepository rates;
    private final AcceptingState accepting;
    private final OrchestratorClient orchestrator;

    public FeedService(RateRepository rates, AcceptingState accepting, OrchestratorClient orchestrator) {
        this.rates = rates;
        this.accepting = accepting;
        this.orchestrator = orchestrator;
    }

    public void handle(IncomingBatch batch) {
        if (!accepting.isAccepting()) {
            log.info("Batch {} DECLINED — ACCEPTING is OFF, nothing stored", batch.batchId());
            orchestrator.ack(batch.batchId(), "DECLINED");
            return;
        }

        int stored = 0;
        for (IncomingRate tick : batch.rates()) {
            rates.insertTick(tick.base(), tick.quote(), tick.rate());
            stored++;
        }
        log.info("Batch {} ACCEPTED — stored {} rates", batch.batchId(), stored);
        orchestrator.ack(batch.batchId(), "ACCEPTED");
    }
}
