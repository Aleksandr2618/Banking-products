package org.bank.domain.model.deposit;

import org.bank.domain.exception.ClosedProductException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepositTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final String DEPOSIT_NAME = "Вечный вклад";
  private static final BigDecimal INITIAL_BALANCE = new BigDecimal("500.00");

  private Deposit deposit;

  @BeforeEach
  void setUp() {
    deposit = new Deposit(DEPOSIT_NAME, USD, INITIAL_BALANCE);
  }

  @Nested
  @DisplayName("Конструктор")
  class ConstructorTests {

    @Test
    @DisplayName("Создает вклад с корректными параметрами")
    void Should_InitializeDeposit_When_ValidParametersAndBalanceProvided() {
      assertThat(deposit.getName()).isEqualTo(DEPOSIT_NAME);
      assertThat(deposit.getCurrency()).isEqualTo(USD);
      assertThat(deposit.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
      assertThat(deposit.isClosed()).isFalse();
    }

    @Test
    @DisplayName("Создает вклад с нулевым балансом, если баланс не передан")
    void Should_InitializeZeroBalance_When_BalanceNotProvided() {
      Deposit zeroDeposit = new Deposit(DEPOSIT_NAME, USD);

      assertThat(zeroDeposit.getName()).isEqualTo(DEPOSIT_NAME);
      assertThat(zeroDeposit.getCurrency()).isEqualTo(USD);
      assertThat(zeroDeposit.getBalance()).isEqualByComparingTo("0.00");
      assertThat(zeroDeposit.isClosed()).isFalse();
    }
  }

  @Nested
  @DisplayName("Пополнение вклада")
  class FundTests {

    @Test
    @DisplayName("Увеличивает баланс активного вклада на сумму пополнения")
    void Should_IncreaseBalance_When_DepositIsActiveAndFunded() {
      BigDecimal amount = new BigDecimal("150.00");

      deposit.fund(amount);

      assertThat(deposit.getBalance()).isEqualByComparingTo("650.00");
    }

    @Test
    @DisplayName("Позволяет пополнить вклад с нулевым балансом")
    void Should_IncreaseBalance_When_ZeroBalanceDepositIsFunded() {
      Deposit zeroDeposit = new Deposit(DEPOSIT_NAME, USD);

      zeroDeposit.fund(new BigDecimal("25.00"));

      assertThat(zeroDeposit.getBalance()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("Позволяет пополнить вклад после отклонённой операции")
    void Should_AllowFunding_When_RejectedOperation() {
      assertThatThrownBy(() -> deposit.fund(BigDecimal.ZERO) )
        .isInstanceOf(IllegalArgumentException.class);

      deposit.fund(new BigDecimal("25.00"));

      assertThat(deposit.getBalance()).isEqualByComparingTo("525.00");
    }
  }

  @Nested
  @DisplayName("Закрытие вклада")
  class CloseTests {

    @Test
    @DisplayName("Успешно закрывает активный вклад")
    void Should_SetClosedStatus_When_DepositIsClosed() {
      assertThat(deposit.isClosed()).isFalse();

      deposit.close();

      assertThat(deposit.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Не изменяет баланс при закрытии вклада")
    void Should_PreserveBalance_When_DepositIsClosed() {
      BigDecimal balanceBefore = deposit.getBalance();

      deposit.close();

      assertThat(deposit.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Закрывает вклад с нулевым балансом")
    void Should_CloseDeposit_When_BalanceIsZero() {
      Deposit zeroDeposit = new Deposit(DEPOSIT_NAME, USD);

      zeroDeposit.close(); assertThat(zeroDeposit.isClosed()).isTrue();

      assertThat(zeroDeposit.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Отклоняет повторное закрытие вклада")
    void Should_RemainClosed_When_CloseCalledMultipleTimes() {
      deposit.close();

      assertThatThrownBy(() -> deposit.close())
        .isInstanceOf(ClosedProductException.class)
        .hasMessage("Вклад 'Вечный вклад' закрыт");
    }

    @Test
    @DisplayName("Отклоняет пополнение и выбрасывает исключение, если вклад закрыт")
    void Should_ThrowExceptionAndPreserveBalance_When_DepositIsClosedAndFundAttempted() {
      deposit.close();
      BigDecimal balanceBefore = deposit.getBalance();
      BigDecimal amount = new BigDecimal("100.00");

      assertThatThrownBy(() -> deposit.fund(amount))
        .isInstanceOf(ClosedProductException.class)
        .hasMessage("Вклад 'Вечный вклад' закрыт");

      assertThat(deposit.getBalance()).isEqualByComparingTo(balanceBefore);
      assertThat(deposit.isClosed()).isTrue();
    }
  }
}