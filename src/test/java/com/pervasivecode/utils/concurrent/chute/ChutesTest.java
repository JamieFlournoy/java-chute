package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
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

  // ChuteIterableAdapterTest comprehensively tests the returned object. This just tests the factory
  // method.
  @Test
  @Repeat(times = NUM_REPEATS)
  public void asIterable_withValidArgs_shouldSucceed() throws Exception {
    ImmutableList<String> elements = ImmutableList.of("a", "b", "c");
    for (String element : elements) {
      testChute.put(element);
    }

    Iterable<String> iterable = Chutes.asIterable(testChute);
    testChute.close();

    ArrayList<String> iterated = new ArrayList<String>();
    for (String s : iterable) {
      iterated.add(s);
    }

    assertThat(iterated).containsExactlyElementsIn(elements).inOrder();
  }

  // TransformingEntranceTest comprehensively tests the returned object. This just tests the factory
  // method.
  @Test
  public void transformingEntrance_withValidArgs_shouldSucceed() throws Exception {
    ChuteEntrance<Double> doubleChute =
        Chutes.transformingEntrance(testChute, (d) -> String.valueOf(d));
    doubleChute.put(0.25d);
    assertThat(testChute.tryTakeNow().get()).isEqualTo("0.25");
  }

  // TransformingExitTest comprehensively tests the returned object. This just tests the factory
  // method.
  @Test
  public void transformingExit_withValidArgs_shouldSucceed() throws Exception {
    testChute.put("hello");
    ChuteExit<String> questionChute = Chutes.transformingExit(testChute, (s) -> s + "?");
    assertThat(questionChute.tryTakeNow().get()).isEqualTo("hello?");
  }

  // ListenableChuteAdapterTest comprehensively tests the returned object. This just tests the
  // factory
  // method.
  @Test(timeout = 100)
  @Repeat(times = NUM_REPEATS)
  public void asListenableChute_withValidArgs_shouldSucceed() throws Exception {
    CountDownLatch numCallsRemaining = new CountDownLatch(2);
    Runnable listener = () -> {
      numCallsRemaining.countDown(); 
    };
    String theElement = "an element";
    
    ListenableChute<String> listenableChute = new ListenableChuteAdapter<>(testChute);
    ExecutorService es = Executors.newSingleThreadExecutor();
    listenableChute.addListener(listener, es);
    
   
    listenableChute.put(theElement); // this should result in a call to the listener
    listenableChute.close();
    Optional<String> taken = listenableChute.tryTakeNow(); // this should also run the listener

    assertThat(taken.get()).isEqualTo(theElement);
    assertThat(listenableChute.isClosedAndEmpty()).isTrue();
    
    numCallsRemaining.await(10, TimeUnit.MILLISECONDS);

    es.shutdownNow();
  }
}
