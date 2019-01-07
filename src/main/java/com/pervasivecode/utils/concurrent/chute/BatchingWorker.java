package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;

class BatchingWorker<E> implements Runnable {
  private ChuteExit<E> input;
  private ChuteEntrance<List<E>> output;
  private final int maxBatchSize;
  private ArrayList<E> builder;
  private boolean closeOutputWhenDone;

  public BatchingWorker(ChuteExit<E> input, ChuteEntrance<List<E>> output, int maxBatchSize,
      boolean closeOutputWhenDone) {
    this.input = checkNotNull(input);
    this.output = checkNotNull(output);
    checkArgument(maxBatchSize > 0, "maxBatchSize must be greater than 0. Got %s", maxBatchSize);
    this.maxBatchSize = maxBatchSize;
    this.builder = new ArrayList<>(maxBatchSize);
    this.closeOutputWhenDone = closeOutputWhenDone;
  }

  @Override
  public void run() {
    try {
      boolean inputClosed = false;
      while (!inputClosed) {
        boolean sendBatch = false;
        Optional<E> taken = input.take();
        if (taken.isPresent()) {
          builder.add(taken.get());
          if (builder.size() >= maxBatchSize) {
            sendBatch = true;
          }
        } else {
          inputClosed = true;
          if (!builder.isEmpty()) {
            sendBatch = true;
          }
        }
        if (sendBatch) {
          ImmutableList<E> batch = ImmutableList.copyOf(builder);
          builder.clear();
          output.put(batch);
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
