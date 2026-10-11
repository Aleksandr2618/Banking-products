package org.bank.domain.exception;

public class ClosedProductException extends RuntimeException {
  public ClosedProductException(String message) {
    super(message);
  }
}
