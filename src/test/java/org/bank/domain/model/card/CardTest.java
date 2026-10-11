package org.bank.domain.model.card;

import org.bank.domain.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final Currency JPY = Currency.getInstance("JPY");
  private static final String CARD_NAME = "Тестовая карта";
  private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");

  private TestCard card;

  @BeforeEach
  void setUp() {
    card = new TestCard(CARD_NAME, USD, INITIAL_BALANCE);
  }

  @Nested
  @DisplayName("Пополнение счета")
  class FundTests {

    @Test
    @DisplayName("Увеличивает баланс на сумму пополнения")
    void Should_IncreaseBalance_When_FundedWithValidAmount() {
      BigDecimal amount = new BigDecimal("25.50");

      card.fund(amount);

      assertThat(card.getBalance()).isEqualByComparingTo("125.50");
    }

    @Test
    @DisplayName("Отклоняет нулевую сумму и сохраняет баланс")
    void Should_ThrowExceptionAndPreserveBalance_When_FundAmountIsZero() {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.fund(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет null в качестве суммы пополнения")
    void Should_ThrowExceptionAndPreserveBalance_When_FundAmountIsNull() {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.fund(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Сумма операции обязательна");

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @ParameterizedTest(name = "Пополнение на сумму {0} отклоняется")
    @ValueSource(strings = {"0", "0.00", "-10.00"})
    @DisplayName("Отклоняет нулевую и отрицательную суммы пополнения")
    void Should_ThrowExceptionAndPreserveBalance_When_FundAmountIsNotPositive(String amount) {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.fund(new BigDecimal(amount)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Сумма операции должна быть больше нуля: " + amount);

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет пополнение с избыточной точностью")
    void Should_ThrowExceptionAndPreserveBalance_When_FundAmountHasUnsupportedScale() {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.fund(new BigDecimal("10.001")))
        .isInstanceOf(UnsupportedFractionDigitsException.class)
        .hasMessage("Неподдерживаемое количество дробных знаков");

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Позволяет пополнить счёт после отклонённой операции")
    void Should_AllowFunding_When_RejectedOperation() {
      assertThatThrownBy(() -> card.fund(BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Сумма операции должна быть больше нуля: 0");

      card.fund(new BigDecimal("25.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("125.00");
    }
  }

  @Nested
  @DisplayName("Снятие средств")
  class WithdrawalTests {

    @Test
    @DisplayName("Уменьшает баланс после успешного снятия")
    void Should_DecreaseBalance_When_WithdrawalSucceeds() {
      BigDecimal amount = new BigDecimal("30.00");

      card.withdrawal(amount);

      assertThat(card.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Корректно выполняет последовательные списания")
    void Should_UpdateBalance_When_MultipleWithdrawals() {
      card.withdrawal(new BigDecimal("30.00"));
      card.withdrawal(new BigDecimal("20.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Отклоняет сумму с дробной частью для JPY")
    void Should_ThrowException_When_JpyAmountHasFractionDigits() {
      DebitCard jpyCard = new DebitCard(CARD_NAME, JPY, new BigDecimal("100"));

      assertThatThrownBy(() -> jpyCard.withdrawal(new BigDecimal("10.50")))
        .isInstanceOf(UnsupportedFractionDigitsException.class)
        .hasMessage("Неподдерживаемое количество дробных знаков");

      assertThat(jpyCard.getBalance()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Не изменяет баланс, если валидация карты запрещает снятие")
    void Should_PreserveBalance_When_WithdrawalValidationFails() {
      card.setWithdrawalAllowed(false);
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.withdrawal(new BigDecimal("30.00")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Снятие запрещено");

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @ParameterizedTest(name = "Снятие суммы {0} отклоняется")
    @ValueSource(strings = {"0", "0.00", "-10.00"})
    @DisplayName("Отклоняет нулевую, отрицательную суммы списания и сохраняет баланс")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalAmountIsNotPositive(String amount) {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.withdrawal(new BigDecimal(amount)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Сумма операции должна быть больше нуля: " + amount);

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет null в качестве суммы списания")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalAmountIsNull() {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.withdrawal(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Сумма операции обязательна");

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @ParameterizedTest(name = "Снятие {0} оставляет баланс {1}")
    @CsvSource({
      "100.00, 0.00",
      "99.99, 0.01"
    })
    @DisplayName("Корректно обрабатывает граничные суммы списания")
    void Should_LeaveExpectedBalance_When_WithdrawnBoundaryAmount(String amount, String expectedBalance) {
      card.withdrawal(new BigDecimal(amount));

      assertThat(card.getBalance()).isEqualByComparingTo(expectedBalance);
    }
  }

  private static class TestCard extends Card {

    private boolean withdrawalAllowed = true;

    TestCard(String name, Currency currency, BigDecimal balance) {
      super(name, currency, balance);
    }

    @Override
    protected void validateWithdrawal(BigDecimal amount) {
      if (!withdrawalAllowed) {
        throw new IllegalArgumentException("Снятие запрещено");
      }
    }

    void setWithdrawalAllowed(boolean withdrawalAllowed) {
      this.withdrawalAllowed = withdrawalAllowed;
    }
  }
}