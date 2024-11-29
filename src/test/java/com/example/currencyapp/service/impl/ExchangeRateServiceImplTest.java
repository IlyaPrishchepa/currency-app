package com.example.currencyapp.service.impl;

import com.example.currencyapp.client.ExchangeRatesClient;
import com.example.currencyapp.dto.ExchangeRateDto;
import com.example.currencyapp.dto.ExchangeRateResponse;
import com.example.currencyapp.entity.Currency;
import com.example.currencyapp.entity.ExchangeRate;
import com.example.currencyapp.exception.CurrencyNotFoundException;
import com.example.currencyapp.repository.CurrencyRepository;
import com.example.currencyapp.repository.ExchangeRateRepository;
import com.example.currencyapp.cache.ExchangeRateCache;
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
    private ExchangeRateCache cache;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Test
    void getExchangeRates_foundInCache() {
        // Arrange
        String baseCurrency = "USD";
        Map<String, BigDecimal> cachedRates = Map.of("EUR", BigDecimal.valueOf(0.85));
        when(cache.hasRates(baseCurrency)).thenReturn(true);
        when(cache.getRatesForCurrency(baseCurrency)).thenReturn(cachedRates);

        // Act
        List<ExchangeRateDto> result = exchangeRateService.getExchangeRates(baseCurrency);

        // Assert
        assertEquals(1, result.size());
        assertEquals("EUR", result.getFirst().getCurrencyCode());
        assertEquals(BigDecimal.valueOf(0.85), result.getFirst().getRate());
        verify(cache, times(1)).getRatesForCurrency(baseCurrency);
    }

    @Test
    void getExchangeRates_FoundInDbAndCacheUpdated() {
        String baseCurrencyCode = "USD";
        Map<String, BigDecimal> ratesMap = Map.of("EUR", BigDecimal.valueOf(0.9),
                "GBP", BigDecimal.valueOf(0.75));

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

        when(cache.hasRates(baseCurrencyCode)).thenReturn(false);
        when(currencyRepository.findByCode(baseCurrencyCode)).thenReturn(Optional.of(currency));
        when(exchangeRateRepository.findByCurrency(currency)).thenReturn(List.of(exchangeRate1, exchangeRate2));
        when(cache.getRatesForCurrency(baseCurrencyCode)).thenReturn(ratesMap);

        List<ExchangeRateDto> exchangeRates = exchangeRateService.getExchangeRates(baseCurrencyCode);
        exchangeRates.sort(Comparator.comparing(ExchangeRateDto::getCurrencyCode));

        assertNotNull(exchangeRates);
        assertEquals(2, exchangeRates.size());
        assertEquals("USD", exchangeRates.getFirst().getBaseCurrencyCode());
        assertEquals("EUR", exchangeRates.getFirst().getCurrencyCode());
        assertEquals(BigDecimal.valueOf(0.9), exchangeRates.getFirst().getRate());
    }



    @Test
    void getExchangeRates_notFoundAnywhere() {
        // Arrange
        String baseCurrency = "USD";

        when(cache.hasRates(baseCurrency)).thenReturn(false);
        when(currencyRepository.findByCode(baseCurrency)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CurrencyNotFoundException.class, () -> exchangeRateService.getExchangeRates(baseCurrency));
        verify(cache, never()).updateRates(anyString(), anyMap());
    }

    @Test
    void updateAllExchangeRates_noCurrenciesInDatabase() {
        // Arrange
        when(currencyRepository.count()).thenReturn(0L);

        // Act
        exchangeRateService.updateAllExchangeRates();

        // Assert
        verify(exchangeRatesClient, never()).getExchangeRates(anyString());
        verify(exchangeRateRepository, never()).saveAll(anyList());
    }

    @Test
    void updateAllExchangeRates_currenciesExistInDatabase() {
        // Arrange
        List<Currency> currencies = List.of(new Currency(1L, "USD", new ArrayList<>()));
        Map<String, BigDecimal> rates = Map.of("EUR", BigDecimal.valueOf(0.85));
        ExchangeRateResponse response = new ExchangeRateResponse("USD", rates);

        when(currencyRepository.count()).thenReturn(1L);
        when(currencyRepository.findAll()).thenReturn(currencies);
        when(exchangeRatesClient.getExchangeRates("USD")).thenReturn(response);

        // Act
        exchangeRateService.updateAllExchangeRates();

        // Assert
        verify(exchangeRateRepository, times(1)).saveAll(anyList());
        verify(cache, times(1)).updateRates("USD", rates);
    }
}


