package org.bank.domain.validator;

import org.bank.domain.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyValidatorTest {

  private static final Currency USD = Currency.getInstance("USD");

  @Nested
  @DisplayName("Строгая проверка точности (scaleStrict)")
  class ScaleStrictTests {

    @ParameterizedTest(name = "Сумма {0} для {1} преобразуется в {2}")
    @CsvSource({
      "10.50, USD, 10.50",
      "10, USD, 10.00",
      "1000, JPY, 1000",
      "10.500, USD, 10.50",
      "10.000, JPY, 10"
    })
    @DisplayName("Возвращает сумму с точностью валюты без потери значащих цифр")
    void Should_ReturnScaledAmount_When_PrecisionIsValid(
      String amountStr,
      String currencyCode,
      String expectedResultStr
    ) {
      BigDecimal amount = new BigDecimal(amountStr);
      Currency currency = Currency.getInstance(currencyCode);

      BigDecimal result = MoneyValidator.scaleStrict(amount, currency);

      assertThat(result).isEqualByComparingTo(expectedResultStr);
      assertThat(result.scale()).isEqualTo(currency.getDefaultFractionDigits());
    }

    @ParameterizedTest(name = "Сумма {0} для {1} отклоняется")
    @CsvSource({
      "10.555, USD",
      "10.5, JPY",
      "10.001, USD",
      "0.01, JPY"
    })
    @DisplayName("Отклоняет суммы, для которых требуется округление")
    void Should_ThrowException_When_AmountHasExcessFractionDigits(String amountStr, String currencyCode) {

      BigDecimal amount = new BigDecimal(amountStr);
      Currency currency = Currency.getInstance(currencyCode);

      assertThatThrownBy(() -> MoneyValidator.scaleStrict(amount, currency))
        .isInstanceOf(UnsupportedFractionDigitsException.class)
        .hasMessage("Неподдерживаемое количество дробных знаков");
    }

    @Test
    @DisplayName("Выбрасывает исключение, если сумма равна null")
    void Should_ThrowException_When_AmountIsNull() {
      assertThatThrownBy(() -> MoneyValidator.scaleStrict(null, USD))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Сумма обязательна");
    }

    @Test
    @DisplayName("Выбрасывает исключение, если валюта равна null")
    void Should_ThrowException_When_CurrencyIsNull() {
      assertThatThrownBy(() -> MoneyValidator.scaleStrict(new BigDecimal("10.00"), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Валюта обязательна");
    }

    @Test
    @DisplayName("Сохраняет отрицательное значение, если его точность допустима")
    void Should_PreserveNegativeAmount_When_PrecisionIsValid() {
      BigDecimal result = MoneyValidator.scaleStrict(new BigDecimal("-10.50"), USD);

      assertThat(result).isEqualByComparingTo("-10.50");
      assertThat(result.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Отклоняет валюту с неизвестной точностью")
    void Should_ThrowException_When_CurrencyFractionDigitsAreUnknown() {
      Currency unknownCurrency = Currency.getInstance("XTS");

      assertThatThrownBy(() -> MoneyValidator.scaleStrict(new BigDecimal("10.00"), unknownCurrency))
        .isInstanceOf(UnknownFractionDigitsException.class)
        .hasMessage("Неизвестная точность валюты: " + unknownCurrency);
    }
  }

  @Nested
  @DisplayName("Получение количества знаков валюты (getFractionDigits)")
  class GetFractionDigitsTests {

    @ParameterizedTest(name = "Валюта {0} имеет точность {1}")
    @CsvSource({
      "USD, 2",
      "EUR, 2",
      "JPY, 0"
    })
    @DisplayName("Возвращает стандартную точность валюты")
    void Should_ReturnFractionDigits_When_CurrencyIsSupported(String currencyCode, int expectedDigits) {
      Currency currency = Currency.getInstance(currencyCode);

      int digits = MoneyValidator.getFractionDigits(currency);

      assertThat(digits).isEqualTo(expectedDigits);
    }

    @Test
    @DisplayName("Выбрасывает исключение, если точность валюты неизвестна")
    void Should_ThrowException_When_FractionDigitsAreUnknown() {
      Currency unknownCurrency = Currency.getInstance("XXX");

      assertThatThrownBy(() -> MoneyValidator.getFractionDigits(unknownCurrency))
        .isInstanceOf(UnknownFractionDigitsException.class)
        .hasMessage("Неизвестная точность валюты: " + unknownCurrency);
    }
  }
}