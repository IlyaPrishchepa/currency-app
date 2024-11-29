package com.example.currencyapp.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ExchangeRateCache {
    private final Map<String, Map<String, BigDecimal>> cache = new ConcurrentHashMap<>();

    public Map<String, BigDecimal> getRatesForCurrency(String baseCurrency) {
        return cache.get(baseCurrency);
    }

    public void updateRates(String baseCurrency, Map<String, BigDecimal> rates) {
        cache.put(baseCurrency, rates);
        log.info("Exchange rates for {} updated in memory.", baseCurrency);
    }

    public boolean hasRates(String baseCurrency) {
        return cache.containsKey(baseCurrency);
    }

    public Map<String, Map<String, BigDecimal>> getAllRates() {
        return cache;
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }
}
