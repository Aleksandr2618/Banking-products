package org.bank.domain.api;

import java.math.BigDecimal;

public interface Withdrawable {

  void withdrawal(BigDecimal amount);
}
