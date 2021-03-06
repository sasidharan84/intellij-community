package com.intellij.util.concurrency;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;
import java.util.concurrent.Semaphore;

public class FutureResult<T> implements Future<T> {
  private final Semaphore mySema = new Semaphore(0);
  private volatile Pair<Object, Boolean> myValue;

  public synchronized void set(@Nullable T result) {
    assertNotSet();

    myValue = Pair.create((Object)result, true);
    mySema.release();
  }

  public synchronized void setException(@NotNull Throwable e) {
    assertNotSet();

    myValue = Pair.create((Object)e, false);
    mySema.release();
  }

  public synchronized void reset() {
    try {
      // wait till readers get their results
      if (isDone()) mySema.acquire();
    }
    catch (InterruptedException ignore) {
      return;
    }
    myValue = null;
  }

  private void assertNotSet() {
    if (isDone()) throw new IllegalStateException("Result is already set");
  }

  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  public boolean isCancelled() {
    return false;
  }

  public boolean isDone() {
    return myValue != null;
  }

  public T get() throws InterruptedException, ExecutionException {
    mySema.acquire();
    try {
      return doGet();
    }
    finally {
      mySema.release();
    }
  }

  public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    if (!mySema.tryAcquire(timeout, unit)) throw new TimeoutException();
    try {
      return doGet();
    }
    finally {
      mySema.release();
    }
  }

  @Nullable
  public T tryGet() throws ExecutionException {
    return doGet();
  }

  @Nullable
  private T doGet() throws ExecutionException {
    Pair<Object, Boolean> pair = myValue;
    if (pair == null) return null;

    if (!pair.second) throw new ExecutionException(((Throwable)pair.first).getMessage(), (Throwable)pair.first);
    //noinspection unchecked
    return (T)pair.first;
  }
}
