package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
import com.google.common.truth.Truth;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import repeat.Repeat;
import repeat.RepeatRule;

public class ChuteIteratorTest {
  private static final int NUM_REPEATS = 50;

  @Rule
  public RepeatRule rule = new RepeatRule();

  private BufferingChute<String> testChute;

  @Before
  public void setup() {
    testChute = new BufferingChute<>(10, () -> System.nanoTime());
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void next_withEmptyChute_shouldThrowException() throws Exception {
    Iterator<String> iterator = new ChuteIterator<>(testChute);
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
  public void iterator_withChuteContainingZeroElements_shouldWork() throws Exception {
    Iterator<String> iterator = new ChuteIterator<>(testChute);
    testChute.close();
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void iterator_withChuteContainingOneElement_shouldWork() throws Exception {
    String theOneElement = "Last element!";

    Iterator<String> iterator = new ChuteIterator<>(testChute);
    testChute.put(theOneElement);
    testChute.close();

    assertThat(iterator.hasNext()).isTrue();
    String taken = iterator.next();
    assertThat(taken).isSameAs(theOneElement);

    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void iterator_whenInterrupted_shouldStopIterating() throws Exception {
    testChute.put("first element");
    testChute.put("second element");

    Iterable<String> iterable = new ChuteIterableAdapter<>(testChute);

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

  @Test
  public void equals_shouldWorkCorrectly() {
    EqualsVerifier.forClass(ChuteIterator.class).suppress(Warning.NONFINAL_FIELDS)
        .suppress(Warning.NULL_FIELDS).verify();
  }
}
