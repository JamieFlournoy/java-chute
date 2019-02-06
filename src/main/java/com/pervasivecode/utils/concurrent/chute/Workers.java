package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkNotNull;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import com.pervasivecode.utils.time.TimeSource;

/**
 * Factory methods for executable workers that process elements taken from {@link Chute}s.
 */
public class Workers {

  private Workers() {};

  /**
   * Returns a Runnable that will take all of the elements from the input ChuteExit, group them into
   * batches of no larger than the specified size, and put them in the output ChuteEntrance. Once
   * the input ChuteExit is closed and the batch containing the last element has been put into the
   * output ChuteEntrance, this Runnable will close the output ChuteEntrance and return.
   * <p>
   * Batches will be sent to the output Chute at least once per maxTimeBetweenBatches, or sooner if
   * the elements appear quickly enough that batchSize is reached before that time has elapsed.
   * <p>
   * If no elements have appeared in the input ChuteExit and the maxTimeBetweenBatches elapses, an
   * empty batch will <b>not</b> be sent to the output ChuteEntrance.
   *
   * @param input A ChuteExit from which individual batchable elements are taken.
   * @param output A ChuteEntrance into which batches are sent.
   * @param batchSize The maximum size of a batch before it should be placed into the output Chute.
   * @param closeOutputWhenDone Whether to close the output chute after the last batch has been
   *        sent.
   * @param timeSource A source of time, used to determine whether it has been long enough to send a
   *        partial batch.
   * @param maxTimeBetweenBatches The amount of time to wait for a batch to be completed before just
   *        sending a partial batch into the output ChuteEntrance.
   * @param <I> The type of object that the input chute emits. (The output chute must accept Lists
   *        containing this type.)
   *
   * @return A Runnable worker that will do the specified batching work and optional closing of the
   *         output chute.
   */
  public static <I> Runnable periodicBatchingWorker(ChuteExit<I> input,
      ChuteEntrance<List<I>> output, int batchSize, boolean closeOutputWhenDone,
      TimeSource timeSource, Duration maxTimeBetweenBatches) {
    return new PeriodicBatchingWorker<>(input, output, batchSize, closeOutputWhenDone, timeSource,
        maxTimeBetweenBatches);
  }

  /**
   * Create a Runnable worker that will transform elements from a ChuteExit using a function,
   * putting the resulting elements into a ChuteEntrance, until the ChuteExit is closed (or the
   * Runnable worker is interrupted).
   *
   * @param input The ChuteExit from which elements should be taken.
   * @param output The ChuteEntrance into which the transformed elements should be put.
   * @param converter The function that transforms input elements into output elements.
   * @param closeOutputWhenDone If true, when the input ChuteExit closes and the last transformed
   *        element has been placed into the output ChuteEntrance, the worker will close the output
   *        ChuteEntrance.
   * @param <T> The type of object that the input chute emits, and the input type of the converter.
   * @param <V> The type of object that the converter produces, and the type of the output chute.
   *
   * @return A Runnable worker that will perform the specified transformation and optional closing
   *         of the output ChuteEntrance.
   */
  public static <T, V> Runnable transformingWorker(ChuteExit<T> input, ChuteEntrance<V> output,
      Function<T, V> converter, boolean closeOutputWhenDone) {
    checkNotNull(input);
    checkNotNull(output);
    checkNotNull(converter);
    return () -> {
      try {
        for (T inputElement : Chutes.asIterable(input)) {
          output.put(converter.apply(inputElement));
        }
        // The input might not be closed and empty if we were interrupted during iteration. Only
        // close the output if we kept iterating until the input chute was closed & empty.
        if (closeOutputWhenDone && input.isClosedAndEmpty()) {
          output.close();
        }
      } catch (@SuppressWarnings("unused") InterruptedException e) {
        // Just stop processing and exit.
      }
    };
  }

  /**
   * Returns a Runnable that will take all of the elements from the input ChuteExit, group them into
   * batches of the specified size, and put them in the output ChuteEntrance.
   * <p>
   * The transformer will wait indefinitely for enough input elements to create a batch, unless the
   * input chute closes, in which case it will send the last batch immediately. For a transformer
   * that will periodically flush batches regardless of size, use
   * {@link #periodicBatchingWorker(ChuteExit, ChuteEntrance, int, boolean, TimeSource, Duration)}.
   *
   * @param input The source of elements to be collected into batches.
   * @param output The chute into which batches of elements will be placed.
   * @param maxBatchSize The maximum size of each batch. The last batch (created when the input
   *        chute is closed and empty) may be smaller than this size; all others will be exactly
   *        this size.
   * @param closeOutputWhenDone Whether to close the output chute after the last batch has been
   *        sent.
   * @param <I> The type of object that the input chute emits. (The output chute must accept Lists
   *        containing this type.)
   *
   * @return A Runnable worker that will do the specified batching work and optional closing of the
   *         output chute.
   */
  public static <I> Runnable batchingWorker(ChuteExit<I> input, ChuteEntrance<List<I>> output,
      int maxBatchSize, boolean closeOutputWhenDone) {
    return new BatchingWorker<I>(input, output, maxBatchSize, closeOutputWhenDone);
  }
}
