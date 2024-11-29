package com.example.currencyapp.service.impl;

import com.example.currencyapp.cache.ExchangeRateCache;
import com.example.currencyapp.client.ExchangeRatesClient;
import com.example.currencyapp.dto.CurrencyDto;
import com.example.currencyapp.dto.ExchangeRateResponse;
import com.example.currencyapp.entity.Currency;
import com.example.currencyapp.exception.CurrencyAlreadyExistsException;
import com.example.currencyapp.exception.CurrencyNotFoundException;
import com.example.currencyapp.repository.CurrencyRepository;
import com.example.currencyapp.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceImplTest {

    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    @Mock
    private ExchangeRatesClient exchangeRatesClient;
    @Mock
    private ExchangeRateCache cache;

    @InjectMocks
    private CurrencyServiceImpl currencyService;

    private String currencyCode;
    private Map<String, BigDecimal> rates;
    private Currency currency;

    @BeforeEach
    void setUp() {
        currencyCode = "USD";
        rates = Map.of("EUR", BigDecimal.valueOf(0.85));
        currency = Currency.builder()
                .id(1L)
                .code("USD")
                .build();}

    @Test
    void getAllCurrencies_ReturnsFromCache() {
        Map<String, Map<String, BigDecimal>> cachedRates = Map.of(
                "USD", Map.of("EUR", BigDecimal.valueOf(0.85))
        );
        when(cache.isEmpty()).thenReturn(false);
        when(cache.getAllRates()).thenReturn(cachedRates);

        List<CurrencyDto> result = currencyService.getAllCurrencies();

        assertEquals(1, result.size());
        assertEquals("USD", result.getFirst().getCode());

        verify(cache, times(1)).getAllRates();
        verifyNoInteractions(currencyRepository, exchangeRateRepository);
    }

    @Test
    void getAllCurrencies_ReturnsFromDatabase() {
        List<Currency> currencies = List.of(currency);

        when(cache.isEmpty()).thenReturn(true);
        when(currencyRepository.findAll()).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrency(currency)).thenReturn(List.of());

        List<CurrencyDto> result = currencyService.getAllCurrencies();

        assertEquals(1, result.size());
        assertEquals("USD", result.getFirst().getCode());

        verify(currencyRepository, times(1)).findAll();
        verify(exchangeRateRepository, times(1)).findByCurrency(currency);
        verify(cache, times(1)).updateRates(eq("USD"), any());
    }

    @Test
    void getAllCurrencies_NoDataAvailable() {
        when(cache.isEmpty()).thenReturn(true);
        when(currencyRepository.findAll()).thenReturn(List.of());

        CurrencyNotFoundException exception = assertThrows(CurrencyNotFoundException.class,
                () -> currencyService.getAllCurrencies());
        assertEquals("No currencies available.", exception.getMessage());
    }

    @Test
    void addCurrency() {
        ExchangeRateResponse response = new ExchangeRateResponse(currencyCode,rates);

        when(currencyRepository.existsByCode(currencyCode)).thenReturn(false);
        when(exchangeRatesClient.getExchangeRates(currencyCode)).thenReturn(response);
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);

        currencyService.addCurrency(currencyCode);

        verify(currencyRepository, times(1)).save(any(Currency.class));
        verify(exchangeRateRepository, times(1)).saveAll(anyList());
        verify(cache, times(1)).updateRates(eq(currencyCode), eq(rates));
    }

    @Test
    void addCurrency_CurrencyAlreadyExists() {
        when(currencyRepository.existsByCode(currencyCode)).thenReturn(true);

        CurrencyAlreadyExistsException exception = assertThrows(CurrencyAlreadyExistsException.class, () -> currencyService.addCurrency(currencyCode));
        assertEquals("Currency with code USD already exists.", exception.getMessage());
    }

    @Test
    void addCurrency_ApiError() {
        when(currencyRepository.existsByCode(currencyCode)).thenReturn(false);
        when(exchangeRatesClient.getExchangeRates(currencyCode)).
                thenThrow(new RuntimeException("API error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> currencyService.addCurrency(currencyCode));
        assertEquals("API error", exception.getMessage());
    }

    @Test
    void addCurrency_CurrencyNotFoundInApi() {
        when(currencyRepository.existsByCode(currencyCode)).thenReturn(false);
        when(exchangeRatesClient.getExchangeRates(currencyCode)).
                thenThrow(new RuntimeException("Currency not found"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> currencyService.addCurrency(currencyCode));
        assertEquals("Currency not found", exception.getMessage());
    }
}
