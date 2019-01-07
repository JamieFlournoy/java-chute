package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Iterator;
import java.util.Objects;

/**
 * Iterable over elements taken from a ChuteExit. See {@link Chutes#asIterable(ChuteExit)} for
 * detailed documentation.
 * <p>
 * Use {@link Chutes#asIterable(ChuteExit)} to obtain an instance of this class.
 * 
 * @see Chutes#asIterable(ChuteExit)
 */
final class ChuteIterableAdapter<T> implements Iterable<T> {
  private final ChuteExit<T> source;

  public ChuteIterableAdapter(ChuteExit<T> source) {
    this.source = checkNotNull(source);
  }

  @Override
  public Iterator<T> iterator() {
    return new ChuteIterator<>(source);
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof ChuteIterableAdapter)) {
      return false;
    }
    ChuteIterableAdapter<?> otherAdapter = (ChuteIterableAdapter<?>) other;
    return Objects.equals(otherAdapter.source, this.source);
  }
}
