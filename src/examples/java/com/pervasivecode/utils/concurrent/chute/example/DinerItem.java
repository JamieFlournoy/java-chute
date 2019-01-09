package com.pervasivecode.utils.concurrent.chute.example;

import java.util.concurrent.TimeUnit;
import com.google.auto.value.AutoValue;
import com.pervasivecode.utils.concurrent.chute.example.DinerExample.DinerItemType;

@AutoValue
public abstract class DinerItem {
  public static DinerItem of(DinerItemType type, String name) {
    return new AutoValue_DinerItem(type, name);
  }

  public abstract DinerItemType type();

  public abstract String name();

  public long cookingTime(TimeUnit unit) {
    // "coffee" takes 6ms; "iced tea" takes 8ms.
    return unit.convert(name().length(), TimeUnit.MILLISECONDS);
  }

  @Override
  public final String toString() {
    return name();
  }
}
