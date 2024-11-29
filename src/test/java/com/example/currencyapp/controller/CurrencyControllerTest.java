package com.example.currencyapp.controller;

import com.example.currencyapp.dto.CurrencyDto;
import com.example.currencyapp.dto.ExchangeRateDto;
import com.example.currencyapp.exception.CurrencyAlreadyExistsException;
import com.example.currencyapp.exception.CurrencyNotFoundException;
import com.example.currencyapp.exception.ExternalServiceException;
import com.example.currencyapp.service.CurrencyService;
import com.example.currencyapp.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {
    @MockBean
    private CurrencyService currencyService;
    @MockBean
    private ExchangeRateService exchangeRateService;
    @Autowired
    private MockMvc mockMvc;


    @Test
    void getAllCurrencies() throws Exception {
        List<CurrencyDto> currencies = List.of(new CurrencyDto("USD"),
                new CurrencyDto("EUR"));

        when(currencyService.getAllCurrencies()).thenReturn(currencies);
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("USD"))
                .andExpect(jsonPath("$[1].code").value("EUR"));
        verify(currencyService, times(1)).getAllCurrencies();
    }

    @Test
    void getAllCurrencies_noCurrenciesFound() throws Exception {
        when(currencyService.getAllCurrencies()).thenThrow(new CurrencyNotFoundException("No currencies available."));

        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No currencies available."));

        verify(currencyService, times(1)).getAllCurrencies();
    }

    @Test
    void addCurrency() throws Exception {
        doNothing().when(currencyService).addCurrency("USD");
        mockMvc.perform(post("/api/v1/currencies").param("currencyCode", "USD"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Currency USD successfully added."));
        verify(currencyService, times(1)).addCurrency("USD");
    }

    @Test
    void addCurrency_alreadyExists() throws Exception {
        doThrow(new CurrencyAlreadyExistsException("Currency with code USD already exists."))
                .when(currencyService).addCurrency("USD");

        mockMvc.perform(post("/api/v1/currencies").param("currencyCode", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Currency with code USD already exists."));

        verify(currencyService, times(1)).addCurrency("USD");
    }

    @Test
    void addCurrency_externalServiceError() throws Exception {
        doThrow(new ExternalServiceException("External service error."))
                .when(currencyService).addCurrency("XXX");

        mockMvc.perform(post("/api/v1/currencies").param("currencyCode", "XXX"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("External service error."));

        verify(currencyService, times(1)).addCurrency("XXX");
    }


    @Test
    void getExchangeRateByCode() throws Exception {
        List<ExchangeRateDto> rateDtos = List.of(
                new ExchangeRateDto("USD", "EUR", new BigDecimal("0.94859")),
                new ExchangeRateDto("USD", "PLN", new BigDecimal("4.0884")));
        when(exchangeRateService.getExchangeRates("USD")).thenReturn(rateDtos);
        mockMvc.perform(get("/api/v1/currencies/{currencyCode}/rates", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].baseCurrencyCode").value("USD"))
                .andExpect(jsonPath("$[0].currencyCode").value("EUR"))
                .andExpect(jsonPath("$[0].rate").value("0.94859"))
                .andExpect(jsonPath("$[1].baseCurrencyCode").value("USD"))
                .andExpect(jsonPath("$[1].currencyCode").value("PLN"))
                .andExpect(jsonPath("$[1].rate").value("4.0884"));
        verify(exchangeRateService, times(1)).getExchangeRates("USD");
    }

    @Test
    void getExchangeRateByCode_currencyNotFound() throws Exception {
        when(exchangeRateService.getExchangeRates("USD"))
                .thenThrow(new CurrencyNotFoundException("Currency not found: USD"));

        mockMvc.perform(get("/api/v1/currencies/{currencyCode}/rates", "USD"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Currency not found: USD"));

        verify(exchangeRateService, times(1)).getExchangeRates("USD");
    }

    @Test
    void getExchangeRateByCode_internalServerError() throws Exception {
        when(exchangeRateService.getExchangeRates("USD"))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/v1/currencies/{currencyCode}/rates", "USD"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred."));

        verify(exchangeRateService, times(1)).getExchangeRates("USD");
    }
}