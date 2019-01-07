package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.pervasivecode.utils.time.api.CurrentNanosSource;
import com.pervasivecode.utils.time.api.TimeSource;
import repeat.Repeat;
import repeat.RepeatRule;

public class PeriodicBatchingWorkerTest {
  // Use NUM_REPEATS=500 for torture testing.
  private static final int NUM_REPEATS = 5;

  private static final ImmutableList<String> ONE_THROUGH_TEN = ImmutableList.of("one", "two",
      "three", "four", "five", "six", "seven", "eight", "nine", "ten");

  private static final Duration MAX_TIME_BETWEEN_BATCHES = Duration.ofMillis(50L);

  @Rule
  public RepeatRule rule = new RepeatRule();

  private CurrentNanosSource nanosSource;
  private BufferingChute<Object> objectInput;
  private BufferingChute<List<Object>> objectOutput;
  private TimeSource timeSource;
  private Duration maxTimeBetweenBatches;
  
  @Before
  public void setup() {
    this.nanosSource = () -> System.nanoTime();

    this.objectInput = new BufferingChute<>(100, nanosSource);
    this.objectOutput = new BufferingChute<>(10, nanosSource);

    this.timeSource = () -> Instant.now();
    this.maxTimeBetweenBatches = MAX_TIME_BETWEEN_BATCHES;
  }
 
  @Test(expected = NullPointerException.class)
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withNullInput_shouldThrow() {
    objectInput = null;
    Workers.periodicBatchingWorker(objectInput, objectOutput, 5, true, timeSource,
        maxTimeBetweenBatches);
  }

  @Test(expected = NullPointerException.class)
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withNullOutput_shouldThrow() {
    objectOutput = null;
    Workers.periodicBatchingWorker(objectInput, objectOutput, 5, true, timeSource,
        maxTimeBetweenBatches);
  }

  @Test(expected = IllegalArgumentException.class)
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withBatchSizeOfZero_shouldThrow() {
    Workers.periodicBatchingWorker(objectInput, objectOutput, 0, true, timeSource,
        maxTimeBetweenBatches);
  }

  @Test(expected = NullPointerException.class)
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withNullTimeSource_shouldThrow() {
    timeSource = null;
    Workers.periodicBatchingWorker(objectInput, objectOutput, 5, true, timeSource,
        maxTimeBetweenBatches);
  }

  @Test(expected = NullPointerException.class)
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withNullDuration_shouldThrow() {
    maxTimeBetweenBatches = null;
    Workers.periodicBatchingWorker(objectInput, objectOutput, 5, true, timeSource,
        maxTimeBetweenBatches);
  }

