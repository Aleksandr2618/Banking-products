package org.bank.domain.model.deposit;

import org.bank.domain.api.*;
import org.bank.domain.exception.ClosedProductException;
import org.bank.domain.model.AbstractBankProduct;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Currency;

public class Deposit extends AbstractBankProduct implements Fundable, Closable {

  private boolean closed;

  public Deposit(@NotNull String name, @NotNull Currency currency) {
    super(name, currency, BigDecimal.ZERO);
  }

  public Deposit(@NotNull String name, @NotNull Currency currency, @NotNull BigDecimal balance) {
    super(name, currency, balance);
  }

  @Override
  public void fund(BigDecimal amount) {
    checkActive();
    applyFund(amount);
  }

  @Override
  public void close() {
    checkActive();
    closed = true;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  private void checkActive() {
    if (isClosed()) {
      throw new ClosedProductException("Вклад '" + getName() + "' закрыт");
    }
  }
}