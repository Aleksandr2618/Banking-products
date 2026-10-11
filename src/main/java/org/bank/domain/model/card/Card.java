package org.bank.domain.model.card;

import org.bank.domain.api.*;
import org.bank.domain.model.AbstractBankProduct;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Currency;

public abstract class Card extends AbstractBankProduct implements Fundable, Withdrawable {

  protected Card(@NotNull String name, @NotNull Currency currency, @NotNull BigDecimal balance) {
    super(name, currency, balance);
  }

  @Override
  public void fund(@NotNull BigDecimal amount) {
    applyFund(amount);
  }

  @Override
  public void withdrawal(@NotNull BigDecimal amount) {
    validateWithdrawal(amount);
    applyWithdrawal(amount);
  }

  protected abstract void validateWithdrawal(BigDecimal amount);
}