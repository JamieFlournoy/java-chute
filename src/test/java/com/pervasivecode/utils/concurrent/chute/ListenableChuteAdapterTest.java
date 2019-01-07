package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import nl.jqno.equalsverifier.EqualsVerifier;
import repeat.Repeat;
import repeat.RepeatRule;

public class ListenableChuteAdapterTest {
  private static final int NUM_REPEATS = 50;

  @Rule
  public RepeatRule rule = new RepeatRule();

  @Mock
  Chute<Integer> mockChute;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = NullPointerException.class)
  public void constructor_withNullChute_shouldThrow() {
    Chutes.asListenableChute(null);
  }

  @Test
  public void close_shouldForwardToWrappedChute() throws Exception {
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    listenableChute.close();
    Mockito.verify(mockChute).close();
  }

  @Test
  public void isClosed_shouldForwardToWrappedChute() throws Exception {
    Mockito.when(mockChute.isClosed()).thenReturn(true, false);
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    assertThat(listenableChute.isClosed()).isTrue();
    assertThat(listenableChute.isClosed()).isFalse();
  }

  @Test
  public void put_shouldForwardToWrappedChute() throws Exception {
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    Integer someInt = Integer.valueOf(131415);
    listenableChute.put(someInt);
    Mockito.verify(mockChute).put(someInt);
  }

  @Test
  public void tryTake_shouldForwardToWrappedChute() throws Exception {
    Optional<Integer> maybeI = Optional.of(Integer.valueOf(76543));
    Mockito.when(mockChute.tryTake(1, SECONDS)).thenReturn(maybeI);

    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    Optional<Integer> result = listenableChute.tryTake(1, SECONDS);

    assertThat(result).isSameAs(maybeI);
  }

  @Test
  public void tryTakeNow_shouldForwardToWrappedChute() throws Exception {
    Optional<Integer> maybeI = Optional.of(Integer.valueOf(65432));
    Mockito.when(mockChute.tryTakeNow()).thenReturn(maybeI);

    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    Optional<Integer> result = listenableChute.tryTakeNow();

    assertThat(result).isSameAs(maybeI);
  }

  @Test
  public void take_shouldForwardToWrappedChute() throws Exception {
    Optional<Integer> maybeI = Optional.of(Integer.valueOf(54321));
    Mockito.when(mockChute.take()).thenReturn(maybeI);

    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    Optional<Integer> result = listenableChute.take();

    assertThat(result).isSameAs(maybeI);
  }

  @Test
  public void isClosedAndEmpty_shouldForwardToWrappedChute() {
    Mockito.when(mockChute.isClosedAndEmpty()).thenReturn(true, false, true);

    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    assertThat(listenableChute.isClosedAndEmpty()).isTrue();
    assertThat(listenableChute.isClosedAndEmpty()).isFalse();
    assertThat(listenableChute.isClosedAndEmpty()).isTrue();
  }

  @Test(expected = NullPointerException.class)
  public void addListener_withNullListener_shouldThrow() {
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    Executor executor = Mockito.mock(Executor.class);
    listenableChute.addListener(null, executor);
  }

  @Test(expected = NullPointerException.class)
  public void addListener_withNullExecutor_shouldThrow() {
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    Runnable listener = () -> {
    };
    listenableChute.addListener(listener, null);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void addListener_withValidArgs_shouldSucceed() {
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(mockChute);
    Runnable listener = () -> {
    };
    Executor executor = Mockito.mock(Executor.class);
    listenableChute.addListener(listener, executor);
  }

  private BufferingChute<Integer> makeRealChute() {
    return new BufferingChute<>(3, () -> System.nanoTime());
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void listener_whenChuteBecomesClosedButNotEmpty_shouldNotBeRun() throws Exception {
    Chute<Integer> chute = makeRealChute();
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(chute);
    listenableChute.put(Integer.valueOf(23456));

    Runnable listener = () -> {
    };
    Executor executor = Mockito.mock(Executor.class);
    listenableChute.addListener(listener, executor);

    listenableChute.close();
    Mockito.verify(executor, Mockito.never()).execute(listener);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void listener_whenChuteBecomesClosedAndEmpty_shouldBeRun() throws Exception {
    Chute<Integer> chute = makeRealChute();
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(chute);
    Runnable listener = () -> {
    };
    Executor executor = Mockito.mock(Executor.class);
    listenableChute.addListener(listener, executor);

    listenableChute.close();
    Mockito.verify(executor, Mockito.times(1)).execute(listener);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void listener_whenChuteBecomesEmptyButNotClosed_shouldNotBeRun() throws Exception {
    Chute<Integer> chute = makeRealChute();
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(chute);
    Integer intVal = Integer.valueOf(345);
    listenableChute.put(intVal);

    Runnable listener = () -> {
    };
    Executor executor = Mockito.mock(Executor.class);
    listenableChute.addListener(listener, executor);

    Optional<Integer> taken = listenableChute.tryTakeNow();
    assertThat(taken.get()).isSameAs(intVal);
    Mockito.verify(executor, Mockito.times(0)).execute(listener);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void listener_whenChuteBecomesNotEmpty_shouldBeRun() throws Exception {
    Chute<Integer> chute = makeRealChute();
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(chute);

    Runnable listener = () -> {
    };
    Executor executor = Mockito.mock(Executor.class);
    listenableChute.addListener(listener, executor);

    listenableChute.put(Integer.valueOf(345));
    Mockito.verify(executor, Mockito.times(1)).execute(listener);
  }

  @Test
  @Repeat(times = NUM_REPEATS)
  public void listener_whenOtherListersGetRejectedExecutionException_shouldStillBeRun()
      throws Exception {
    Chute<Integer> chute = makeRealChute();
    ListenableChute<Integer> listenableChute = Chutes.asListenableChute(chute);

    Runnable unfortunateListener = () -> {
      
    };
    Executor rejectingExecutor = Mockito.mock(Executor.class);
    Mockito.doThrow(new RejectedExecutionException("Nope!")).when(rejectingExecutor)
        .execute(unfortunateListener);
    listenableChute.addListener(unfortunateListener, rejectingExecutor);

    Runnable listener = () -> {
    };
    Executor executor = Mockito.mock(Executor.class);
    listenableChute.addListener(listener, executor);

    listenableChute.put(Integer.valueOf(345));
    Mockito.verify(rejectingExecutor, Mockito.times(1)).execute(unfortunateListener);
    Mockito.verify(executor, Mockito.times(1)).execute(listener);
  }

  @Test
  public void equals_shouldWorkCorrectly() {
    EqualsVerifier.forClass(ListenableChuteAdapter.class).verify();
  }
}
