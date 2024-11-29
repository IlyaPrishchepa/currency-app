package com.example.currencyapp.service.impl;

import com.example.currencyapp.client.ExchangeRatesClient;
import com.example.currencyapp.dto.CurrencyDto;
import com.example.currencyapp.dto.ExchangeRateResponse;
import com.example.currencyapp.entity.Currency;
import com.example.currencyapp.entity.ExchangeRate;
import com.example.currencyapp.exception.CurrencyAlreadyExistsException;
import com.example.currencyapp.exception.CurrencyNotFoundException;
import com.example.currencyapp.repository.CurrencyRepository;
import com.example.currencyapp.repository.ExchangeRateRepository;
import com.example.currencyapp.service.CurrencyService;
import com.example.currencyapp.cache.ExchangeRateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyServiceImpl implements CurrencyService {
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRatesClient exchangeRatesClient;
    private final ExchangeRateCache cache;

    @Override
    public List<CurrencyDto> getAllCurrencies() {
        if (!cache.isEmpty()) {
            return getCurrenciesFromCache();
        }

        List<Currency> currencies = getCurrenciesFromDatabase();
        updateCacheFromDatabase(currencies);
        return mapCurrenciesToDto(currencies);
    }

    @Override
    public void addCurrency(String currencyCode) {
        String upperCurrencyCode = currencyCode.toUpperCase();

        if (currencyRepository.existsByCode(upperCurrencyCode)) {
            throw new CurrencyAlreadyExistsException("Currency with code " + upperCurrencyCode + " already exists.");
        }

        ExchangeRateResponse response = fetchExchangeRatesFromApi(upperCurrencyCode);
        saveCurrencyAndExchangeRates(upperCurrencyCode, response.getRates());
        cache.updateRates(upperCurrencyCode, response.getRates());

        log.info("Currency {} successfully added.", upperCurrencyCode);
    }

    private List<CurrencyDto> getCurrenciesFromCache() {
        log.info("Returning currencies from cache.");
        return cache.getAllRates().keySet().stream()
                .map(CurrencyDto::new)
                .toList();
    }

    private List<Currency> getCurrenciesFromDatabase() {
        log.info("Loading currencies from database...");
        List<Currency> currencies = currencyRepository.findAll();
        if (currencies.isEmpty()) {
            throw new CurrencyNotFoundException("No currencies available.");
        }
        return currencies;
    }

    private void updateCacheFromDatabase(List<Currency> currencies) {
        log.info("Updating cache with data from database...");
        currencies.forEach(currency -> {
            List<ExchangeRate> rates = exchangeRateRepository.findByCurrency(currency);
            Map<String, BigDecimal> rateMap = rates.stream()
                    .collect(Collectors.toMap(ExchangeRate::getCode, ExchangeRate::getRate));
            cache.updateRates(currency.getCode(), rateMap);
        });
    }

    private List<CurrencyDto> mapCurrenciesToDto(List<Currency> currencies) {
        return currencies.stream()
                .map(currency -> new CurrencyDto(currency.getCode()))
                .toList();
    }

    private ExchangeRateResponse fetchExchangeRatesFromApi(String currencyCode) {
        try {
            return exchangeRatesClient.getExchangeRates(currencyCode);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new CurrencyNotFoundException("Currency not found in external service: " + currencyCode);
            }
            throw new RuntimeException("External service error.");
        }
    }

    private void saveCurrencyAndExchangeRates(String currencyCode, Map<String, BigDecimal> rates) {
        Currency currency = currencyRepository.save(
                Currency.builder()
                        .code(currencyCode)
                        .build()
        );

        List<ExchangeRate> exchangeRates = rates.entrySet().stream()
                .map(entry -> ExchangeRate.builder()
                        .currency(currency)
                        .code(entry.getKey())
                        .rate(entry.getValue())
                        .timestamp(LocalDateTime.now())
                        .build())
                .toList();

        exchangeRateRepository.saveAll(exchangeRates);
    }
}