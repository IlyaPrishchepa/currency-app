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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRatesClient exchangeRatesClient;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateCacheService cacheService;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Test
    void testGetExchangeRates_cacheMiss_loadFromDbAndUpdateCache() {
        String baseCurrencyCode = "USD";
        Map<String, BigDecimal> ratesMap = Map.of("EUR", BigDecimal.valueOf(0.9), "GBP", BigDecimal.valueOf(0.75));

        Currency currency = Currency.builder()
                .id(1L)
                .code("USD")
                .build();

        ExchangeRate exchangeRate1 = ExchangeRate.builder()
                .currency(currency)
                .code("EUR")
                .rate(BigDecimal.valueOf(0.9))
                .timestamp(LocalDateTime.now())
                .build();
        ExchangeRate exchangeRate2 = ExchangeRate.builder()
                .currency(currency)
                .code("GBP")
                .rate(BigDecimal.valueOf(0.75))
                .timestamp(LocalDateTime.now())
                .build();

        when(cacheService.hasRates(baseCurrencyCode)).thenReturn(false);
        when(currencyRepository.findByCode(baseCurrencyCode)).thenReturn(Optional.of(currency));
        when(exchangeRateRepository.findByCurrency(currency)).thenReturn(List.of(exchangeRate1, exchangeRate2));
        when(cacheService.getRatesForCurrency(baseCurrencyCode)).thenReturn(ratesMap);

        List<ExchangeRateDto> exchangeRates = exchangeRateService.getExchangeRates(baseCurrencyCode);
        exchangeRates.sort(Comparator.comparing(ExchangeRateDto::getCurrencyCode));

        assertNotNull(exchangeRates);
        assertEquals(2, exchangeRates.size());
        assertEquals("USD", exchangeRates.getFirst().getBaseCurrencyCode());
        assertEquals("EUR", exchangeRates.getFirst().getCurrencyCode());
        assertEquals(BigDecimal.valueOf(0.9), exchangeRates.getFirst().getRate());
    }

    @Test
    void testGetExchangeRates_cacheHit_returnRatesFromCache() {
        String baseCurrencyCode = "USD";
        Map<String, BigDecimal> ratesMap = Map.of("EUR", BigDecimal.valueOf(0.9), "GBP", BigDecimal.valueOf(0.75));

        when(cacheService.hasRates(baseCurrencyCode)).thenReturn(true);
        when(cacheService.getRatesForCurrency(baseCurrencyCode)).thenReturn(ratesMap);

        List<ExchangeRateDto> exchangeRates = exchangeRateService.getExchangeRates(baseCurrencyCode);
        exchangeRates.sort(Comparator.comparing(ExchangeRateDto::getCurrencyCode));

        assertNotNull(exchangeRates);
        assertEquals(2, exchangeRates.size());
        assertEquals("USD", exchangeRates.getFirst().getBaseCurrencyCode());
        assertEquals("EUR", exchangeRates.getFirst().getCurrencyCode());
        assertEquals(BigDecimal.valueOf(0.9), exchangeRates.getFirst().getRate());
    }

    @Test
    void testGetExchangeRates_currencyNotFoundInDb_throwsCurrencyNotFoundException() {
        String baseCurrencyCode = "XYZ";

        when(cacheService.hasRates(baseCurrencyCode)).thenReturn(false);
        when(currencyRepository.findByCode(baseCurrencyCode)).thenReturn(Optional.empty());

        assertThrows(CurrencyNotFoundException.class, () -> exchangeRateService.getExchangeRates(baseCurrencyCode));
    }

    @Test
    void testUpdateAllExchangeRates_noCurrenciesInDb_logsWarning() {
        when(currencyRepository.count()).thenReturn(0L);

        exchangeRateService.updateAllExchangeRates();

        verify(exchangeRatesClient, never()).getExchangeRates(anyString());
    }

    @Test
    void testUpdateAllExchangeRates_successfulUpdate() {
        Currency usd = Currency.builder()
                .id(1L)
                .code("USD")
                .build();
        List<Currency> currencies = List.of(usd);
        Map<String, BigDecimal> rates = Map.of("EUR", BigDecimal.valueOf(0.9), "GBP", BigDecimal.valueOf(0.75));
        ExchangeRateResponse response = new ExchangeRateResponse("USD", rates);

        when(currencyRepository.count()).thenReturn(1L);
        when(currencyRepository.findAll()).thenReturn(currencies);
        when(exchangeRatesClient.getExchangeRates("USD")).thenReturn(response);
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(exchangeRateRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

        exchangeRateService.updateAllExchangeRates();

        verify(exchangeRatesClient, times(1)).getExchangeRates("USD");
        verify(exchangeRateRepository, times(1)).saveAll(anyList());
        verify(cacheService, times(1)).updateRates(eq("USD"), eq(rates));
    }

    @Test
    void testUpdateAllExchangeRates_externalApiError_logsError() {
        Currency usd = Currency.builder()
                .id(1L)
                .code("USD")
                .build();
        List<Currency> currencies = List.of(usd);

        when(currencyRepository.count()).thenReturn(1L);
        when(currencyRepository.findAll()).thenReturn(currencies);
        when(exchangeRatesClient.getExchangeRates("USD")).thenThrow(new RuntimeException("External service error"));

        exchangeRateService.updateAllExchangeRates();

        verify(exchangeRatesClient, times(1)).getExchangeRates("USD");
        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void testUpdateAllExchangeRates_currencyNotFoundInApi_logsWarning() {
        Currency usd = Currency.builder()
                .id(1L)
                .code("USD")
                .build();
        List<Currency> currencies = List.of(usd);

        when(currencyRepository.count()).thenReturn(1L);
        when(currencyRepository.findAll()).thenReturn(currencies);
        when(exchangeRatesClient.getExchangeRates("USD")).thenThrow(new CurrencyNotFoundException("Currency not found"));

        exchangeRateService.updateAllExchangeRates();

        verify(exchangeRatesClient, times(1)).getExchangeRates("USD");
        verifyNoInteractions(exchangeRateRepository);
    }
}
