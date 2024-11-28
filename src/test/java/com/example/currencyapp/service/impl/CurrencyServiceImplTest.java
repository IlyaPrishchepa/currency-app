package com.example.currencyapp.service.impl;

import com.example.currencyapp.client.ExchangeRatesClient;
import com.example.currencyapp.dto.ExchangeRateResponse;
import com.example.currencyapp.entity.Currency;
import com.example.currencyapp.exception.CurrencyAlreadyExistsException;
import com.example.currencyapp.repository.CurrencyRepository;
import com.example.currencyapp.repository.ExchangeRateRepository;
import com.example.currencyapp.service.ExchangeRateCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyServiceImplTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRatesClient exchangeRatesClient;

    @Mock
    private ExchangeRateCacheService cacheService;

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    private Currency currency;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        currency = Currency.builder()
                .id(1L)
                .code("USD")
                .build();
    }

    @Test
    void testGetAllCurrencies_cacheHit_returnRatesFromCache() {
        when(cacheService.getAllRates()).thenReturn(Map.of("USD", Map.of("EUR", BigDecimal.valueOf(0.85))));

        var result = currencyService.getAllCurrencies();

        verify(cacheService, times(2)).getAllRates();
        assertEquals(1, result.size());
        assertEquals("USD", result.getFirst().getCode());
    }

    @Test
    void testAddCurrency_success() {
        String currencyCode = "EUR";
        Map<String, BigDecimal> rates = Map.of("USD", BigDecimal.valueOf(1.1));

        ExchangeRateResponse response = new ExchangeRateResponse();
        response.setBase(currencyCode);
        response.setRates(rates);

        when(currencyRepository.existsByCode(currencyCode)).thenReturn(false);
        when(exchangeRatesClient.getExchangeRates(currencyCode)).thenReturn(response);

        currencyService.addCurrency(currencyCode);

        verify(currencyRepository, times(1)).save(any(Currency.class));
        verify(exchangeRateRepository, times(1)).saveAll(anyList());
        verify(cacheService, times(1)).updateRates(eq(currencyCode), eq(rates));
    }

    @Test
    void testAddCurrency_currencyAlreadyExists() {
        String currencyCode = "USD";

        when(currencyRepository.existsByCode(currencyCode)).thenReturn(true);

        CurrencyAlreadyExistsException exception = assertThrows(CurrencyAlreadyExistsException.class, () -> {
            currencyService.addCurrency(currencyCode);
        });

        assertEquals("Currency with code USD already exists.", exception.getMessage());
    }

    @Test
    void testGetAllCurrencies_databaseHit() {
        when(currencyRepository.findAll()).thenReturn(Collections.singletonList(currency));

        var result = currencyService.getAllCurrencies();

        assertEquals(1, result.size());
        assertEquals("USD", result.getFirst().getCode());
    }
}
