package org.bank.domain.model;

import org.bank.domain.api.*;
import org.bank.domain.exception.UnknownFractionDigitsException;
import org.bank.domain.exception.UnsupportedFractionDigitsException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractBankProductTest {

  private static final Currency USD = Currency.getInstance("USD");
  private static final Currency JPY = Currency.getInstance("JPY");
  private static final Currency CURRENCY_WITH_UNKNOWN_FRACTION_DIGITS = Currency.getInstance("XTS");

  private static final String PRODUCT_NAME = "Продукт";
  private static final String INITIAL_BALANCE = "100.00";
  
  private static class TestProduct extends AbstractBankProduct {
    TestProduct(String name, Currency currency, BigDecimal balance) {
      super(name, currency, balance);
    }

    void fund(BigDecimal amount) {
      applyFund(amount);
    }

    void withdraw(BigDecimal amount) {
      applyWithdrawal(amount);
    }
  }

  private static TestProduct newProduct() {
    return new TestProduct(PRODUCT_NAME, USD, new BigDecimal(INITIAL_BALANCE));
  }

  @Nested
  @DisplayName("Конструктор")
  class ConstructorTests {
    @ParameterizedTest(name = "Начальный баланс {0} принимается")
    @CsvSource({
      "1000, 1000.00",
      "1000.55, 1000.55",
      "0, 0.00",
      "0.01, 0.01",
      "10.5000000000000, 10.50"
    })
    @DisplayName("Создаёт продукт с корректным начальным балансом")
    void Should_InitializeProduct_When_ValidBalanceProvided(String initialBalance, String expectedBalance) {
      BigDecimal balance = new BigDecimal(initialBalance);

      TestProduct product = new TestProduct(PRODUCT_NAME, USD, balance);

      assertThat(product.getName()).isEqualTo(PRODUCT_NAME);
      assertThat(product.getCurrency()).isEqualTo(USD);
      assertThat(product.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    @DisplayName("Отклоняет название, состоящее из пробелов")
    void Should_ThrowException_When_NameIsBlank() {
      String name = "   ";

      assertThatThrownBy(() -> new TestProduct(name, USD, BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Название продукта не может быть пустым");
    }

    @Test
    @DisplayName("Отклоняет null вместо названия")
    void Should_ThrowException_When_NameIsNull() {
      String name = null;

      assertThatThrownBy(() -> new TestProduct(name, USD, BigDecimal.ZERO))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Название продукта обязательно");
    }

    @Test
    @DisplayName("Отклоняет null вместо валюты")
    void Should_ThrowException_When_CurrencyIsNull() {
      Currency currency = null;

      assertThatThrownBy(() -> new TestProduct(PRODUCT_NAME, currency, BigDecimal.ZERO))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Выбор валюты обязателен");
    }

    @Test
    @DisplayName("Отклоняет null вместо начального баланса")
    void Should_ThrowException_When_BalanceIsNull() {
      BigDecimal balance = null;

      assertThatThrownBy(() -> new TestProduct(PRODUCT_NAME, USD, balance))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Начальный баланс обязателен");
    }

    @ParameterizedTest(name = "Начальный баланс {0} отклоняется")
    @MethodSource("negativeBalances")
    @DisplayName("Отклоняет отрицательный начальный баланс")
    void Should_ThrowException_When_BalanceIsNegative(BigDecimal balance) {
      assertThatThrownBy(() -> new TestProduct(PRODUCT_NAME, USD, balance))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Начальный баланс не может быть отрицательным");
    }

    static Stream<BigDecimal> negativeBalances() {
      return Stream.of(
        new BigDecimal("-0.01"),
        new BigDecimal("-10.50"),
        new BigDecimal("-1000000000000.00")
      );
    }

    @Test
    @DisplayName("Отклоняет начальный баланс с лишними ненулевыми дробными знаками")
    void Should_ThrowException_When_BalanceHasExcessFractionDigits() {
      BigDecimal balance = new BigDecimal("11.001");

      assertThatThrownBy(() -> new TestProduct(PRODUCT_NAME, USD, balance))
        .isInstanceOf(UnsupportedFractionDigitsException.class)
        .hasMessage("Неподдерживаемое количество дробных знаков");
    }

    @Test
    @DisplayName("Принимает целую сумму в валюте без дробных единиц")
    void Should_AcceptBalance_When_CurrencyHasNoFractionDigitsAndAmountIsWhole() {
      BigDecimal balance = new BigDecimal("10");
      
      TestProduct product = new TestProduct(PRODUCT_NAME, JPY, balance);

      assertThat(product.getBalance()).isEqualByComparingTo("10");
      assertThat(product.getCurrency()).isEqualTo(JPY);
    }

    @Test
    @DisplayName("Отклоняет дробный баланс для валюты без дробных единиц")
    void Should_ThrowException_When_CurrencyHasNoFractionDigitsAndAmountIsFractional() {
      BigDecimal balance = new BigDecimal("10.5");

      assertThatThrownBy(() -> new TestProduct(PRODUCT_NAME, JPY, balance))
        .isInstanceOf(UnsupportedFractionDigitsException.class)
        .hasMessage("Неподдерживаемое количество дробных знаков");
    }

    @Test
    @DisplayName("Отклоняет валюту с неизвестной точностью")
    void Should_ThrowException_When_CurrencyHasUnknownFractionDigits() {
      Currency currency = CURRENCY_WITH_UNKNOWN_FRACTION_DIGITS;

      assertThat(currency.getDefaultFractionDigits()).isEqualTo(-1);

      assertThatThrownBy(() -> new TestProduct(PRODUCT_NAME, currency, BigDecimal.ZERO))
        .isInstanceOf(UnknownFractionDigitsException.class)
        .hasMessage("Неизвестная точность валюты: " + currency);
    }

    @Test
    @DisplayName("Принимает очень большой начальный баланс без потери точности")
    void Should_PreservePrecision_When_InitialBalanceIsVeryLarge() {
      BigDecimal balance = new BigDecimal("999999999999999999999999999999.99");

      TestProduct product = new TestProduct(PRODUCT_NAME, USD, balance);

      assertThat(product.getBalance()).isEqualByComparingTo("999999999999999999999999999999.99");
    }
  }

  @Nested
  @DisplayName("Геттеры")
  class GetterTests {
    @Test
    @DisplayName("getName возвращает название продукта")
    void Should_ReturnProductName_When_GetNameCalled() {
      TestProduct product = newProduct();

      String result = product.getName();

      assertThat(result).isEqualTo(PRODUCT_NAME);
    }

    @Test
    @DisplayName("getCurrency возвращает валюту продукта")
    void Should_ReturnProductCurrency_When_GetCurrencyCalled() {
      TestProduct product = newProduct();
      
      Currency result = product.getCurrency();

      assertThat(result).isEqualTo(USD);
    }

    @Test
    @DisplayName("getBalance возвращает актуальный баланс")
    void Should_ReturnCurrentBalance_When_GetBalanceCalled() {
      TestProduct product = newProduct();
      
      BigDecimal result = product.getBalance();

      assertThat(result).isEqualByComparingTo("100.00");
    }
  }

  @Nested
  @DisplayName("Пополнение баланса")
  class FundTests {

    private TestProduct product;

    @BeforeEach
    void setUp() {
      product = newProduct();
    }

    @Test
    @DisplayName("Увеличивает баланс на сумму пополнения")
    void Should_IncreaseBalance_When_FundedWithValidAmount() {
      BigDecimal amount = new BigDecimal("25.50");

      product.fund(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("125.50");
    }

    @Test
    @DisplayName("Принимает пополнение на минимальную денежную единицу")
    void Should_IncreaseBalance_When_FundedWithMinimumCurrencyUnit() {
      BigDecimal amount = new BigDecimal("0.01");
      
      product.fund(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("100.01");
    }

    @Test
    @DisplayName("Принимает сумму с завершающими нулями")
    void Should_NormalizeScale_When_FundedWithTrailingZeros() {
      BigDecimal amount = new BigDecimal("11.230000000");

      product.fund(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("111.23");
    }

    @ParameterizedTest(name = "Сумма {0} отклоняется")
    @MethodSource("org.bank.domain.model.AbstractBankProductTest#getInvalidAmounts")
    @DisplayName("Отклоняет null, ноль и отрицательные суммы")
    void Should_ThrowExceptionAndPreserveBalance_When_FundAmountIsInvalid(
      BigDecimal amount,
      Class<? extends Throwable> expectedException,
      String expectedMessage
    ) {
      BigDecimal balanceBefore = product.getBalance();

      assertThatThrownBy(() -> product.fund(amount))
        .isInstanceOf(expectedException)
        .hasMessage(expectedMessage);

      assertThat(product.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет сумму с лишними ненулевыми дробными знаками")
    void Should_ThrowExceptionAndPreserveBalance_When_FundAmountHasExcessFractionDigits() {
      BigDecimal amount = new BigDecimal("10.123");
      BigDecimal balanceBefore = product.getBalance();

      assertThatThrownBy(() -> product.fund(amount))
        .isInstanceOf(UnsupportedFractionDigitsException.class)
        .hasMessage("Неподдерживаемое количество дробных знаков");

      assertThat(product.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Поддерживает пополнение очень большой суммой")
    void Should_SupportFunding_When_AmountIsVeryLarge() {
      BigDecimal amount = new BigDecimal("999999999999999999999999999999.99");

      product.fund(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("1000000000000000000000000000099.99");
    }
  }

  @Nested
  @DisplayName("Списание с баланса")
  class WithdrawalTests {

    private TestProduct product;

    @BeforeEach
    void setUp() {
      product = newProduct();
    }

    @Test
    @DisplayName("Уменьшает баланс на сумму списания")
    void Should_DecreaseBalance_When_WithdrawnWithValidAmount() {
      BigDecimal amount = new BigDecimal("25.50");
      
      product.withdraw(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("74.50");
    }

    @Test
    @DisplayName("Позволяет списать сумму, равную балансу")
    void Should_ZeroBalance_When_WithdrawnExactBalance() {
      BigDecimal amount = new BigDecimal("100.00");

      product.withdraw(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Принимает сумму с завершающими нулями")
    void Should_NormalizeScale_When_WithdrawnWithTrailingZeros() {
      BigDecimal amount = new BigDecimal("11.230000000");

      product.withdraw(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("88.77");
    }

    @ParameterizedTest(name = "Сумма {0} отклоняется")
    @MethodSource("org.bank.domain.model.AbstractBankProductTest#getInvalidAmounts")
    @DisplayName("Отклоняет null, ноль и отрицательные суммы")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawAmountIsInvalid(
      BigDecimal amount,
      Class<? extends Throwable> expectedException,
      String expectedMessage
    ) {
      BigDecimal balanceBefore = product.getBalance();

      assertThatThrownBy(() -> product.withdraw(amount))
        .isInstanceOf(expectedException)
        .hasMessage(expectedMessage);

      assertThat(product.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Отклоняет сумму с лишними ненулевыми дробными знаками")
    void Should_ThrowExceptionAndPreserveBalance_When_WithdrawAmountHasExcessFractionDigits() {
      BigDecimal amount = new BigDecimal("10.123");
      BigDecimal balanceBefore = product.getBalance();

      assertThatThrownBy(() -> product.withdraw(amount))
        .isInstanceOf(UnsupportedFractionDigitsException.class)
        .hasMessage("Неподдерживаемое количество дробных знаков");

      assertThat(product.getBalance()).isEqualByComparingTo(balanceBefore);
    }

    @Test
    @DisplayName("Поддерживает списание суммы, превышающей начальный баланс")
    void Should_AllowNegativeBalance_When_WithdrawnAmountExceedsBalance() {
      BigDecimal amount = new BigDecimal("150.00");
      
      product.withdraw(amount);

      assertThat(product.getBalance()).isEqualByComparingTo("-50.00");
    }
  }

  @Disabled("Требует реализации потокобезопасности")
  @Nested
  @DisplayName("Тесты в многопоточности")
  class ConcurrencyTests {

    @Test
    @DisplayName("Корректно обрабатывает параллельные пополнения")
    void Should_MaintainCorrectBalance_When_FundedConcurrently() throws InterruptedException {
      int threadCount = 10;
      int operationsPerThread = 1_000;
      BigDecimal amount = new BigDecimal("1.00");
      TestProduct product = newProduct();

      CountDownLatch startSignal = new CountDownLatch(1);
      CountDownLatch completionSignal = new CountDownLatch(threadCount);

      Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

      try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
        for (int i = 0; i < threadCount; i++) {
          executor.submit(() -> {
            try {
              startSignal.await();

              for (int j = 0; j < operationsPerThread; j++) {
                product.fund(amount);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } catch (Exception e) {
              exceptions.add(e);
            } finally {
              completionSignal.countDown();
            }
          });
        }
      }

      startSignal.countDown();
      completionSignal.await();

      BigDecimal expectedBalance = new BigDecimal(
        threadCount * operationsPerThread
      ).add(new BigDecimal(INITIAL_BALANCE));

      assertThat(product.getBalance()).isEqualByComparingTo(expectedBalance);

      assertThat(exceptions)
        .as("Во время выполнения параллельных операций не должно быть исключений")
        .isEmpty();
    }
  }

  static Stream<Arguments> getInvalidAmounts() {
    return Stream.of(
      Arguments.of(
        null,
        NullPointerException.class,
        "Сумма операции обязательна"
      ),
      Arguments.of(
        new BigDecimal("0.00"),
        IllegalArgumentException.class,
        "Сумма операции должна быть больше нуля: 0.00"
      ),
      Arguments.of(
        new BigDecimal("-0.01"),
        IllegalArgumentException.class,
        "Сумма операции должна быть больше нуля: -0.01"
      ),
      Arguments.of(
        new BigDecimal("-10.00"),
        IllegalArgumentException.class,
        "Сумма операции должна быть больше нуля: -10.00"
      )
    );
  }
}