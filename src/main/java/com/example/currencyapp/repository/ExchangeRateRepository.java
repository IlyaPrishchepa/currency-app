package com.example.currencyapp.repository;

import com.example.currencyapp.entity.Currency;
import com.example.currencyapp.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Integer> {
    List<ExchangeRate> findByCurrency(Currency currency);
}
