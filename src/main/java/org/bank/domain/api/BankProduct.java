package org.bank.domain.api;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Currency;

public interface BankProduct {

  @NotNull
  String getName();

  @NotNull
  Currency getCurrency();

  @NotNull
  BigDecimal getBalance();
}
