package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import nl.jqno.equalsverifier.EqualsVerifier;
import repeat.Repeat;
import repeat.RepeatRule;

public class TransformingExitTest {
  private static final int NUM_REPEATS = 50;

  @Rule
  public RepeatRule rule = new RepeatRule();

  private BufferingChute<String> testChute;

  @Before
  public void setup() {
    testChute = new BufferingChute<>(10, () -> System.nanoTime());
  }

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
    testChute.put("12345");
    testChute.put("23456");
    testChute.put("34567");
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

  @Test
  public void equals_shouldWorkCorrectly() {
    EqualsVerifier.forClass(TransformingExit.class).verify();
  }
}
