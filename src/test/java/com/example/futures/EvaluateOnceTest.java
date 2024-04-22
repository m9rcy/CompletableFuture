package com.example.futures;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import futures.CompletableFutures;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluateOnceTest {
  @Test
  public void evaluateOnlyOnce() {
    AtomicInteger counter = new AtomicInteger(0);
    CompletableFuture<Object> start = new CompletableFuture<>();
    CompletableFuture<Integer> middle = start.thenApply(s -> counter.incrementAndGet());
    CompletableFuture<Integer> child1 = middle.thenApply(s -> counter.incrementAndGet());
    CompletableFuture<Integer> child2 = middle.thenApply(s -> counter.incrementAndGet());

    start.complete(0);
    middle.join();
    child1.join();
    child2.join();
    assertEquals(3, counter.get());
  }
}
