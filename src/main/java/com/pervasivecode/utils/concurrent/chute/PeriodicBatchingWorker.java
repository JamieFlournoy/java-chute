package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.pervasivecode.utils.time.TimeSource;

class PeriodicBatchingWorker<E> implements Runnable {
  private ChuteExit<E> input;
  private ChuteEntrance<List<E>> output;
  private final int maxBatchSize;
  private ArrayList<E> builder;
  private final TimeSource timeSource;
  private final Duration maxTimeBetweenBatches;
  private Instant whenToFlush;
  private boolean closeOutputWhenDone;

  public PeriodicBatchingWorker(ChuteExit<E> input, ChuteEntrance<List<E>> output, int maxBatchSize,
      boolean closeOutputWhenDone, TimeSource timeSource, Duration maxTimeBetweenBatches) {
    this.input = checkNotNull(input);
    this.output = checkNotNull(output);

    checkArgument(maxBatchSize > 0, "maxBatchSize must be greater than 0. Got %s", maxBatchSize);
    this.maxBatchSize = maxBatchSize;
    this.builder = new ArrayList<>(maxBatchSize);
    this.closeOutputWhenDone = closeOutputWhenDone;

    this.timeSource = checkNotNull(timeSource);
    checkArgument(!maxTimeBetweenBatches.equals(Duration.ZERO),
        "maxTimeBetweenBatches cannot be zero.");
    this.maxTimeBetweenBatches = checkNotNull(maxTimeBetweenBatches);
    this.whenToFlush = timeSource.now().plus(maxTimeBetweenBatches);
  }

  @Override
  public void run() {
    try {
      boolean inputClosed = false;
      while (!inputClosed) {
        boolean sendBatch = false;
        Instant now = timeSource.now();
        long millisToWait =
            whenToFlush.isAfter(now) ? Duration.between(now, whenToFlush).toMillis() : 0;
        Optional<E> taken = input.tryTake(millisToWait, MILLISECONDS);
        if (taken.isPresent()) {
          builder.add(taken.get());
          if (builder.size() >= maxBatchSize) {
            sendBatch = true;
          }
        } else {
          // tryTake didn't return anything. This could mean that the input is closed, or it could
          // mean that we just timed out and should send a partial batch.
          if (input.isClosedAndEmpty()) {
            inputClosed = true;
          }
          if (!builder.isEmpty()) {
            sendBatch = true;
          }
        }
        if (sendBatch) {
          ImmutableList<E> batch = ImmutableList.copyOf(builder);
          builder.clear();
          output.put(batch);
          whenToFlush = timeSource.now().plus(maxTimeBetweenBatches);
        }
      }
      if (closeOutputWhenDone) {
        output.close();
      }
    } catch (@SuppressWarnings("unused") InterruptedException ie) {
      // Just stop processing and exit.
    }
  }
}
