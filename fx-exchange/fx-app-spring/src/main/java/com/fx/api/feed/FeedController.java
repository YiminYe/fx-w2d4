package com.fx.api.feed;

import com.fx.api.feed.model.IncomingBatch;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inbound half of the contract: fx-orchestrator pushes batches here. */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feed;

    public FeedController(FeedService feed) {
        this.feed = feed;
    }

    /**
     * 202 Accepted, not 201 Created: we are acknowledging receipt of the batch, and the
     * real answer travels back separately as the ACK. Different question, different code.
     */
    @PostMapping("/rates")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(@RequestBody IncomingBatch batch) {
        feed.handle(batch);
    }
}
