package org.bank.domain.exception;

public class UnknownFractionDigitsException extends RuntimeException {
  public UnknownFractionDigitsException(String message) {
    super(message);
  }
}
