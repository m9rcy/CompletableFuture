package com.example.futures;


import java.util.concurrent.CompletableFuture;

import futures.CompletableFutures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

public class ObtrudeTest {
  @Test
  public void testObtrudeAfterCallback() {
    CompletableFuture<String> parent = CompletableFuture.completedFuture("first");
    CompletableFuture<String> child = parent.thenApply(v -> v + " second");

    assertEquals("first", CompletableFutures.getCompleted(parent));
    assertEquals("first second", CompletableFutures.getCompleted(child));

    parent.obtrudeValue("not-first");
    assertEquals("not-first", CompletableFutures.getCompleted(parent));
    assertEquals("first second", CompletableFutures.getCompleted(child));
  }

  @Test
  public void testObtrudeBeforeCallback() {
    CompletableFuture<String> parent = new CompletableFuture<>();
    CompletableFuture<String> child = parent.thenApply(v -> v + " second");

    parent.obtrudeValue("first");
    assertEquals("first", CompletableFutures.getCompleted(parent));
    assertEquals("first second", CompletableFutures.getCompleted(child));

    parent.obtrudeValue("not-first");
    CompletableFuture<String> future3 = parent.thenApply(v -> v + " second");

    assertEquals("not-first second", CompletableFutures.getCompleted(future3));
  }

  @Test
  public void testObtrude() {
    CompletableFuture<String> future = new CompletableFuture<>();
    future.complete("first");
    future.obtrudeException(new IllegalArgumentException());
    assertEquals(IllegalArgumentException.class, CompletableFutures.getException(future).getClass());
    future.obtrudeValue("second");
    assertEquals("second", CompletableFutures.getCompleted(future));
  }
}
