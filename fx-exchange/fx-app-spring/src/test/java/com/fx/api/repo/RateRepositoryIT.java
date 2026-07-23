package com.fx.api.repo;

import com.fx.api.model.Rate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RateRepositoryIT {

    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("fxdb");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired RateRepository repo;

    @Test void latestReturnsTheTenPairsOfTheNewestDate() {
        assertThat(repo.findLatest()).hasSize(10);
    }

    @Test void eurUsdIsExactlyTheSeededRate() {
        Optional<Rate> eurUsd = repo.findLatestForPair("EUR", "USD");
        assertThat(eurUsd).isPresent();
        assertThat(eurUsd.get().rate()).isCloseTo(1.0818, within(1e-9));
    }

    @Test void unknownPairIsEmptyNotAnError() {
        assertThat(repo.findLatestForPair("AAA", "BBB")).isEmpty();
    }
}