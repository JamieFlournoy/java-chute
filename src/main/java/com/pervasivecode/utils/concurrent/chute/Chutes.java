package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Factory methods for representing {@link ChuteEntrance}s and {@link ChuteExit}s in useful ways.
 */
public class Chutes {
  private Chutes() {}

  private static class OptionalTransformer<I, O> implements Function<Optional<I>, Optional<O>> {
    private final Function<I, O> transformer;

    public OptionalTransformer(Function<I, O> transformer) {
      this.transformer = checkNotNull(transformer);
    }

    @Override
    public Optional<O> apply(Optional<I> maybeInputElement) {
      return maybeInputElement.isPresent() ? Optional.of(transformer.apply(maybeInputElement.get()))
          : Optional.empty();
    }
  }


  private static class TransformingExit<S1, S2> implements ChuteExit<S2> {
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
  }


  private static class TransformingEntrance<S1, S2> implements ChuteEntrance<S1> {
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
  }


  private static class ChuteIterator<T> implements Iterator<T> {
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
  }


  private static class ChuteIterableAdapter<T> implements Iterable<T> {
    private final ChuteExit<T> source;

    public ChuteIterableAdapter(ChuteExit<T> source) {
      this.source = checkNotNull(source);
    }

    @Override
    public Iterator<T> iterator() {
      return new ChuteIterator<>(source);
    }
  }

  /**
   * Wrap the specified {@link ChuteExit} with an {@link Iterable}, which will {@link ChuteExit#take
   * take} elements from the {@link ChuteExit} until it is closed and empty.
   * <p>
   * Elements put into the Chute after the Iterable is created will become available via the
   * Iterable. Elements taken from the ChuteExit separately (by other threads, or by this thread but
   * not using the Iterable) will not be available via the Iterable.
   * <p>
   * Individual Iterator instances produced by this Iterable can only be used by a single thread per
   * Iterator, but multiple Iterators can safely be used to consume elements from the ChuteExit as
   * long as each Iterator is used by a single thread. Each Iterator will see a mutually-exclusive
   * set of elements taken from the ChuteExit; they are not all iterating over the same series of
   * elements.
   *
   * @param source The source of elements for the Iterable.
   * @param <T> The type of object that the ChuteExit and Iterator emit.
   * @return An Iterable that will produce Iterators that present elements taken from the ChuteExit.
   */
  public static <T> Iterable<T> asIterable(ChuteExit<T> source) {
    return new ChuteIterableAdapter<>(source);
  }

  /**
   * Wrap a given ChuteEntrance with a ChuteEntrance that applies a specified function to each input
   * element and puts the resulting objects into the wrapped ChuteEntrance.
   *
   * @param receiver The ChuteEntrance that will receive the results of applying the function.
   * @param transformer A function that will be applied to every element that is put into the
   *        ChuteEntrance that this method returns.
   * @param <T> The type of object that the function accepts, which will also be the type that the
   *        returned ChuteEntrance accepts.
   * @param <V> The type of object that the function produces, which must also be the type that the
   *        receiver ChuteEntrance accepts.
   * @return A ChuteEntrance that will apply the specified function and put the resulting objects
   *         into the receiver ChuteEntrance.
   */
  public static <T, V> ChuteEntrance<T> transformingEntrance(ChuteEntrance<V> receiver,
      Function<T, V> transformer) {
    return new TransformingEntrance<T, V>(receiver, transformer);
  }

  /**
   * Wrap a given ChuteExit with a ChuteExit that applies a specific function to each element of the
   * supplier ChuteExit.
   *
   * @param supplier The ChuteExit from which the objects are taken, before the function is applied.
   * @param transformer The function that will be applied to each object taken from the supplier
   *        ChuteExit.
   * @param <T> The type of object that the function accepts, which must also be the type that the
   *        supplier ChuteExit provides.
   * @param <V> The type of object that the function produces, which will also be the type that the
   *        returned ChuteExit provides.
   * @return A ChuteExit that applies the specified function to objects taken from the supplier
   *         ChuteExit.
   */
  public static <T, V> ChuteExit<V> transformingExit(ChuteExit<T> supplier,
      Function<T, V> transformer) {
    return new TransformingExit<T, V>(supplier, transformer);
  }
}
