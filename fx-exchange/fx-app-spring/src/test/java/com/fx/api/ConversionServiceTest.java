package com.fx.api;

import com.fx.api.model.Rate;
import com.fx.api.repo.RateRepository;
import com.fx.api.service.ConversionService;
import com.fx.api.service.UnknownPairException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock RateRepository repo;

    @Test void convertsAndRoundsToTwoDecimals() {
        when(repo.findLatestForPair("EUR", "USD"))
            .thenReturn(Optional.of(new Rate(1, "EUR", "USD", 1.0818, LocalDate.of(2026, 1, 12), null)));
        var result = new ConversionService(repo).convert("EUR", "USD", 123.45);
        assertEquals(133.55, result.converted(), 1e-9);   // 133.5482 -> 133.55
        assertEquals("EUR/USD", result.pair());
        assertEquals(1.34, result.fee(), 1e-9);            // 零售 1%，基于四舍五入后的 133.55，再次取整
        assertEquals(132.21, result.net(), 1e-9);
    }

    @Test void unknownPairThrows() {
        when(repo.findLatestForPair("AAA", "BBB")).thenReturn(Optional.empty());
        assertThrows(UnknownPairException.class,
            () -> new ConversionService(repo).convert("AAA", "BBB", 10));
    }
}