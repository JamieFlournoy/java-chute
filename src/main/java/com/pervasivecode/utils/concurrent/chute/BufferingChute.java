package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import com.pervasivecode.utils.time.api.CurrentNanosSource;

/**
 * A {@link Chute} based on a {@link java.util.concurrent.BlockingQueue BlockingQueue}, providing a
 * fixed-size nonzero-capacity buffer that holds elements that have been put into the
 * {@link ChuteEntrance} but not yet taken from the {@link ChuteExit}.
 *
 * @param <E> The type of object that can be sent through the BufferingChute.
 */
public final class BufferingChute<E> implements Chute<E> {
  static final class Datum<L> {
    public final L wrappedElement;

    /**
     * @param elementToWrap should only be null for the eofDatum instance.
     */
    public Datum(L elementToWrap) {
      wrappedElement = elementToWrap;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(wrappedElement);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }
      if (!(other instanceof Datum)) {
        return false;
      }
      Datum<?> otherDatum = (Datum<?>) other;
      return Objects.equals(otherDatum.wrappedElement, this.wrappedElement);
    }
  }

  private final ArrayBlockingQueue<Datum<E>> buffer;
  private final CurrentNanosSource nanosSource;

  private final AtomicBoolean isOpen = new AtomicBoolean(true);
  private final Datum<E> eofDatum;

  // Lock used to guard against adding a new, non-EOF datum when the chute is closed or closing:
  private final Lock putLock = new ReentrantLock();

  // Lock used to guard against accidentally taking the EOF datum out of the buffer when it's the
  // last element.
  private final Lock takeLock = new ReentrantLock();

  public BufferingChute(int bufferSize, CurrentNanosSource nanosSource) {
    checkArgument(bufferSize > 0, "Buffer size must be at least 1.");
    this.buffer = new ArrayBlockingQueue<>(bufferSize);

    this.nanosSource = checkNotNull(nanosSource);

    // TODO find a way to not use an eofDatum.
    E eofValue = null;
    this.eofDatum = new Datum<>(eofValue);
  }

  private boolean isEof(Datum<E> datum) {
    return datum.wrappedElement == null;
  }
  
  //
  // Methods from ChuteEntrance
  //

  @Override
  public void close() throws InterruptedException {
    putLock.lockInterruptibly();
    try {
      this.isOpen.set(false);
      // TODO figure out a way to close() without blocking when the buffer is full, while still
      // unblocking take() callers who are blocked waiting for an element when the chute is closed.
      buffer.put(eofDatum);
    } finally {
      putLock.unlock();
    }
  }

  @Override
  public boolean isClosed() {
    return !this.isOpen.get();
  }

  @Override
  public void put(@Nonnull E element) throws InterruptedException {
    checkNotNull(element, "Null elements are not allowed");
    putLock.lockInterruptibly();
    try {
      if (!isClosed()) {
        buffer.put(new Datum<>(element));
      } else {
        throw new IllegalStateException("Channel is already closed.");
      }
    } finally {
      putLock.unlock();
    }
  }

  //
  // Methods from ChuteExit
  //

  @Override
  public Optional<E> tryTake(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
    if (isClosedAndEmpty()) {
      return Optional.empty();
    }

    if (timeout == 0) {
      return tryTakeNow();
    }

    long currentTimeNanosBeforeTryLock = this.nanosSource.currentTimeNanoPrecision();
    boolean gotTakeLockInTime = this.takeLock.tryLock(timeout, timeoutUnit);
    if (!gotTakeLockInTime) {
      return Optional.empty();
    }
    try {
      long nanosElapsedAcquiringLock =
          this.nanosSource.currentTimeNanoPrecision() - currentTimeNanosBeforeTryLock;
      long timeoutNanos = timeoutUnit.toNanos(timeout);
      long remainingTimeoutNanos = timeoutNanos - nanosElapsedAcquiringLock;

      Datum<E> datum = buffer.poll(remainingTimeoutNanos, TimeUnit.NANOSECONDS);

      if (datum == null) {
        // Timed out waiting for a datum, but the channel was not closed yet.
        return Optional.empty();
      }

      if (isEof(datum)) {
        // Oops; we didn't mean to take that instance. Put it back.
        buffer.put(datum);
        // The channel is closed.
        return Optional.empty();
      }

      return Optional.of(datum.wrappedElement);
    } finally {
      takeLock.unlock();
    }
  }

  private boolean isNullOrEof(Datum<E> datum) {
    return (datum == null || isEof(datum));
  }

  @Override
  public Optional<E> tryTakeNow() {
    if (isClosedAndEmpty()) {
      return Optional.empty();
    }

    boolean gotLock = takeLock.tryLock();
    if (!gotLock) {
      return Optional.empty();
    }

    try {
      // Look at it, but don't take it.
      Datum<E> datum = buffer.peek();
      if (isNullOrEof(datum)) {
        return Optional.empty();
      }
      // Remove the head of the queue, which we already have a reference to in datum.
      buffer.poll();
      return Optional.of(datum.wrappedElement);
    } finally {
      takeLock.unlock();
    }
  }

  @Override
  public Optional<E> take() throws InterruptedException {
    if (isClosedAndEmpty()) {
      return Optional.empty();
    }
    takeLock.lockInterruptibly();
    try {
      final Datum<E> takenDatum = buffer.take(); // will not return null
      if (isEof(takenDatum)) {
        buffer.put(takenDatum);
        return Optional.empty();
      }
      return Optional.of(takenDatum.wrappedElement);
    } finally {
      takeLock.unlock();
    }
  }

  @Override
  public boolean isClosedAndEmpty() {
    if (isClosed()) {
      Datum<E> last = buffer.peek();
      if (isNullOrEof(last)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(buffer, nanosSource, isOpen, eofDatum, putLock, takeLock);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof BufferingChute)) {
      return false;
    }
    BufferingChute<?> otherChute = (BufferingChute<?>) other; 
    return Objects.equals(otherChute.buffer, this.buffer)
        && Objects.equals(otherChute.nanosSource, this.nanosSource)
        && Objects.equals(otherChute.isOpen, this.isOpen)
        && Objects.equals(otherChute.eofDatum, this.eofDatum)
        && Objects.equals(otherChute.putLock, this.putLock)
        && Objects.equals(otherChute.takeLock, this.takeLock);
  }
}
