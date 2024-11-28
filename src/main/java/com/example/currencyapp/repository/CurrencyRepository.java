package com.example.currencyapp.repository;

import com.example.currencyapp.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    Boolean existsByCode(String code);
    Optional<Currency> findByCode(String code);
}
