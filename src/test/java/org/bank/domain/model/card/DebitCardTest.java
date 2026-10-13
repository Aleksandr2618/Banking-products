package org.bank.domain.model.card;

import org.bank.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebitCardTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final String CARD_NAME = "Дебетовая карта";
  private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100.00");

  private DebitCard card;

  @BeforeEach
  void setUp() {
    card = new DebitCard(CARD_NAME, USD, INITIAL_BALANCE);
  }

  @Nested
  @DisplayName("Конструктор")
  class ConstructorTests {

    @Test
    @DisplayName("Создаёт карту с корректными параметрами")
    void Should_InitializeCard_When_ValidParametersProvided() {
      assertThat(card.getName()).isEqualTo(CARD_NAME);
      assertThat(card.getCurrency()).isEqualTo(USD);
      assertThat(card.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
    }

    @Test
    @DisplayName("Устанавливает нулевой баланс, если он не передан")
    void Should_InitializeZeroBalance_When_BalanceNotProvided() {
      DebitCard zeroCard = new DebitCard(CARD_NAME, USD);

      assertThat(zeroCard.getName()).isEqualTo(CARD_NAME);
      assertThat(zeroCard.getCurrency()).isEqualTo(USD);
      assertThat(zeroCard.getBalance()).isEqualByComparingTo("0.00");
    }
  }

  @Nested
  @DisplayName("Снятие средств")
  class WithdrawalTests {

    @Test
    @DisplayName("Уменьшает баланс при снятии доступной суммы")
    void Should_DecreaseBalance_When_WithdrawnWithinAvailableBalance() {
      card.withdrawal(new BigDecimal("30.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("70.00");
    }

    @ParameterizedTest(name = "Снятие {0} отклоняется")
    @CsvSource({
      "100.01, 'Недостаточно средств: баланс 100.00, сумма списания 100.01'",
      "150.00, 'Недостаточно средств: баланс 100.00, сумма списания 150.00'"
    })
    @DisplayName("Отклоняет снятие суммы, превышающей баланс")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalExceedsBalance(String amount, String expectedMessage) {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.withdrawal(new BigDecimal(amount)))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessage(expectedMessage);

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Позволяет снять средства после отклонённой операции")
    void Should_AllowWithdrawal_When_RejectedOperation() {
      assertThatThrownBy(() -> card.withdrawal(new BigDecimal("150.00")))
        .isInstanceOf(InsufficientFundsException.class)
        .hasMessage("Недостаточно средств: баланс 100.00, сумма списания 150.00");

      card.withdrawal(new BigDecimal("40.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("60.00");
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
  }

  @Nested
  @DisplayName("Пополнение счёта")
  class FundTests {

    @Test
    @DisplayName("Увеличивает баланс при пополнении")
    void Should_IncreaseBalance_When_FundedWithValidAmount() {
      card.fund(new BigDecimal("50.00"));

      assertThat(card.getBalance()).isEqualByComparingTo("150.00");
    }
  }
}