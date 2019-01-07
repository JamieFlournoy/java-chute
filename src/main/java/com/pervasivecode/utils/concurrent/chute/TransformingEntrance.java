package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.function.Function;

final class TransformingEntrance<S1, S2> implements ChuteEntrance<S1> {
  private final ChuteEntrance<S2> receiver;
  private final Function<S1, S2> transformer;

  public TransformingEntrance(ChuteEntrance<S2> receiver, Function<S1, S2> transformer) {
    this.receiver = checkNotNull(receiver);
    this.transformer = checkNotNull(transformer);
  }

  @Override
  public void close() throws InterruptedException {
    receiver.close();
  }

  @Override
  public boolean isClosed() {
    return receiver.isClosed();
  }

  @Override
  public void put(S1 element) throws InterruptedException {
    receiver.put(transformer.apply(element));
  }

  @Override
  public int hashCode() {
    return Objects.hash(receiver, transformer);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof TransformingEntrance)) {
      return false;
    }
    TransformingEntrance<?, ?> otherEntrance = (TransformingEntrance<?, ?>) other;
    return Objects.equals(otherEntrance.receiver, this.receiver)
        && Objects.equals(otherEntrance.transformer, this.transformer);
  }
}