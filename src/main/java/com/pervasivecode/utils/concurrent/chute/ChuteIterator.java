package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

/**
 * Iterator produced by {@link ChuteIterableAdapter}. See {@link Chutes#asIterable(ChuteExit)} for
 * detailed documentation.
 * <p>
 * Use {@link Chutes#asIterable(ChuteExit)} to obtain an instance of {@link ChuteIterableAdapter},
 * and use that Iterable in a foreach loop, which will automatically use this Iterator class.
 * <p>
 * Alternatively, you can obtain an instance of this class from
 * {@link ChuteIterableAdapter#iterator()} method, and use it directly.
 * 
 * @param <T> The type of element emitted by this Iterator.
 *
 * @see Chutes#asIterable(ChuteExit)
 * @see ChuteIterableAdapter#iterator()
 */
final class ChuteIterator<T> implements Iterator<T> {
  private boolean interrupted = false;
  private final ChuteExit<T> source;
  private Optional<T> buffer;

  public ChuteIterator(ChuteExit<T> source) {
    this.source = checkNotNull(source);
    this.buffer = Optional.empty();
  }

  private void maybeFillBuffer() {
    if (buffer.isPresent() || interrupted || source.isClosedAndEmpty()) {
      return;
    }
    try {
      buffer = source.take();
    } catch (@SuppressWarnings("unused") InterruptedException e) {
      // Stop iterating. hasNext will return false and next() will throw from this point on.
      buffer = Optional.empty();
      interrupted = true;
    }
  }

  @Override
  public boolean hasNext() {
    maybeFillBuffer();
    return buffer.isPresent();
  }

  @Override
  public T next() {
    maybeFillBuffer();
    T bufferedElement = buffer.get();
    buffer = Optional.empty();
    return bufferedElement;
  }

  @Override
  public int hashCode() {
    return Objects.hash(interrupted, source, buffer);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof ChuteIterator)) {
      return false;
    }
    ChuteIterator<?> otherIterator = (ChuteIterator<?>) other;
    return Objects.equals(otherIterator.interrupted, this.interrupted)
        && Objects.equals(otherIterator.source, this.source)
        && Objects.equals(otherIterator.buffer, this.buffer);
  }
}
