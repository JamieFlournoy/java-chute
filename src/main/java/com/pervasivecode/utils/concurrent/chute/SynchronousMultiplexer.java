package com.pervasivecode.utils.concurrent.chute;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.ImmutableList;

/**
 * A SynchronousMultiplexer provides multiple {@link ChuteEntrance} instances which all feed into a
 * single {@link ChuteEntrance}, all with the same element type. When all of the provided
 * {@link ChuteEntrance}s are closed, the output {@link ChuteEntrance} will be closed.
 *
 * @param <E> The type of object that can be sent through the SynchronousMultiplexer.
 */
public final class SynchronousMultiplexer<E> {
  private final AtomicInteger numInputChutesStillOpen;
  private final ImmutableList<ChuteEntrance<E>> inputChutes;
  private final ChuteEntrance<E> outputChute;

  /**
   * Create a multiplexer with the specified number of inputs, which places elements into the
   * specified ChuteEntrance.
   *
   * @param numInputs The number of separately-closeable ChuteEntrances that this instance will
   *        provide.
   * @param outputChute The ChuteEntrance into which the combined elements from all of the entrances
   *        presented by this class will be placed.
   */
  public SynchronousMultiplexer(int numInputs, ChuteEntrance<E> outputChute) {
    // numInputs=1 is redundant (just use the sole outputChute directly), but will work, so allow
    // callers to do that.
    checkArgument(numInputs > 0, "numInputs must be at least 1.");

    this.numInputChutesStillOpen = new AtomicInteger(numInputs);
    this.outputChute = checkNotNull(outputChute);

    ImmutableList.Builder<ChuteEntrance<E>> inputChutesBuilder = ImmutableList.builder();
    for (int i = 0; i < numInputs; i++) {
      inputChutesBuilder.add(new MultiplexingEntrance());
    }
    this.inputChutes = inputChutesBuilder.build();
  }

  /**
   * Get all of the input ChuteEntrances provided by this instance. When all of these ChuteEntrances
   * have been closed, the output chute will be closed.
   *
   * @return A list of the input ChuteEntrances provided by this instance.
   */
  public ImmutableList<ChuteEntrance<E>> inputChutes() {
    return inputChutes;
  }

  final class MultiplexingEntrance implements ChuteEntrance<E> {
    private boolean isClosed = false;

    @Override
    public boolean isClosed() {
      return isClosed || outputChute.isClosed();
    }

    @Override
    public void close() throws InterruptedException {
      if (this.isClosed()) {
        return;
      }
      this.isClosed = true;
      int numLeftOpen = numInputChutesStillOpen.decrementAndGet();
      if (numLeftOpen <= 0) {
        outputChute.close();
      }
    }

    @Override
    public void put(E element) throws InterruptedException {
      if (this.isClosed) {
        throw new IllegalStateException("This ChuteEntrance was already closed.");
      }
      outputChute.put(element);
    }

    private SynchronousMultiplexer<?> enclosingInstance() {
      return SynchronousMultiplexer.this;
    }

    @Override
    public int hashCode() {
      return Objects.hash(SynchronousMultiplexer.this, isClosed);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }
      if (!(other instanceof SynchronousMultiplexer.MultiplexingEntrance)) {
        return false;
      }
      SynchronousMultiplexer<?>.MultiplexingEntrance otherEntrance =
          (SynchronousMultiplexer<?>.MultiplexingEntrance) other;
      return Objects.equals(otherEntrance.isClosed, this.isClosed)
          && Objects.equals(otherEntrance.enclosingInstance(), SynchronousMultiplexer.this);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(numInputChutesStillOpen, inputChutes, outputChute);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof SynchronousMultiplexer)) {
      return false;
    }
    SynchronousMultiplexer<?> otherMux = (SynchronousMultiplexer<?>) other;
    return Objects.equals(otherMux.numInputChutesStillOpen, this.numInputChutesStillOpen)
        && Objects.equals(otherMux.inputChutes, this.inputChutes)
        && Objects.equals(otherMux.outputChute, this.outputChute);
  }
}
