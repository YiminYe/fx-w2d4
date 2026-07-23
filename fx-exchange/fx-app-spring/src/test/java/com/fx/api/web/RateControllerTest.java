package com.fx.api.web;

import com.fx.api.model.ConversionResult;
import com.fx.api.model.Rate;
import com.fx.api.repo.RateRepository;
import com.fx.api.service.ConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RateController.class)
class RateControllerTest {

    @Autowired MockMvc mvc;
    @MockBean RateRepository rates;
    @MockBean ConversionService conversions;

    @Test void listsLatestRates() throws Exception {
        when(rates.findLatest()).thenReturn(List.of(
            new Rate(25, "EUR", "USD", 1.0818, LocalDate.of(2026, 1, 12), null)));
        mvc.perform(get("/api/rates"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].baseCode").value("EUR"))
           .andExpect(jsonPath("$[0].rate").value(1.0818));
    }

    @Test void unknownPairIs404WithErrorBody() throws Exception {
        when(rates.findLatestForPair("XX", "YY")).thenReturn(Optional.empty());
        mvc.perform(get("/api/rates/XX/YY"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").exists());
    }

    @Test void validConversionReturns201() throws Exception {
        when(conversions.convert(eq("EUR"), eq("USD"), anyDouble()))
            .thenReturn(new ConversionResult("EUR/USD", 100, 1.0818, 108.18, 1.08, 107.10));
        mvc.perform(post("/api/conversions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"base\":\"EUR\",\"quote\":\"USD\",\"amount\":100}"))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.converted").value(108.18));
    }

    @Test void negativeAmountIs400EvenThoughServiceIsMocked() throws Exception {
        mvc.perform(post("/api/conversions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"base\":\"EUR\",\"quote\":\"USD\",\"amount\":-5}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error").exists());   // 校验逻辑在 Web 层
    }
}