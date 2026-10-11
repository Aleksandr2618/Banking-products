package org.bank.domain.exception;

public class UnsupportedFractionDigitsException extends RuntimeException {
  public UnsupportedFractionDigitsException(String message) {
    super(message);
  }
}
