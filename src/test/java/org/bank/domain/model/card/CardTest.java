package org.bank.domain.model.card;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardTest {

  private static final Currency USD = Currency.getInstance("USD");
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
    @DisplayName("Не изменяет баланс, если валидация карты запрещает снятие")
    void Should_PreserveBalance_When_WithdrawalValidationFails() {
      card.setWithdrawalAllowed(false);
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.withdrawal(new BigDecimal("30.00")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Снятие запрещено");

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет нулевую сумму снятия и сохраняет баланс")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawalAmountIsZero() {
      BigDecimal balanceBefore = card.getBalance();

      assertThatThrownBy(() -> card.withdrawal(BigDecimal.ZERO)).isInstanceOf(IllegalArgumentException.class);

      assertThat(card.getBalance()).isEqualByComparingTo(balanceBefore);
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