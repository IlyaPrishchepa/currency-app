package com.example.currencyapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDto {
    private String baseCurrencyCode;
    private String currencyCode;
    private BigDecimal rate;
}

