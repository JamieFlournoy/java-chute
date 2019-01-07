package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.truth.Truth.assertThat;
import java.util.Optional;
import org.junit.Test;
import nl.jqno.equalsverifier.EqualsVerifier;

public class OptionalTransformerTest {
  @Test(expected = NullPointerException.class)
  public void constructor_withNulltransformer_shouldThrow() {
    new OptionalTransformer<String, Integer>(null);
  }
  
  @Test
  public void apply_withValidTransformer_shouldWork() {
    OptionalTransformer<String, Integer> transformer =
        new OptionalTransformer<String, Integer>((s)->s.length());
    
    Optional<Integer> result = transformer.apply(Optional.ofNullable("0123456789abcdef"));
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isEqualTo(16);
    
    result = transformer.apply(Optional.empty());
    assertThat(!result.isPresent()).isTrue();
  }

  @Test
  public void equals_shouldWorkCorrectly() {
    EqualsVerifier.forClass(OptionalTransformer.class).verify();
  }
}
