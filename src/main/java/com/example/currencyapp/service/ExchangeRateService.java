package com.example.currencyapp.service;

import com.example.currencyapp.dto.ExchangeRateDto;

import java.util.List;

public interface ExchangeRateService {
    List<ExchangeRateDto> getExchangeRates(String baseCurrencyCode);
    void updateAllExchangeRates();
}
