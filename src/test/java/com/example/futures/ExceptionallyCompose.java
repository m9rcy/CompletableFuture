package com.example.futures;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ExceptionallyCompose {
  public static <T> CompletableFuture<T> exceptionallyCompose(
          CompletableFuture<T> input,
          Function<Throwable, CompletableFuture<T>> fun) {
    CompletableFuture<CompletableFuture<T>> wrapped = input.thenApply(CompletableFuture::completedFuture);
    CompletableFuture<CompletableFuture<T>> wrappedWithException = wrapped.exceptionally(fun);
      return wrappedWithException.thenCompose(future -> future);
  }
}
