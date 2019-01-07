package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final class OptionalTransformer<I, O> implements Function<Optional<I>, Optional<O>> {
  private final Function<I, O> transformer;

  public OptionalTransformer(Function<I, O> transformer) {
    this.transformer = checkNotNull(transformer);
  }

  @Override
  public Optional<O> apply(Optional<I> maybeInputElement) {
    return maybeInputElement.isPresent() ? Optional.of(transformer.apply(maybeInputElement.get()))
        : Optional.empty();
  }

  @Override
  public int hashCode() {
    return Objects.hash(transformer);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof OptionalTransformer)) {
      return false;
    }
    OptionalTransformer<?, ?> otherTransformer = (OptionalTransformer<?, ?>) other;
    return Objects.equals(otherTransformer.transformer, this.transformer);
  }
}
