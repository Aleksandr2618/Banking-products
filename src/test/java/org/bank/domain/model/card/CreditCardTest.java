package org.bank.domain.model.card;

import org.bank.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditCardTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final Currency JPY = Currency.getInstance("JPY");

  private static final String CARD_NAME = "Credit card";
  private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");
  private static final BigDecimal INTEREST_RATE = new BigDecimal("20");
  private static final BigDecimal CREDIT_LIMIT = new BigDecimal("1000.00");

  private CreditCard card;

  @BeforeEach
  void setUp() {
    card = new CreditCard(
      CARD_NAME,
      USD,
      INITIAL_BALANCE,
      INTEREST_RATE,
      CREDIT_LIMIT
    );
  }

  @Nested
  @DisplayName("Конструктор")
  class ConstructorTests {

    @Test
    @DisplayName("Создает карту с корректными параметрами")
    void Should_InitializeCard_When_ValidParametersProvided() {
      assertThat(card.getName()).isEqualTo(CARD_NAME);
      assertThat(card.getCurrency()).isEqualTo(USD);
      assertThat(card.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
      assertThat(card.getInterestRate()).isEqualByComparingTo(INTEREST_RATE);
      assertThat(card.getCreditLimit()).isEqualByComparingTo(CREDIT_LIMIT);
    }

    @Test
    @DisplayName("Устанавливает нулевой баланс в конструкторе без balance")
    void Should_InitializeZeroBalance_When_BalanceIsNotProvided() {
      CreditCard newCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      assertThat(newCard.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Допускает нулевую процентную ставку")
    void Should_AcceptRate_When_InterestRateIsZero() {
      CreditCard newCard = new CreditCard(
        CARD_NAME,
        USD,
        BigDecimal.ZERO,
        CREDIT_LIMIT
      );

      assertThat(newCard.getInterestRate()).isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"99.9", "100"})
    @DisplayName("Допускает процентную ставку до 100%")
    void Should_AcceptRate_When_InterestRateIsUpToOneHundredPercent(String rate) {
      CreditCard newCard = new CreditCard(
        CARD_NAME,
        USD,
        new BigDecimal(rate),
        CREDIT_LIMIT
      );

      assertThat(newCard.getInterestRate()).isEqualByComparingTo(rate);
    }

    @Test
    @DisplayName("Отклоняет отрицательную процентную ставку")
    void Should_ThrowException_When_InterestRateIsNegative() {
      BigDecimal rate = new BigDecimal("-20");

      assertThatThrownBy(() -> new CreditCard(
        CARD_NAME,
        USD,
        rate,
        CREDIT_LIMIT
      ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Процентная ставка не может быть отрицательной");
    }

    @ParameterizedTest
    @ValueSource(strings = {"100.01", "101"})
    @DisplayName("Отклоняет процентную ставку выше 100%")
    void Should_ThrowException_When_InterestRateExceedsOneHundredPercent(String rate) {
      assertThatThrownBy(() -> new CreditCard(
        CARD_NAME,
        USD,
        new BigDecimal(rate),
        CREDIT_LIMIT
      ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Процентная ставка не может быть больше 100%, получено: " + rate);
    }

    @Test
    @DisplayName("Отклоняет null вместо процентной ставки")
    void Should_ThrowException_When_InterestRateIsNull() {
      assertThatThrownBy(() -> new CreditCard(
        CARD_NAME,
        USD,
        null,
        CREDIT_LIMIT
      ))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Процентная ставка обязательна");
    }

    @Test
    @DisplayName("Отклоняет null вместо кредитного лимита")
    void Should_ThrowException_When_CreditLimitIsNull() {
      assertThatThrownBy(() -> new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        null
      ))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Кредитный лимит обязателен");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-0.01", "-1"})
    @DisplayName("Отклоняет нулевой и отрицательный кредитный лимит")
    void Should_ThrowException_When_CreditLimitIsNonPositive(String limit) {
      assertThatThrownBy(() -> new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        new BigDecimal(limit)
      ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Кредитный лимит должен быть больше 0");
    }
  }

  @Nested
  @DisplayName("Геттеры")
  class GetterTests {

    @Test
    @DisplayName("Возвращает параметры кредитной карты")
    void Should_ReturnConfiguredProperties_When_GettersCalled() {
      assertThat(card.getName()).isEqualTo(CARD_NAME);
      assertThat(card.getCurrency()).isEqualTo(USD);
      assertThat(card.getInterestRate()).isEqualByComparingTo("20");
      assertThat(card.getCreditLimit()).isEqualByComparingTo("1000.00");
      assertThat(card.getBalance()).isEqualByComparingTo("100.00");
    }
  }

  @Nested
  @DisplayName("Снятие средств")
  class WithdrawalTests {

    @Test
    @DisplayName("Списывает средства в пределах собственного баланса")
    void Should_DecreaseBalance_When_WithdrawnWithinOwnBalance() {
      card.withdrawal(new BigDecimal("30.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Допускает снятие ровно на весь доступный объем средств")
    void Should_AllowWithdrawal_When_AmountEqualsTotalAvailableFunds() {
      card.withdrawal(new BigDecimal("1100.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("-1000.00");
    }

    @Test
    @DisplayName("Допускает частичное использование кредитного лимита")
    void Should_AllowWithdrawal_When_CreditLimitIsPartiallyUsed() {
      card.withdrawal(new BigDecimal("150.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("-50.00");
    }

    @Test
    @DisplayName("Допускает списание до нуля собственного баланса")
    void Should_AllowWithdrawal_When_AmountEqualsBalance() {
      card.withdrawal(new BigDecimal("100.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Отклоняет снятие при превышении лимита на одну копейку")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalExceedsCreditLimitByOneCent() {
      BigDecimal balanceBefore = card.getBalance();
      BigDecimal amount = new BigDecimal("1100.01");

      assertThatThrownBy(() -> card.withdrawal(amount))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessage(
          "Превышен кредитный лимит: лимит 1000.00"
            + ", текущий баланс 100.00"
            + ", сумма списания 1100.01");

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет снятие при значительном превышении лимита")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalSignificantlyExceedsCreditLimit() {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.withdrawal(new BigDecimal("5000.00")))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessage("Превышен кредитный лимит: лимит 1000.00"
          + ", текущий баланс 100.00"
          + ", сумма списания 5000.00");

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет null в качестве суммы снятия")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalAmountIsNull() {
      assertThatThrownBy(() -> card.withdrawal(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Сумма списания обязательна");

      assertThat(card.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
    }

    @Test
    @DisplayName("Отклоняет нулевую сумму снятия")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalAmountIsZero() {
      assertThatThrownBy(() -> card.withdrawal(BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Сумма операции должна быть больше нуля: 0");

      assertThat(card.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
    }

    @Test
    @DisplayName("Отклоняет отрицательную сумму снятия")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalAmountIsNegative() {
      assertThatThrownBy(() -> card.withdrawal(new BigDecimal("-10.00")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Сумма операции должна быть больше нуля: -10.00");

      assertThat(card.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
    }

    @Test
    @DisplayName("Позволяет снять средства после неудачной операции")
    void Should_AllowWithdrawal_When_AttemptedAfterRejectedOperation() {
      assertThatThrownBy(() -> card.withdrawal(new BigDecimal("1100.01")))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessage("Превышен кредитный лимит: лимит 1000.00, текущий баланс 100.00, сумма списания 1100.01");

      card.withdrawal(new BigDecimal("30.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Обрабатывает очень большие денежные суммы")
    void Should_HandleLargeAmounts_When_WithdrawalExecuted() {
      BigDecimal hugeAmount = new BigDecimal("100000000000000000000000000000000000000000000000000000000000.00");

      CreditCard hugeLimitCard = new CreditCard(
        CARD_NAME,
        USD,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        hugeAmount
      );

      hugeLimitCard.withdrawal(hugeAmount);

      assertThat(hugeLimitCard.getBalance()).isEqualByComparingTo(hugeAmount.negate());
    }
  }

  @Nested
  @DisplayName("Расчет задолженности")
  class DebtTests {

    @Test
    @DisplayName("Возвращает нулевой долг при положительном балансе")
    void Should_ReturnZeroDebt_When_BalanceIsPositive() {
      assertThat(card.getDebt()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Сохраняет точность знаков после запятой для выбранной валюты при расчете задолженности")
    void Should_HaveTwoFractionDigits_When_DebtCalculatedInUsd() {
      assertThat(card.getDebt().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Возвращает нулевой долг при нулевом балансе")
    void Should_ReturnZeroDebt_When_BalanceIsZero() {
      CreditCard zeroBalanceCard = new CreditCard(
        CARD_NAME,
        USD,
        BigDecimal.ZERO,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      assertThat(zeroBalanceCard.getDebt()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Рассчитывает долг с учетом процентов")
    void Should_CalculateDebt_When_InterestRateIsApplied() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      assertThat(debtCard.getDebt()).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("Рассчитывает только основной долг при ставке 0%")
    void Should_ReturnPrincipalDebtOnly_When_InterestRateIsZero() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        BigDecimal.ZERO,
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      assertThat(debtCard.getDebt()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Рассчитывает долг при отрицательном балансе в одну копейку")
    void Should_CalculateDebt_When_BalanceIsOneCentNegative() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("0.01"));

      assertThat(debtCard.getDebt()).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("Округляет проценты по HALF_EVEN вниз")
    void Should_ApplyRoundingDown_When_InterestCalculationHasHalfEven() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        new BigDecimal("12.245"),
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      assertThat(debtCard.getDebt()).isEqualByComparingTo("112.24");
    }

    @Test
    @DisplayName("Округляет проценты по HALF_EVEN вверх")
    void Should_ApplyRoundingUp_When_InterestCalculationHasHalfEven() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        new BigDecimal("12.255"),
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      assertThat(debtCard.getDebt()).isEqualByComparingTo("112.26");
    }

    @Test
    @DisplayName("Учитывает валюту без дробной части")
    void Should_RespectCurrencyFractionDigits_When_CurrencyIsJpy() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        JPY,
        new BigDecimal("10"),
        new BigDecimal("2000")
      );

      debtCard.withdrawal(new BigDecimal("1000"));

      BigDecimal debt = debtCard.getDebt();

      assertThat(debt).isEqualByComparingTo("1100");
      assertThat(debt.scale()).isZero();
    }

    @Test
    @DisplayName("Не изменяет баланс при расчете задолженности")
    void Should_PreserveBalance_When_DebtCalculated() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      BigDecimal balanceBefore = debtCard.getBalance();

      debtCard.getDebt();

      assertThat(debtCard.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Уменьшает задолженность после частичного погашения")
    void Should_ReduceDebt_When_PartiallyRepaid() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      assertThat(debtCard.getDebt()).isEqualByComparingTo("120.00");

      debtCard.fund(new BigDecimal("40.00"));

      assertThat(debtCard.getBalance()).isEqualByComparingTo("-60.00");
      assertThat(debtCard.getDebt()).isEqualByComparingTo("72.00");
    }

    @Test
    @DisplayName("Обнуляет задолженность после полного погашения")
    void Should_ClearDebt_When_FullyRepaid() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      BigDecimal fullRepayment = debtCard.getDebt();

      debtCard.fund(fullRepayment);

      assertThat(debtCard.getBalance()).isEqualByComparingTo("20.00");
      assertThat(debtCard.getDebt()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Возвращает воспроизводимый результат при повторных вызовах getDebt()")
    void Should_BeIdempotent_When_GetDebtCalledMultipleTimes() {
      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        CREDIT_LIMIT
      );

      debtCard.withdrawal(new BigDecimal("100.00"));

      BigDecimal firstCall = debtCard.getDebt();
      BigDecimal secondCall = debtCard.getDebt();

      assertThat(firstCall).isEqualByComparingTo(secondCall);
    }

    @Test
    @DisplayName("Рассчитывает задолженность для очень больших сумм")
    void Should_CalculateDebt_When_AmountsAreVeryLarge() {
      BigDecimal hugePrincipal = new BigDecimal("10000000000000000000000000000000000000000000000000000000.00");

      CreditCard debtCard = new CreditCard(
        CARD_NAME,
        USD,
        INTEREST_RATE,
        hugePrincipal
      );

      debtCard.withdrawal(hugePrincipal);

      BigDecimal expectedDebt = hugePrincipal
        .multiply(new BigDecimal("1.20"))
        .setScale(2, RoundingMode.HALF_EVEN);

      assertThat(debtCard.getDebt()).isEqualByComparingTo(expectedDebt);
    }
  }
}