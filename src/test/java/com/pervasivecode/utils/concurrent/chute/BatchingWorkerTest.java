package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.pervasivecode.utils.time.CurrentNanosSource;
import repeat.Repeat;
import repeat.RepeatRule;

public class BatchingWorkerTest {
  // Use NUM_REPEATS=500 for torture testing.
  private static final int NUM_REPEATS = 5;

  private static final ImmutableList<String> ONE_THROUGH_TEN = ImmutableList.of("one", "two",
      "three", "four", "five", "six", "seven", "eight", "nine", "ten");

  @Rule
  public RepeatRule rule = new RepeatRule();

  private CurrentNanosSource nanosSource;
  private BufferingChute<Object> objectInput;
  private BufferingChute<List<Object>> objectOutput;

  @Before
  public void setup() {
    this.nanosSource = () -> System.nanoTime();
    this.objectInput = new BufferingChute<>(100, nanosSource);
    this.objectOutput = new BufferingChute<>(10, nanosSource);
  }

  @Test(expected = NullPointerException.class)
  @Repeat(times = NUM_REPEATS)
  public void constructor_withNullInput_shouldThrow() {
    new BatchingWorker<>(null, objectOutput, 5, true);
  }

  @Test(expected = NullPointerException.class)
  @Repeat(times = NUM_REPEATS)
  public void constructor_withNullOutput_shouldThrow() {
    new BatchingWorker<>(objectInput, null, 5, true);
  }

  @Test(expected = IllegalArgumentException.class)
  @Repeat(times = NUM_REPEATS)
  public void constructor_withBatchSizeOfZero_shouldThrow() {
    new BatchingWorker<>(objectInput, objectOutput, 0, true);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void batchingWorker_withInitiallyClosedInput_shouldImmediatelyCloseOutput()
      throws Exception {
    objectInput.close();

    assertThat(objectOutput.isClosedAndEmpty()).isFalse();

    Runnable transformer = new BatchingWorker<>(objectInput, objectOutput, 5, true);
    transformer.run();
    assertThat(objectInput.isClosedAndEmpty()).isTrue();
    Optional<List<Object>> o = objectOutput.take();
    assertThat(o.isPresent()).isFalse();
    assertThat(objectOutput.isClosedAndEmpty()).isTrue();
  }

  private void testBatchingWorker(int batchSize, int numElements) throws Exception {
    for (int i = 0; i < numElements; i++) {
      objectInput.put(ONE_THROUGH_TEN.get(i));
    }
    objectInput.close();

    Runnable transformer = new BatchingWorker<>(objectInput, objectOutput, batchSize, true);
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
  public void batchingWorker_withThreeElementsAndBatchSizeOfOne_shouldWork() throws Exception {
    testBatchingWorker(1, 3);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void batchingWorker_withThreeElementsAndBatchSizeOfThree_shouldWork() throws Exception {
    testBatchingWorker(3, 3);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void batchingWorker_withFourElementsAndBatchSizeOfThree_shouldWork() throws Exception {
    testBatchingWorker(3, 4);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void batchingWorker_withFiveElementsAndBatchSizeOfThree_shouldWork() throws Exception {
    testBatchingWorker(3, 5);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void batchingWorker_withSixElementsAndBatchSizeOfThree_shouldWork() throws Exception {
    testBatchingWorker(3, 6);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void batchingWorker_withNotCloseOutputWhenDone_shouldNotCloseOutput() throws Exception {
    Runnable worker = Workers.batchingWorker(objectInput, objectOutput, 5, false);
    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<?> transformResult = es.submit(worker);

    objectInput.put("one");
    objectInput.put("two");
    objectInput.put("three");
    objectInput.close();
    transformResult.get(100, MILLISECONDS);

    Optional<List<Object>> output = objectOutput.tryTakeNow();
    assertThat(output.isPresent()).isTrue();
    assertThat(output.get()).containsExactly("one", "two", "three").inOrder();

    assertThat(objectOutput.isClosed()).isFalse();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void batchingWorker_withCloseOutputWhenDone_shouldNotCloseOutputWhenInterrupted()
      throws Exception {
    Runnable worker = Workers.batchingWorker(objectInput, objectOutput, 3, true);

    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<?> transformResult = es.submit(worker);

    // Grab an element from the output Chute so that we can be sure the worker is up & running.
    objectInput.put("thing 1");
    objectInput.put("thing 2");
    objectInput.put("thing 3");
    Optional<List<Object>> result = objectOutput.tryTake(10, MILLISECONDS);
    assertThat(result.isPresent()).isTrue();

    // Now we interrupt the worker, and give it a chance to terminate.
    transformResult.cancel(true);
    result = objectOutput.tryTake(10, MILLISECONDS);
    assertThat(result.isPresent()).isFalse();

    // Close the input Chute. The worker should not process this batch.
    objectInput.put("added too late 1");
    objectInput.put("added too late 2");
    objectInput.put("added too late 3");
    objectInput.close();
    result = objectOutput.tryTake(10, MILLISECONDS);
    assertThat(result.isPresent()).isFalse();

    // The output Chute should *not* be closed (even though we passed closeOutputWhenDone=true),
    // since it was interrupted.
    assertThat(objectOutput.isClosed()).isFalse();
  }
}
