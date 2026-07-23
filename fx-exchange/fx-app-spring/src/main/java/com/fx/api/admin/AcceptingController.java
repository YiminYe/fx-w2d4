package com.fx.api.admin;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The switch fx-monitor's toggle drives. */
@RestController
@RequestMapping("/api/admin")
public class AcceptingController {

    private static final Logger log = LoggerFactory.getLogger(AcceptingController.class);

    private final AcceptingState state;

    public AcceptingController(AcceptingState state) {
        this.state = state;
    }

    @GetMapping("/accepting")
    public Map<String, Boolean> get() {
        return Map.of("accepting", state.isAccepting());
    }

    @PostMapping("/accepting")
    public Map<String, Boolean> set(@RequestBody Map<String, Boolean> body) {
        boolean value = Boolean.TRUE.equals(body.get("accepting"));
        state.set(value);
        log.info("ACCEPTING switched {}", value ? "ON" : "OFF");
        return Map.of("accepting", value);
    }
}
