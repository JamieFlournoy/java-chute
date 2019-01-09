package com.pervasivecode.utils.concurrent.chute.example;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class CookedOrder {
  public static CookedOrder of(OrderTicket order) {
    return new AutoValue_CookedOrder(order.item(), order.customerNumber());
  }

  public abstract DinerItem item();

  public abstract int customerNumber();
}
