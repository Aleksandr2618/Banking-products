package org.bank.domain.model;

import org.bank.domain.api.BankProduct;
import org.bank.domain.exception.*;
import org.bank.domain.validator.MoneyValidator;
import org.jetbrains.annotations.NotNull;

import java.math.*;
import java.util.*;

public abstract class AbstractBankProduct implements BankProduct {

  @NotNull
  private final String name;
  @NotNull
  private final Currency currency;
  @NotNull
  private BigDecimal balance;

  protected AbstractBankProduct(@NotNull String name, @NotNull Currency currency, @NotNull BigDecimal balance) {
    this.name = Objects.requireNonNull(name, "Название продукта обязательно");
    if (name.isBlank()) {
      throw new IllegalArgumentException("Название продукта не может быть пустым");
    }

    this.currency = Objects.requireNonNull(currency, "Выбор валюты обязателен");

    validateInitialBalance(balance);

    this.balance = MoneyValidator.scaleStrict(balance, currency);
  }

  @Override
  @NotNull
  public String getName() {
    return name;
  }

  @Override
  @NotNull
  public Currency getCurrency() {
    return currency;
  }

  @Override
  @NotNull
  public BigDecimal getBalance() {
    return balance;
  }

  protected void requirePositiveAmount(@NotNull BigDecimal amount) {
    Objects.requireNonNull(amount, "Сумма операции обязательна");

    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("Сумма операции должна быть больше нуля: " + amount);
    }
  }

  protected final void applyFund(@NotNull BigDecimal amount) {
    requirePositiveAmount(amount);

    balance = balance.add(MoneyValidator.scaleStrict(amount, getCurrency()));
  }

  protected final void applyWithdrawal(@NotNull BigDecimal amount) {
    requirePositiveAmount(amount);

    balance = balance.subtract(MoneyValidator.scaleStrict(amount, getCurrency()));
  }

  private void validateInitialBalance(@NotNull BigDecimal balance) {
    Objects.requireNonNull(balance, "Начальный баланс обязателен");

    if (balance.signum() < 0) {
      throw new IllegalArgumentException("Начальный баланс не может быть отрицательным");
    }
  }
}
