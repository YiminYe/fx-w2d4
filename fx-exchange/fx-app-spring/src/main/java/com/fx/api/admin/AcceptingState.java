package com.fx.api.admin;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

/**
 * Are we taking the upstream feed right now?
 *
 * In memory on purpose — it is an operational switch, not business data, and it resets
 * to ON when the app restarts. AtomicBoolean because the feed thread reads it while an
 * HTTP thread may be flipping it.
 */
@Component
public class AcceptingState {

    private final AtomicBoolean accepting = new AtomicBoolean(true);

    public boolean isAccepting() {
        return accepting.get();
    }

    public boolean set(boolean value) {
        accepting.set(value);
        return value;
    }
}
