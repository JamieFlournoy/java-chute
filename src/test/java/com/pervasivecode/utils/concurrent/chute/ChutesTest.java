package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import repeat.Repeat;
import repeat.RepeatRule;

public class ChutesTest {
  private static final int NUM_REPEATS = 50;

  @Rule
  public RepeatRule rule = new RepeatRule();

  private BufferingChute<String> testChute;

  @Before
  public void setup() {
    testChute = new BufferingChute<>(10, () -> System.nanoTime());
  }

  private static ImmutableList<String> putAll(ChuteEntrance<String> entrance, String... s)
      throws InterruptedException {
    for (int i = 0; i < s.length; i++) {
      entrance.put(s[i]);
    }
    return ImmutableList.copyOf(s);
  }

  // --------------------------------------------------------------------------
  //
  // Tests for iterable
  //
  // --------------------------------------------------------------------------

  @Test(expected = NullPointerException.class)
  public void iterable_withNullSource_shouldThrow() {
    Chutes.asIterable(null);
  }

  @Test
  public void iterable_withValidArgs_shouldProduceWorkingIterator() throws Exception {
    ImmutableList<String> elements = putAll(testChute, "a", "b", "c");
    Iterable<String> iterable = Chutes.asIterable(testChute);
    testChute.close();

    ArrayList<String> iterated = new ArrayList<String>();
    for (String s : iterable) {
      iterated.add(s);
    }

    assertThat(iterated).containsExactlyElementsIn(elements);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void iterable_withChuteContainingZeroElements_shouldWork() throws Exception {
    Iterator<String> iterator = Chutes.asIterable(testChute).iterator();
    testChute.close();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void iterable_withChuteContainingOneElement_shouldWork() throws Exception {
    String theOneElement = "Last element!";

    Iterator<String> iterator = Chutes.asIterable(testChute).iterator();
    testChute.put(theOneElement);
    testChute.close();

    assertThat(iterator.hasNext()).isTrue();
    String taken = iterator.next();
    assertThat(taken).isSameAs(theOneElement);

    assertThat(iterator.hasNext()).isFalse();
  }

  // This is part of how an Iterator is supposed to work: when hasNext() returns false, calling
  // next() should result in a NoSuchElementException being thrown.
  @Test
  @Repeat(times = NUM_REPEATS)
  public void chuteIterator_next_withEmptyChute_shouldThrowException() throws Exception {
    Iterator<String> iterator = Chutes.asIterable(testChute).iterator();
    testChute.close();

    try {
      iterator.next();
      Truth.assert_().fail("Expected next() to fail with zero elements left.");
    } catch (@SuppressWarnings("unused") NoSuchElementException nsee) {
      // Expected.
    }
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void iterable_whenInterrupted_shouldStopIterating() throws Exception {
    testChute.put("first element");
    testChute.put("second element");

    Iterable<String> iterable = Chutes.asIterable(testChute);

    ExecutorService es = Executors.newSingleThreadExecutor();

    CountDownLatch stoppedIterating = new CountDownLatch(1);
    BufferingChute<String> outputChute = new BufferingChute<String>(5, () -> System.nanoTime());

    Callable<Void> consumeElementsAndGetCancelled = () -> {
      for (String element : iterable) {
        outputChute.put(element);
      }
      stoppedIterating.countDown();
      return null;
    };

    Future<Void> futureFirstResult = es.submit(consumeElementsAndGetCancelled);

    Optional<String> outputElement = outputChute.tryTake(100, MILLISECONDS);
    assertThat(outputElement.isPresent()).isTrue();
    outputElement = outputChute.tryTake(100, MILLISECONDS);
    assertThat(outputElement.isPresent()).isTrue();

    // Now, the consumeElementsAndGetCancelled worker should be blocked waiting for a third element
    // to appear in testChute (which will not occur).
    outputElement = outputChute.tryTake(10, MILLISECONDS);
    assertThat(outputElement.isPresent()).isFalse();
    try {
      futureFirstResult.get(10, MILLISECONDS);
      Truth.assert_().fail("Expected TimeoutException.");
    } catch (@SuppressWarnings("unused") TimeoutException te) {
      // expected.
    }

    // Cancel the worker. The Iterator should catcn the resulting InterruptedExeption and return
    // hasNext=false, which should result in stoppedIterating being counted down to 0.
    futureFirstResult.cancel(true);
    stoppedIterating.await(100, MILLISECONDS);

    try {
      futureFirstResult.get();
      Truth.assert_().fail("Expected futureFirstResult to throw CancellationException.");
    } catch (CancellationException ce) {
      // The worker should have been cancelled, but the CancellationException should not contain a
      // cause since the consumeElementsAndGetCancelled worker didn't throw an Exception.
      assertThat(ce).hasCauseThat().isNull();
    }

    es.shutdownNow();
  }

  // --------------------------------------------------------------------------
  //
  // Tests for transformingEntrance
  //
  // --------------------------------------------------------------------------

  @Test(expected = NullPointerException.class)
  public void transformingEntrance_withNullReceiver_shouldThrow() {
    Chutes.transformingEntrance(null, (s) -> s + "!");
  }

  @Test(expected = NullPointerException.class)
  public void transformingEntrance_withNullTransformer_shouldThrow() {
    Chutes.transformingEntrance(testChute, null);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void transformingEntrance_whenClosed_shouldCloseReceiver() throws Exception {
    ChuteEntrance<String> entrance = Chutes.transformingEntrance(testChute, (s) -> s.toUpperCase());
    assertThat(testChute.isClosed()).isFalse();
    entrance.close();
    assertThat(testChute.isClosed()).isTrue();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void transformingEntrance_withValidArgs_shouldProduceWorkingChuteEntrance()
      throws Exception {
    ChuteEntrance<String> entrance =
        Chutes.transformingEntrance(testChute, (s) -> s.toUpperCase() + "!");
    assertThat(entrance.isClosed()).isFalse();

    putAll(entrance, "a", "b", "c");
    assertThat(testChute.isClosed()).isFalse();

    testChute.close();
    assertThat(testChute.isClosed()).isTrue();
    assertThat(testChute.isClosedAndEmpty()).isFalse();

    Optional<String> taken = testChute.take();
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo("A!");
    assertThat(testChute.isClosedAndEmpty()).isFalse();

    taken = testChute.take();
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo("B!");
    assertThat(testChute.isClosedAndEmpty()).isFalse();

    taken = testChute.take();
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo("C!");
    assertThat(testChute.isClosedAndEmpty()).isTrue();

    taken = testChute.tryTakeNow();
    assertThat(taken.isPresent()).isFalse();

    taken = testChute.tryTake(1, MILLISECONDS);
    assertThat(taken.isPresent()).isFalse();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<Optional<String>> futureResultOfTake = executor.submit(() -> testChute.take());

    taken = futureResultOfTake.get(10, MILLISECONDS);
    assertThat(taken.isPresent()).isFalse();

    executor.shutdownNow();
    executor.awaitTermination(1, SECONDS);
  }

  // --------------------------------------------------------------------------
  //
  // Tests for TransformingExit
  //
  // --------------------------------------------------------------------------

  @Test(expected = NullPointerException.class)
  public void transformingExit_withNullReceiver_shouldThrow() {
    Chutes.transformingExit(null, (s) -> s + "?");
  }

  @Test(expected = NullPointerException.class)
  public void transformingExit_withNullTransformer_shouldThrow() {
    Chutes.transformingExit(testChute, null);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void transformingExit_withValidArgs_shouldProduceWorkingChuteExit() throws Exception {
    ChuteExit<Integer> exit = Chutes.transformingExit(testChute, Integer::valueOf);

    assertThat(exit.isClosedAndEmpty()).isFalse();
    putAll(testChute, "12345", "23456", "34567");
    assertThat(exit.isClosedAndEmpty()).isFalse();

    Optional<Integer> taken = exit.tryTakeNow();
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(12345);
    assertThat(exit.isClosedAndEmpty()).isFalse();

    taken = exit.tryTake(10, MILLISECONDS);
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(23456);
    assertThat(exit.isClosedAndEmpty()).isFalse();

    taken = exit.take();
    assertThat(taken.isPresent()).isTrue();
    assertThat(taken.get()).isEqualTo(34567);
    assertThat(exit.isClosedAndEmpty()).isFalse();

    taken = exit.tryTakeNow();
    assertThat(taken.isPresent()).isFalse();

    testChute.close();
    assertThat(exit.isClosedAndEmpty()).isTrue();
  }
}
