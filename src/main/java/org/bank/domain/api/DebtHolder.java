package org.bank.domain.api;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public interface DebtHolder {

  @NotNull
  BigDecimal getDebt();
}