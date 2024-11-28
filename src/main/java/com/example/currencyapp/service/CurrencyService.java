package com.example.currencyapp.service;

import com.example.currencyapp.dto.CurrencyDto;

import java.util.List;

public interface CurrencyService {
    List<CurrencyDto> getAllCurrencies();
    void addCurrency(String currencyCode);
}
