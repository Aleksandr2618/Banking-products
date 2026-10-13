package org.bank.domain.model.card;

import org.bank.domain.api.DebtHolder;
import org.bank.domain.exception.*;
import org.bank.domain.validator.MoneyValidator;
import org.jetbrains.annotations.NotNull;

import java.math.*;
import java.util.*;

public class CreditCard extends Card implements DebtHolder {

  @NotNull
  private final BigDecimal interestRate;
  @NotNull
  private final BigDecimal creditLimit;

  private static final int INTEREST_CALCULATION_SCALE = 10;
  private static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("100");

  public CreditCard(@NotNull String name,
                    @NotNull Currency currency,
                    @NotNull BigDecimal interestRate,
                    @NotNull BigDecimal creditLimit) {
    this(name, currency, BigDecimal.ZERO, interestRate, creditLimit);
  }

  public CreditCard(@NotNull String name,
                    @NotNull Currency currency,
                    @NotNull BigDecimal balance,
                    @NotNull BigDecimal interestRate,
                    @NotNull BigDecimal creditLimit) {
    super(name, currency, balance);

    validateInterestRate(interestRate);
    this.interestRate = interestRate;

    validateCreditLimit(creditLimit);
    this.creditLimit = creditLimit;
  }

  @NotNull
  public BigDecimal getInterestRate() {
    return interestRate;
  }

  @NotNull
  public BigDecimal getCreditLimit() {
    return creditLimit;
  }

  @Override
  protected void validateWithdrawal(@NotNull BigDecimal amount) {
    BigDecimal resultingBalance = getBalance().subtract(amount);

    if (resultingBalance.negate().compareTo(creditLimit) > 0) {
      throw new InsufficientFundsException(
        "Превышен кредитный лимит: лимит " + creditLimit
          + ", текущий баланс " + getBalance()
          + ", сумма списания " + amount
      );
    }
  }

  @Override
  @NotNull
  public BigDecimal getDebt() {
    int fractionDigits = MoneyValidator.getFractionDigits(getCurrency());

    if (getBalance().signum() >= 0) {
      return BigDecimal.ZERO.setScale(fractionDigits);
    }

    BigDecimal principalDebt = getBalance().negate();

    BigDecimal interest = normalizeMoneyAmount(principalDebt.multiply(interestRate)
      .divide(BigDecimal.valueOf(100), INTEREST_CALCULATION_SCALE, RoundingMode.HALF_EVEN));

    return normalizeMoneyAmount(principalDebt.add(interest));
  }

  @NotNull
  private BigDecimal normalizeMoneyAmount(@NotNull BigDecimal amount) {
    int fractionDigits = MoneyValidator.getFractionDigits(getCurrency());

    return amount.setScale(fractionDigits, RoundingMode.HALF_EVEN);
  }

  private void validateInterestRate(@NotNull BigDecimal interestRate) {
    Objects.requireNonNull(interestRate,"Процентная ставка обязательна");

    if (interestRate.signum() < 0) {
      throw new IllegalArgumentException("Процентная ставка не может быть отрицательной");
    }

    if (interestRate.compareTo(MAX_INTEREST_RATE) > 0) {
      throw new IllegalArgumentException("Процентная ставка не может быть больше 100%, получено: " + interestRate);
    }
  }

  private void validateCreditLimit(@NotNull BigDecimal creditLimit) {
    Objects.requireNonNull(creditLimit, "Кредитный лимит обязателен");

    if (creditLimit.signum() <= 0) {
      throw new IllegalArgumentException("Кредитный лимит должен быть больше 0");
    }
  }
}