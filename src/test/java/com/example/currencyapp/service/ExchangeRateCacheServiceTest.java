package com.example.currencyapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeRateCacheServiceTest {

    private ExchangeRateCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new ExchangeRateCacheService();
    }

    @Test
    void updateRates_shouldUpdateCacheForCurrency() {
        String currencyCode = "USD";
        Map<String, BigDecimal> rates = Map.of("EUR", BigDecimal.valueOf(0.9));

        cacheService.updateRates(currencyCode, rates);

        assertTrue(cacheService.hasRates(currencyCode), "Cache should contain rates for USD.");
        assertEquals(rates, cacheService.getRatesForCurrency(currencyCode), "Rates for USD should match the updated rates.");
    }

    @Test
    void getRatesForCurrency_shouldReturnCorrectRatesForExistingCurrency() {
        String currencyCode = "USD";
        Map<String, BigDecimal> rates = Map.of("EUR", BigDecimal.valueOf(0.9));
        cacheService.updateRates(currencyCode, rates);

        Map<String, BigDecimal> result = cacheService.getRatesForCurrency(currencyCode);

        assertNotNull(result, "Rates for USD should not be null.");
        assertEquals(rates, result, "Rates for USD should match the stored rates.");
    }

    @Test
    void hasRates_shouldReturnTrueWhenRatesExist() {
        String currencyCode = "USD";
        Map<String, BigDecimal> rates = Map.of("EUR", BigDecimal.valueOf(0.9));
        cacheService.updateRates(currencyCode, rates);

        boolean result = cacheService.hasRates(currencyCode);

        assertTrue(result, "Cache should contain rates for USD.");
    }

    @Test
    void hasRates_shouldReturnFalseWhenRatesDoNotExist() {
        String currencyCode = "USD";

        boolean result = cacheService.hasRates(currencyCode);

        assertFalse(result, "Cache should not contain rates for USD.");
    }

    @Test
    void getAllRates_shouldReturnAllRatesInCache() {
        cacheService.updateRates("USD", Map.of("EUR", BigDecimal.valueOf(0.9)));
        cacheService.updateRates("EUR", Map.of("USD", BigDecimal.valueOf(1.1)));

        Map<String, Map<String, BigDecimal>> allRates = cacheService.getAllRates();

        assertEquals(2, allRates.size(), "Cache should contain 2 currencies.");
        assertTrue(allRates.containsKey("USD"), "Cache should contain USD.");
        assertTrue(allRates.containsKey("EUR"), "Cache should contain EUR.");
    }
}
