package com.pervasivecode.utils.concurrent.chute;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

final class ListenableChuteAdapter<T> implements ListenableChute<T> {
  private final Chute<T> chute;
  private final ArrayList<Runnable> listenersOnExecutors;

  public ListenableChuteAdapter(Chute<T> chute) {
    this.chute = Objects.requireNonNull(chute);
    this.listenersOnExecutors = new ArrayList<>();
  }

  @Override
  public void close() throws InterruptedException {
    chute.close();
    if (chute.isClosedAndEmpty()) {
      notifyListeners();
    }
  }

  @Override
  public boolean isClosed() {
    return chute.isClosed();
  }

  @Override
  public void put(T element) throws InterruptedException {
    chute.put(element);
    notifyListeners();
  }

  @Override
  public Optional<T> tryTake(long timeout, TimeUnit timeoutUnit) throws InterruptedException {
    return chute.tryTake(timeout, timeoutUnit);
  }

  @Override
  public Optional<T> tryTakeNow() {
    return chute.tryTakeNow();
  }

  @Override
  public Optional<T> take() throws InterruptedException {
    return chute.take();
  }

  @Override
  public boolean isClosedAndEmpty() {
    return chute.isClosedAndEmpty();
  }

  @Override
  public void addListener(Runnable listener, Executor executor) {
    Objects.requireNonNull(listener);
    Objects.requireNonNull(executor);
    this.listenersOnExecutors.add(() -> executor.execute(listener));
  }

  private void notifyListeners() {
    for (Runnable listener : listenersOnExecutors) {
      try {
        listener.run();
      } catch (@SuppressWarnings("unused") RejectedExecutionException ree) {
        // That listener couldn't be run, but keep notifying the other listeners.
      }
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(chute, listenersOnExecutors);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof ListenableChuteAdapter)) {
      return false;
    }
    ListenableChuteAdapter<?> otherAdapter = (ListenableChuteAdapter<?>) other;
    return Objects.equals(otherAdapter.chute, this.chute)
        && Objects.equals(otherAdapter.listenersOnExecutors, this.listenersOnExecutors);
  }
}
