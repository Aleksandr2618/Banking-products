package org.bank.domain.api;

import java.math.BigDecimal;

public interface Fundable {

  void fund(BigDecimal amount);
}