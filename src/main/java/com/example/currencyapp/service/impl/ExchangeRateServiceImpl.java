package com.example.currencyapp.service.impl;

import com.example.currencyapp.client.ExchangeRatesClient;
import com.example.currencyapp.dto.ExchangeRateDto;
import com.example.currencyapp.dto.ExchangeRateResponse;
import com.example.currencyapp.entity.Currency;
import com.example.currencyapp.entity.ExchangeRate;
import com.example.currencyapp.exception.CurrencyNotFoundException;
import com.example.currencyapp.repository.CurrencyRepository;
import com.example.currencyapp.repository.ExchangeRateRepository;
import com.example.currencyapp.service.ExchangeRateCacheService;
import com.example.currencyapp.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRatesClient exchangeRatesClient;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateCacheService cacheService;

    @Override
    public List<ExchangeRateDto> getExchangeRates(String baseCurrencyCode) {
        String baseCodeUpperCase = baseCurrencyCode.toUpperCase();

        if (!cacheService.hasRates(baseCodeUpperCase)) {
            log.info("Rates for {} not found in memory. Checking the database...", baseCodeUpperCase);
            Map<String, BigDecimal> ratesMap = loadRatesAndUpdateCache(baseCodeUpperCase);
            cacheService.updateRates(baseCodeUpperCase, ratesMap);
        }

        Map<String, BigDecimal> rates = cacheService.getRatesForCurrency(baseCodeUpperCase);
        return rates.entrySet().stream()
                .map(entry -> new ExchangeRateDto(
                        baseCodeUpperCase,
                        entry.getKey(),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRateString = "${scheduler.update-rates.interval}")
    public void updateAllExchangeRates() {
        log.info("Scheduled task: Updating exchange rates for all currencies...");

        if (currencyRepository.count() == 0) {
            log.warn("No currencies found in the database. Skipping scheduled update.");
            return;
        }

        log.info("Currencies found. Proceeding with exchange rate update...");
        List<Currency> currencies = currencyRepository.findAll();

        currencies.forEach(currency -> {
            try {
                updateRatesFromApi(currency.getCode());
                log.info("Exchange rates for {} updated successfully.", currency.getCode());
            } catch (CurrencyNotFoundException e) {
                log.warn("Currency not found: {}. Skipping...", currency.getCode());
            } catch (Exception e) {
                log.error("Failed to update exchange rates for {}: {}", currency.getCode(), e.getMessage());
            }
        });
    }

    private Map<String, BigDecimal> loadRatesAndUpdateCache(String currencyCode) {
        return currencyRepository.findByCode(currencyCode)
                .map(currency -> {
                    log.info("Rates for {} found in the database. Updating cache...", currencyCode);
                    List<ExchangeRate> rates = exchangeRateRepository.findByCurrency(currency);
                    Map<String, BigDecimal> rateMap = rates.stream()
                            .collect(Collectors.toMap(ExchangeRate::getCode, ExchangeRate::getRate));
                    cacheService.updateRates(currencyCode, rateMap);
                    return rateMap;
                })
                .orElseThrow(() -> new CurrencyNotFoundException("Rates for " + currencyCode + " not found."));
    }

    private void updateRatesFromApi(String baseCurrencyCode) {
        ExchangeRateResponse response = fetchRatesFromApi(baseCurrencyCode);
        Currency baseCurrency = currencyRepository.findByCode(baseCurrencyCode)
                .orElseGet(() -> currencyRepository.save(Currency.builder()
                        .code(baseCurrencyCode)
                        .build()));

        List<ExchangeRate> exchangeRates = mapApiResponseToEntities(baseCurrency, response.getRates());
        exchangeRateRepository.saveAll(exchangeRates);
        cacheService.updateRates(baseCurrencyCode, response.getRates());
    }

    private ExchangeRateResponse fetchRatesFromApi(String currencyCode) {
        try {
            return exchangeRatesClient.getExchangeRates(currencyCode);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new CurrencyNotFoundException("Currency not found in external service: " + currencyCode);
            }
            throw new RuntimeException("External service error.");
        }
    }

    private List<ExchangeRate> mapApiResponseToEntities(Currency baseCurrency, Map<String, BigDecimal> rates) {
        return rates.entrySet().stream()
                .map(entry -> ExchangeRate.builder()
                        .currency(baseCurrency)
                        .code(entry.getKey())
                        .rate(entry.getValue())
                        .timestamp(LocalDateTime.now())
                        .build())
                .toList();
    }
}