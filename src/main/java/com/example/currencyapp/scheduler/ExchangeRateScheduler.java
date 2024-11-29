package com.example.currencyapp.scheduler;

import com.example.currencyapp.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateService exchangeRateService;

    @Scheduled(fixedRateString = "${scheduler.update-rates.interval}")
    public void updateAllExchangeRates() {
        log.info("Scheduled task: Updating exchange rates for all currencies...");
        try {
            exchangeRateService.updateAllExchangeRates();
        } catch (Exception e) {
            log.error("Failed to update exchange rates: {}", e.getMessage());
        }
    }
}
