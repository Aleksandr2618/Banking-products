package org.bank.domain.model.card;

import org.bank.domain.exception.InsufficientFundsException;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Currency;

public class DebitCard extends Card {

  public DebitCard(@NotNull String name, @NotNull Currency currency) {
    this(name, currency, BigDecimal.ZERO);
  }

  public DebitCard(@NotNull String name, @NotNull Currency currency, @NotNull BigDecimal balance) {
    super(name, currency, balance);
  }

  @Override
  protected void validateWithdrawal(@NotNull BigDecimal amount) {
    if (getBalance().compareTo(amount) < 0) {
      throw new InsufficientFundsException(
        "Недостаточно средств: баланс " + getBalance() + ", сумма списания " + amount
      );
    }
  }
}