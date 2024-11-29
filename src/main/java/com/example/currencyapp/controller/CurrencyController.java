package com.example.currencyapp.controller;
import com.example.currencyapp.dto.CurrencyDto;
import com.example.currencyapp.dto.ExchangeRateDto;
import com.example.currencyapp.service.CurrencyService;
import com.example.currencyapp.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
public class CurrencyController {
    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "Get all available currencies", description = "Returns a list of all available currencies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of currencies"),
            @ApiResponse(responseCode = "404", description = "No currencies found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        List<CurrencyDto> currencies = currencyService.getAllCurrencies();
        return ResponseEntity.ok(currencies);
    }

    @Operation(summary = "Add a new currency", description = "Adds a new currency to the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of currencies"),
            @ApiResponse(responseCode = "404", description = "No currencies found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String addCurrency(@RequestParam String currencyCode) {
        currencyService.addCurrency(currencyCode);
        return "Currency " + currencyCode.toUpperCase() + " successfully added.";
    }

    @Operation(summary = "Get exchange rates for a currency", description = "Fetches exchange rates for the specified currency.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved exchange rates"),
            @ApiResponse(responseCode = "404", description = "Currency not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    @GetMapping("/{currencyCode}/rates")
    public ResponseEntity<List<ExchangeRateDto>> getExchangeRateByCode(@PathVariable String currencyCode) {
        List<ExchangeRateDto> rates = exchangeRateService.getExchangeRates(currencyCode);
        return ResponseEntity.ok(rates);
    }
}