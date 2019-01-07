package com.pervasivecode.utils.concurrent.chute;

import java.util.ArrayList;
import org.junit.Test;
import com.google.common.truth.Truth;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class ChuteIterableAdapterTest {
  @Test(expected = NullPointerException.class)
  public void iterable_withNullSource_shouldThrow() {
    Chutes.asIterable(null);
  }

  @Test(timeout = 100)
  public void iterable_withValidSource_shouldProduceWorkingIterable() throws Exception {
    BufferingChute<String> chute = new BufferingChute<>(10, () -> System.nanoTime());
    chute.put("something");
    chute.close();
    Iterable<String> iterable = Chutes.asIterable(chute);
    ArrayList<String> output = new ArrayList<>();
    for (String element : iterable) {
      output.add(element);
    }
    Truth.assertThat(output).containsExactly("something");
  }

  @Test
  public void equals_shouldWorkCorrectly() {
    EqualsVerifier.forClass(ChuteIterableAdapter.class).suppress(Warning.NULL_FIELDS).verify();
  }
}
