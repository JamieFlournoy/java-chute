package com.pervasivecode.utils.concurrent.chute.example;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import com.google.auto.value.AutoValue;
import com.pervasivecode.utils.time.CurrentNanosSource;

@AutoValue
public abstract class CookingItem implements Delayed {
  protected CookingItem() {}

  private static Instant now(CurrentNanosSource nanoSource) {
    return Instant.ofEpochMilli(nanoSource.currentTimeNanoPrecision() / 1_000_000L);
  }

  public static CookingItem of(OrderTicket order, CurrentNanosSource nanoSource) {
    Instant whenReady = now(nanoSource).plusMillis(order.item().cookingTime(MILLISECONDS));
    return new AutoValue_CookingItem(order, whenReady, nanoSource);
  }

  protected abstract OrderTicket order();

  protected abstract Instant whenReady();

  protected abstract CurrentNanosSource nanoSource();

  public CookedOrder asCookedItem() {
    return CookedOrder.of(order());
  }

  @Override
  public int compareTo(Delayed other) {
    return Long.compare(this.getDelay(NANOSECONDS), other.getDelay(NANOSECONDS));
  }

  @Override
  public long getDelay(TimeUnit unit) {
    Duration timeLeft = Duration.between(now(nanoSource()), whenReady());
    // TimeUnit#convert is awkward: "to this unit, convert a duration of nanoseconds."
    return unit.convert(timeLeft.toNanos(), NANOSECONDS);
  }
}
