package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

final class TransformingExit<S1, S2> implements ChuteExit<S2> {
  private final ChuteExit<S1> supplier;
  private final OptionalTransformer<S1, S2> transformer;

  public TransformingExit(ChuteExit<S1> supplier, Function<S1, S2> transformer) {
    this.supplier = checkNotNull(supplier);
    this.transformer = new OptionalTransformer<>(checkNotNull(transformer));
  }

  @Override
  public Optional<S2> tryTake(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
    return transformer.apply(supplier.tryTake(timeout, timeoutUnit));
  }

  @Override
  public Optional<S2> tryTakeNow() {
    return transformer.apply(supplier.tryTakeNow());
  }

  @Override
  public Optional<S2> take() throws InterruptedException {
    return transformer.apply(supplier.take());
  }

  @Override
  public boolean isClosedAndEmpty() {
    return supplier.isClosedAndEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hash(supplier, transformer);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof TransformingExit)) {
      return false;
    }
    TransformingExit<?, ?> otherExit = (TransformingExit<?, ?>) other;
    return Objects.equals(this.supplier, otherExit.supplier)
        && Objects.equals(this.transformer, otherExit.transformer);
  }
}
