package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.pervasivecode.utils.time.CurrentNanosSource;
import repeat.Repeat;
import repeat.RepeatRule;

public class WorkersTest {
  // Use NUM_REPEATS=500 for torture testing.
  private static final int NUM_REPEATS = 5;

  @Rule
  public RepeatRule rule = new RepeatRule();

  private CurrentNanosSource nanosSource;
  private BufferingChute<String> stringInput;
  private BufferingChute<String> stringOutput;

  @Before
  public void setup() {
    this.nanosSource = () -> System.nanoTime();
    this.stringInput = new BufferingChute<>(10, nanosSource);
    this.stringOutput = new BufferingChute<>(10, nanosSource);
  }

  private static void putAll(ChuteEntrance<String> entrance, String... s)
      throws InterruptedException {
    for (int i = 0; i < s.length; i++) {
      entrance.put(s[i]);
    }
  }

  // --------------------------------------------------------------------------
  //
  // Tests for transformingWorker
  //
  // --------------------------------------------------------------------------

  @Test(expected = NullPointerException.class)
  public void transformingWorker_withNullInput_shouldThrow() {
    Workers.transformingWorker((ChuteExit<String>) null, stringOutput, String::trim, false);
  }

  @Test(expected = NullPointerException.class)
  public void transformingWorker_withNullOutput_shouldThrow() {
    Workers.transformingWorker(stringInput, (ChuteEntrance<String>) null, String::trim, false);
  }

  @Test(expected = NullPointerException.class)
  public void transformingWorker_withNullConverter_shouldThrow() {
    Workers.transformingWorker(stringInput, stringOutput, null, false);
  }

  @Test
  public void transformingWorker_withValidArgsAndCloseWhenDone_shouldProduceWorkingTransformer()
      throws Exception {
    BufferingChute<Integer> converted = new BufferingChute<>(5, () -> System.nanoTime());
    Runnable worker = Workers.transformingWorker(stringInput, converted, Integer::valueOf, true);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> futureTransformResult = executor.submit(worker);

    putAll(stringInput, "345678", "456789", "567890");

    Optional<Integer> taken = converted.tryTake(10, MILLISECONDS);
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(345678);
    assertThat(converted.isClosedAndEmpty()).isFalse();

    taken = converted.tryTake(10, MILLISECONDS);
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(456789);
    assertThat(converted.isClosedAndEmpty()).isFalse();

    taken = converted.tryTake(10, MILLISECONDS);
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(567890);
    assertThat(converted.isClosedAndEmpty()).isFalse();

    stringInput.close();
    futureTransformResult.get(10, MILLISECONDS);
    assertThat(converted.isClosedAndEmpty()).isTrue();

    executor.shutdownNow();
    executor.awaitTermination(1, SECONDS);
  }

  @Test
  public void transformingWorker_withValidArgsAndNotCloseWhenDone_shouldProduceWorkingTransformer()
      throws Exception {
    BufferingChute<Integer> converted = new BufferingChute<>(5, () -> System.nanoTime());
    Runnable worker = Workers.transformingWorker(stringInput, converted, Integer::valueOf, false);

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> futureTransformResult = executor.submit(worker);

    putAll(stringInput, "4567890", "5678901", "6789012");

    Optional<Integer> taken = converted.tryTake(10, MILLISECONDS);
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(4567890);
    assertThat(converted.isClosedAndEmpty()).isFalse();

    taken = converted.tryTake(10, MILLISECONDS);
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(5678901);
    assertThat(converted.isClosedAndEmpty()).isFalse();

    taken = converted.tryTake(10, MILLISECONDS);
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(6789012);
    assertThat(converted.isClosedAndEmpty()).isFalse();

    stringInput.close();
    // Closing the input chute should not cause the worker to close the output chute.
    futureTransformResult.get(10, MILLISECONDS);
    assertThat(converted.isClosedAndEmpty()).isFalse();

    // There should not be any results hanging around in the converted chute, so closing should just
    // result in the worker closing the converted chute and then returning.
    converted.close();
    futureTransformResult.get(100, MILLISECONDS);
    assertThat(converted.isClosedAndEmpty()).isTrue();

    executor.shutdownNow();
    executor.awaitTermination(1, SECONDS);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void transformingWorker_withCloseOutputWhenDone_shouldNotCloseOutputWhenInterrupted()
      throws Exception {
    stringOutput = new BufferingChute<>(2, () -> System.nanoTime());
    Runnable worker =
        Workers.transformingWorker(stringInput, stringOutput, (s) -> s.toUpperCase(), true);

    ExecutorService es = Executors.newFixedThreadPool(1);
    Future<?> transformResult = es.submit(worker);

    // Grab an element from the output Chute so that we can be sure the worker is up & running.
    stringInput.put("whatever");
    Optional<String> result = stringOutput.tryTake(1, SECONDS);
    assertThat(result.isPresent()).isTrue();

    // Make sure the worker is blocked waiting to put an element in the output chute.
    stringInput.put("thing 1");
    stringInput.put("thing 2");
    stringInput.put("thing 3"); // should cause the worker to block, since stringOutput has size 2

    // Now we interrupt the worker, and give it a chance to terminate.
    transformResult.cancel(true);

    // Close the input Chute.
    stringInput.close();

    // Make sure the worker has stopped running.
    Runnable secondTask = () -> {
    };
    Future<?> futureSecondTaskResult = es.submit(secondTask);
    futureSecondTaskResult.get(1, SECONDS);

    // The output Chute should *not* be closed (even though we passed closeOutputWhenDone=true),
    // since it was interrupted.
    assertThat(stringOutput.isClosed()).isFalse();
  }
}