  @Test(expected = IllegalArgumentException.class)
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withDurationOfZero_shouldThrow() {
    maxTimeBetweenBatches = Duration.ofMillis(0);
    Workers.periodicBatchingWorker(objectInput, objectOutput, 5, true, timeSource,
        maxTimeBetweenBatches);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withInitiallyClosedInput_shouldImmediatelyCloseOutput()
      throws Exception {
    objectInput.close();

    assertThat(!objectOutput.isClosedAndEmpty()).isTrue();

    Runnable batcher = Workers.periodicBatchingWorker(objectInput, objectOutput, 5, true,
        timeSource, maxTimeBetweenBatches);
    batcher.run();
    assertThat(objectInput.isClosedAndEmpty()).isTrue();
    assertThat(objectOutput.isClosedAndEmpty()).isTrue();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withBatchSizeOfOne_shouldWork() throws Exception {
    objectInput.put("one");
    objectInput.put("two");
    objectInput.put("three");
    objectInput.close();

    Runnable batcher = Workers.periodicBatchingWorker(objectInput, objectOutput, 1, true,
        timeSource, maxTimeBetweenBatches);
    batcher.run();
    assertThat(objectInput.isClosedAndEmpty()).isTrue();

    Optional<List<Object>> o = objectOutput.tryTakeNow();
    assertThat(o.isPresent()).isTrue();
    assertThat(o.get()).containsExactly("one");

    o = objectOutput.tryTakeNow();
    assertThat(o.isPresent()).isTrue();
    assertThat(o.get()).containsExactly("two");

    o = objectOutput.tryTakeNow();
    assertThat(o.isPresent()).isTrue();
    assertThat(o.get()).containsExactly("three");

    assertThat(objectOutput.isClosedAndEmpty()).isTrue();
  }

  private void testPeriodicBatchingWorkerWithFastInput(int batchSize, int numElements)
      throws Exception {
    for (int i = 0; i < numElements; i++) {
      objectInput.put(ONE_THROUGH_TEN.get(i));
    }
    objectInput.close();

    Runnable transformer = Workers.periodicBatchingWorker(objectInput, objectOutput, batchSize,
        true, timeSource, maxTimeBetweenBatches);
    transformer.run();
    assertThat(objectInput.isClosedAndEmpty()).isTrue();

    transformer.run();

    Optional<List<Object>> possibleBatch;
    int remainingElements = numElements;
    int iterations = 0;
    while (remainingElements > 0 && iterations < numElements) {
      int startIndex = iterations * batchSize;
      int endIndex = Math.min(startIndex + batchSize, numElements);
      List<String> expectedBatch = ONE_THROUGH_TEN.subList(startIndex, endIndex);

      possibleBatch = objectOutput.tryTakeNow();
      assertThat(possibleBatch.isPresent()).isTrue();
      assertThat(possibleBatch.get()).containsExactlyElementsIn(expectedBatch).inOrder();

      iterations++;
      remainingElements -= batchSize;
    }

    assertThat(objectOutput.isClosedAndEmpty()).isTrue();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withThreeElementsAndBatchSizeOfOneAndRapidInput_shouldWork()
      throws Exception {
    testPeriodicBatchingWorkerWithFastInput(1, 3);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withThreeElementsAndBatchSizeOfThreeAndRapidInput_shouldWork()
      throws Exception {
    testPeriodicBatchingWorkerWithFastInput(3, 3);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withFourElementsAndBatchSizeOfThreeAndRapidInput_shouldWork()
      throws Exception {
    testPeriodicBatchingWorkerWithFastInput(3, 4);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withFiveElementsAndBatchSizeOfThreeAndRapidInput_shouldWork()
      throws Exception {
    testPeriodicBatchingWorkerWithFastInput(3, 5);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withSixElementsAndBatchSizeOfThreeAndRapidInput_shouldWork()
      throws Exception {
    testPeriodicBatchingWorkerWithFastInput(3, 6);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withTenElementsAndBatchSizeOfFiveAndRapidInput_shouldWork()
      throws Exception {
    testPeriodicBatchingWorkerWithFastInput(5, 10);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withCloseOutputWhenDone_shouldNotCloseOutputWhenInterrupted()
      throws Exception {
    Runnable worker = Workers.periodicBatchingWorker(objectInput, objectOutput, 3, true, timeSource,
        maxTimeBetweenBatches);

    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<?> transformResult = es.submit(worker);

    // Grab an element from the output Chute so that we can be sure the worker is up & running.
    objectInput.put("something");
    long longerThanBatchInterval = maxTimeBetweenBatches.toMillis() * 2;
    Optional<List<Object>> result =
        objectOutput.tryTake(longerThanBatchInterval, MILLISECONDS);
    assertThat(result.isPresent()).isTrue();

    // Now we interrupt the worker, and give it a chance to terminate.
    transformResult.cancel(true);
    result = objectOutput.tryTake(10, MILLISECONDS);
    assertThat(result.isPresent()).isFalse();

    // Close the input Chute. The worker should not process this element.
    objectInput.put("added too late");
    objectInput.close();
    result = objectOutput.tryTake(longerThanBatchInterval, MILLISECONDS);
    assertThat(result.isPresent()).isFalse();

    // The output Chute should *not* be closed (even though we passed closeOutputWhenDone=true),
    // since it was interrupted.
    assertThat(objectOutput.isClosed()).isFalse();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withNotCloseOutputWhenDone_shouldNotCloseOutput()
      throws Exception {
    Runnable worker = Workers.periodicBatchingWorker(objectInput, objectOutput, 5, false,
        timeSource, maxTimeBetweenBatches);
    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<?> transformResult = es.submit(worker);

    objectInput.put("one");
    objectInput.put("two");
    objectInput.close();
    transformResult.get(100, MILLISECONDS);

    Optional<List<Object>> output = objectOutput.tryTakeNow();
    assertThat(output.isPresent()).isTrue();
    assertThat(output.get()).containsExactly("one", "two").inOrder();

    assertThat(objectOutput.isClosed()).isFalse();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void periodicBatchingWorker_withBatchSizeOfThreeAndSlowInput_shouldWork()
      throws Exception {
    Duration maxTimeBetweenBatches = Duration.ofMillis(10);
    Runnable batcher = Workers.periodicBatchingWorker(objectInput, objectOutput, 3, true,
        timeSource, maxTimeBetweenBatches);

    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<?> transformResult = es.submit(batcher);

    objectInput.put("one");
    objectInput.put("two");

    // Block until the first batch is available.
    Optional<List<Object>> o = objectOutput.take();
    assertThat(o.isPresent()).isTrue();
    assertThat(o.get()).containsExactly("one", "two").inOrder();

    // Make the worker wait for longer than its maxTimeBetweenBatches, to ensure that it won't send
    // a batch that contains 0 elements.
    objectOutput.tryTake(maxTimeBetweenBatches.toMillis() * 2, MILLISECONDS);

    objectInput.put("three");
    o = objectOutput.take();
    assertThat(o.isPresent()).isTrue();
    assertThat(o.get()).containsExactly("three");

    objectInput.put("four");
    objectInput.close();
    o = objectOutput.take();
    assertThat(o.isPresent()).isTrue();
    assertThat(o.get()).containsExactly("four");

    transformResult.get(); // let the transformer finish transformin'

    assertThat(objectInput.isClosedAndEmpty()).isTrue();
    assertThat(objectOutput.isClosedAndEmpty()).isTrue();

    es.shutdownNow();
  }

}
