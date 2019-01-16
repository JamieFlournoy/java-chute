package com.pervasivecode.utils.concurrent.chute;

/**
 * A ListenableChute is a {@link Chute} that has a {@link ListenableChuteExit} rather than just a
 * regular {@link ChuteExit}.
 *
 * @param <T> The type of object that can be sent through the chute.
 * @see ListenableChuteExit#addListener(Runnable, java.util.concurrent.Executor)
 */
public interface ListenableChute<T> extends Chute<T>, ListenableChuteExit<T> {
  // This interface is just the combination of ChuteEntrance and ListenableChuteExit.
}
