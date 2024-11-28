package com.example.currencyapp.controller;

import com.example.currencyapp.dto.CurrencyDto;
import com.example.currencyapp.dto.ExchangeRateDto;
import com.example.currencyapp.service.CurrencyService;
import com.example.currencyapp.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    void shouldGetAllCurrencies() throws Exception {
        List<CurrencyDto> currencies = List.of(new CurrencyDto("USD"), new CurrencyDto("EUR"));
        Mockito.when(currencyService.getAllCurrencies()).thenReturn(currencies);

        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value("USD"))
                .andExpect(jsonPath("$[1].code").value("EUR"));
    }

    @Test
    void shouldAddCurrency() throws Exception {
        Mockito.doNothing().when(currencyService).addCurrency(Mockito.anyString());

        mockMvc.perform(post("/api/v1/currencies")
                        .param("currencyCode", "USD"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Currency USD successfully added."));
    }

    @Test
    void shouldGetExchangeRates() throws Exception {
        List<ExchangeRateDto> rates = List.of(
                new ExchangeRateDto("USD", "EUR", new BigDecimal("0.85")),
                new ExchangeRateDto("USD", "GBP", new BigDecimal("0.75"))
        );
        Mockito.when(exchangeRateService.getExchangeRates("USD")).thenReturn(rates);

        mockMvc.perform(get("/api/v1/currencies/USD/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].baseCurrencyCode").value("USD"))
                .andExpect(jsonPath("$[0].currencyCode").value("EUR"))
                .andExpect(jsonPath("$[0].rate").value("0.85"));
    }
}
