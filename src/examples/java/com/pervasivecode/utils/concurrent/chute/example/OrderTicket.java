package com.pervasivecode.utils.concurrent.chute.example;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class OrderTicket {
  public static OrderTicket of(int customerNumber, DinerItem item) {
    return new AutoValue_OrderTicket(customerNumber, item);
  }

  public abstract int customerNumber();

  public abstract DinerItem item();
}
