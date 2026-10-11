package org.bank.domain.api;

public interface Closable {

  void close();

  boolean isClosed();
}