package com.pervasivecode.utils.concurrent.chute;

import java.util.concurrent.Executor;

/**
 * A {@link ChuteExit} that allows listeners to be called when there is something to take from the
 * {@link ChuteExit}, or when the {@link ChuteExit} is closed and empty.
 * <p>
 * Using a listener avoids the need to have one thread per {@link ChuteExit} blocked waiting for the
 * next element. Instead, a single thread can process elements as they become available from
 * multiple unrelated {@link ChuteExit}s.
 *
 * @param <T> The type of element that the ChuteExit emits.
 */
public interface ListenableChuteExit<T> extends ChuteExit<T> {

  /**
   * Add a listener that will be called when the ChuteExit's externally-visible state changes. This
   * means that one of the following things has happened:
   * <ol>
   * <li>an element has been put into the corresponding ChuteEntrance, or
   * <li>the Chute has become closed and empty.
   * </ol>
   * The listener should therefore call {@link ChuteExit#tryTakeNow()} to try to obtain an element,
   * and/or should call {@link ChuteExit#isClosedAndEmpty()} to determine whether the Chute is
   * closed and empty.
   * <p>
   * (Note: the state change from "has elements to take" to "is empty" will not result in the
   * listener being notified.)
   * 
   * @param listener A handler that will be run when the ChuteExit's state changes
   * @param executor The executor in which the listener will be run
   */
  public void addListener(Runnable listener, Executor executor);
}
