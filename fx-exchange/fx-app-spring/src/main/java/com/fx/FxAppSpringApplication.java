package com.fx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Currency Exchange System — the Spring Boot service.
 *
 * Right now this is a hello-world skeleton: it boots, serves GET /health, and does
 * nothing else. Everything that makes it an FX system, you add:
 *   W2D2 — your Week-1 domain, then REST endpoints backed by JDBC against fxdb.
 *   W2D3 — three altitudes of tests, plus CI that runs them on every push.
 *   W2D4 — a container, a compose stack, and a deployment pipeline.
 *   Week 3 — your team ships it as the Full-Stack Financial Application.
 *
 * One annotation does three things here: @SpringBootApplication is @Configuration +
 * @EnableAutoConfiguration + @ComponentScan. The scan starts in THIS class's package
 * (com.fx), which is why everything you add under com.fx.* is found automatically —
 * and why this class must stay at the root of the package tree.
 */
@SpringBootApplication
public class FxAppSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(FxAppSpringApplication.class, args);
    }
}
