package com.example.currencyapp.client;

import com.example.currencyapp.dto.ExchangeRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "exchangeRatesClient", url = "${frankfurter.url}")
public interface ExchangeRatesClient {
    @GetMapping("/latest")
    ExchangeRateResponse getExchangeRates(@RequestParam("base") String baseCurrency);
}
