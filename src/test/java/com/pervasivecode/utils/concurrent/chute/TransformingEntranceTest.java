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
import com.google.common.collect.ImmutableList;
import nl.jqno.equalsverifier.EqualsVerifier;
import repeat.Repeat;
import repeat.RepeatRule;

public class TransformingEntranceTest {
  private static final int NUM_REPEATS = 50;

  @Rule
  public RepeatRule rule = new RepeatRule();

  private BufferingChute<String> testChute;

  @Before
  public void setup() {
    testChute = new BufferingChute<>(10, () -> System.nanoTime());
  }
  
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

    for (String element : ImmutableList.of("a", "b", "c")) {
      entrance.put(element);
    }
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

  @Test
  public void equals_shouldWorkCorrectly() {
    EqualsVerifier.forClass(TransformingEntrance.class).verify();
  }
}
