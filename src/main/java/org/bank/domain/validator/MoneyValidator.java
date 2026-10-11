package org.bank.domain.validator;

import org.bank.domain.exception.*;
import org.jetbrains.annotations.NotNull;

import java.math.*;
import java.util.*;

public final class MoneyValidator {

  private MoneyValidator() {
  }

  @NotNull
  public static BigDecimal scaleStrict(@NotNull BigDecimal amount, @NotNull Currency currency) {
    Objects.requireNonNull(amount, "Сумма обязательна");
    Objects.requireNonNull(currency, "Валюта обязательна");

    try {
      return amount.setScale(getFractionDigits(currency), RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new UnsupportedFractionDigitsException("Неподдерживаемое количество дробных знаков");
    }
  }

  public static int getFractionDigits(Currency currency) {
    int fractionDigits = currency.getDefaultFractionDigits();

    if (fractionDigits < 0) {
      throw new UnknownFractionDigitsException("Неизвестная точность валюты: " + currency);
    }

    return fractionDigits;
  }
}