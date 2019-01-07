package com.pervasivecode.utils.concurrent.chute;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Factory methods for representing {@link Chutes}, {@link ChuteEntrance}s, and {@link ChuteExit}s
 * in useful ways.
 */
public class Chutes {
  private Chutes() {}

  /**
   * Wrap the specified {@link ChuteExit} with an {@link Iterable}, which will {@link ChuteExit#take
   * take} elements from the {@link ChuteExit} until it is closed and empty.
   * <p>
   * Properties of this Iterable:
   * <ul>
   * <li>Thread safety: Individual Iterator instances produced by this Iterable can only be used by
   * a single thread each, but multiple Iterators can safely be used to consume elements from the
   * same ChuteExit. (It does not matter whether the Iterators were obtained from the same Iterable
   * or not.)
   * <p>
   * Each Iterator will see a mutually-exclusive set of elements taken from the ChuteExit; they are
   * not all iterating over the same series of elements.
   * <li>Visibility of elements: Elements put into the Chute after the Iterable is created will be
   * available via the Iterable. Elements taken from the ChuteExit separately (by other threads, or
   * by this thread but not using the Iterable) will not be available via the Iterable.
   * <li>Buffering: Iterator instances buffer one element from the {@code ChuteExit} in order to
   * correctly implement the {@link Iterator#hasNext() hasNext()} method. If
   * {@link Iterator#hasNext() hasNext()} returns {@code true}, then an element has been taken from
   * the {@code ChuteExit} and buffered in the Iterator, and the caller must use
   * {@link Iterator#next() next()} to access it. Since the element is buffered in the Iterator, no
   * other Iterators will see that element, so it is important to use an Iterator in the usual way,
   * always calling {@code next()} if {@code hasNext()} returns true.
   * <p>
   * Using the returned Iterable in a foreach-loop satisfies this requirement. Using the typical
   * Iterator while-loop construct also satisfies this requirement:
   * 
   * <pre>
   * while (iterator.hasNext()) {
   *   E element = iterator.next();
   *   // ...
   * }
   * </pre>
   * </ul>
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

  /**
   * Wrap a given Chute with a {@link ListenableChute} adapter which allows use of the ChuteExit in
   * a non-blocking fashion, allowing a single thread to process elements from many ChuteExits.
   *
   * @param chute The Chute that should be made into a ListenableChute (by wrapping it in an adapter
   *        class).
   * @param <T> The type of element handled by the provided chute, which will also by the type of
   *        element handled by the returned ListenableChute.
   * @return The ListenableChute made from the provided chute.
   */
  public static <T> ListenableChute<T> asListenableChute(Chute<T> chute) {
    return new ListenableChuteAdapter<T>(chute);
  }
}
