/*-
 * -\-\-
 * completable-futures
 * --
 * Copyright (C) 2016 - 2023 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package futures;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static futures.CompletableFutures.getException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.*;

public class ConcurrencyReducerTest {

  @Test
  public void testTooLowConcurrency() {
    assertThrows(IllegalArgumentException.class, () -> ConcurrencyReducer.create(0, 10));
  }

  @Test
  public void testTooLowQueueSize() {
    assertThrows(IllegalArgumentException.class, () -> ConcurrencyReducer.create(10, 0));
  }

  @Test
  public void testNullJob() {
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(1, 10);
    assertThrows(NullPointerException.class, () -> limiter.add(null));
  }

  @Test()
  public void testVoidJob() {
    final ConcurrencyReducer<Void> limiter = ConcurrencyReducer.create(1, 10);
    final CompletionStage<Void> task = CompletableFuture.completedFuture(null);
    assertTrue(task.toCompletableFuture().isDone());

    final CompletableFuture<Void> stage = limiter.add(() -> task);
    assertTrue(stage.isDone());
  }

  @Test
  public void testJobReturnsNull() {
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(1, 10);
    final CompletableFuture<String> response = limiter.add(job(null));
    assertTrue(response.isDone());
    final Throwable exception = getException(response);
    assertThat(exception, instanceOf(NullPointerException.class));
  }

  @Test
  public void testJobThrows() {
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(1, 10);
    final CompletableFuture<String> response =
        limiter.add(
            () -> {
              throw new IllegalStateException();
            });

    assertTrue(response.isDone());
    final Throwable exception = getException(response);
    assertThat(exception, instanceOf(IllegalStateException.class));
  }

  @Test
  public void testJobReturnsFailure() {
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(1, 10);
    final CompletionStage<String> response =
        limiter.add(
            job(CompletableFutures.exceptionallyCompletedFuture(new IllegalStateException())));

    assertTrue(response.toCompletableFuture().isDone());
    final Throwable exception = getException(response);
    assertThat(exception, instanceOf(IllegalStateException.class));
  }

  @Test
  public void testCancellation() {
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(2, 10);
    final CompletableFuture<String> request1 = new CompletableFuture<>();
    final CompletableFuture<String> request2 = new CompletableFuture<>();

    final CompletableFuture<String> response1 = limiter.add(job(request1));
    final CompletableFuture<String> response2 = limiter.add(job(request2));

    final AtomicBoolean wasInvoked = new AtomicBoolean();
    final CompletableFuture<String> response3 =
        limiter.add(
            () -> {
              wasInvoked.set(true);
              return null;
            });

    response3.toCompletableFuture().cancel(false);

    // 1 and 2 are in progress, 3 is cancelled

    assertFalse(response1.isDone());
    assertFalse(response2.isDone());
    assertTrue(response3.isDone());
    assertEquals(2, limiter.numActive());
    assertEquals(1, limiter.numQueued());

    request2.complete("2");

    assertFalse(response1.isDone());
    assertTrue(response2.isDone());
    assertTrue(response3.isDone());
    assertEquals(1, limiter.numActive());
    assertEquals(0, limiter.numQueued());

    request1.complete("1");

    assertTrue(response1.isDone());
    assertTrue(response2.isDone());
    assertTrue(response3.isDone());
    assertEquals(0, limiter.numActive());
    assertEquals(0, limiter.numQueued());

    assertFalse(wasInvoked.get());
  }

  @Test
  public void testSimple() {
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(2, 10);
    final CompletableFuture<String> request1 = new CompletableFuture<>();
    final CompletableFuture<String> request2 = new CompletableFuture<>();
    final CompletableFuture<String> request3 = new CompletableFuture<>();
    final CompletableFuture<String> response1 = limiter.add(job(request1));
    final CompletableFuture<String> response2 = limiter.add(job(request2));
    final CompletableFuture<String> response3 = limiter.add(job(request3));

    request3.complete("3");

    // 1 and 2 are in progress, 3 is still blocked

    assertFalse(response1.isDone());
    assertFalse(response2.isDone());
    assertFalse(response3.isDone());
    assertEquals(2, limiter.numActive());
    assertEquals(1, limiter.numQueued());

    request2.complete("2");

    assertFalse(response1.isDone());
    assertTrue(response2.isDone());
    assertTrue(response3.isDone());
    assertEquals(1, limiter.numActive());
    assertEquals(0, limiter.numQueued());

    request1.complete("1");

    assertTrue(response1.isDone());
    assertTrue(response2.isDone());
    assertTrue(response3.isDone());
    assertEquals(0, limiter.numActive());
    assertEquals(0, limiter.numQueued());
  }

  @Test
  public void testLongRunning() {
    final AtomicInteger activeCount = new AtomicInteger();
    final AtomicInteger maxCount = new AtomicInteger();
    final int queueSize = 11;
    final int maxConcurrency = 10;
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(maxConcurrency, queueSize);
    List<CountingJob> jobs = new ArrayList<>();
    List<CompletableFuture<String>> responses = new ArrayList<>();
    for (int i = 0; i < queueSize; i++) {
      final CountingJob job = new CountingJob(limiter::numActive, maxCount);
      jobs.add(job);
      responses.add(limiter.add(job));
    }

    for (int i = 0; i < jobs.size(); i++) {
      final CountingJob job = jobs.get(i);
      if (i % 2 == 0) {
        job.future.complete("success");
      } else {
        job.future.completeExceptionally(new IllegalStateException());
      }
    }
    responses.forEach(response -> assertTrue(response.isDone()));
    assertEquals(0, activeCount.get());
    assertEquals(0, limiter.numActive());
    assertEquals(0, limiter.numQueued());
    assertEquals(maxConcurrency, limiter.remainingActiveCapacity());
    assertEquals(maxConcurrency, maxCount.get());
    assertEquals(queueSize, limiter.remainingQueueCapacity());
  }

  @Test
  public void testQueueSize() {
    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(10, 10);
    for (int i = 0; i < 20; i++) {
      limiter.add(job(new CompletableFuture<>()));
    }

    final CompletableFuture<String> future = limiter.add(job(new CompletableFuture<>()));
    assertTrue(future.isDone());
    final Throwable e = getException(future);
    assertThat(e, instanceOf(ConcurrencyReducer.CapacityReachedException.class));
  }

  @Test
  public void testQueueSizeCounter() {
    final CompletableFuture<String> future = new CompletableFuture<>();

    final ConcurrencyReducer<String> limiter = ConcurrencyReducer.create(10, 10);
    for (int i = 0; i < 20; i++) {
      limiter.add(job(future));
    }

    assertEquals(10, limiter.numActive());
    assertEquals(10, limiter.numQueued());
    assertEquals(0, limiter.remainingActiveCapacity());
    assertEquals(0, limiter.remainingQueueCapacity());

    future.complete("");

    assertEquals(0, limiter.numActive());
    assertEquals(0, limiter.numQueued());
    assertEquals(10, limiter.remainingActiveCapacity());
    assertEquals(10, limiter.remainingQueueCapacity());
  }

  private Callable<CompletionStage<String>> job(final CompletionStage<String> future) {
    return () -> future;
  }

  private static class CountingJob implements Callable<CompletionStage<String>> {

    private final Supplier<Integer> activeCount;
    private final AtomicInteger maxCount;

    final CompletableFuture<String> future = new CompletableFuture<>();

    public CountingJob(Supplier<Integer> activeCount, AtomicInteger maxCount) {
      this.activeCount = activeCount;
      this.maxCount = maxCount;
    }

    @Override
    public CompletionStage<String> call() {
      if (activeCount.get() > maxCount.get()) {
        maxCount.set(activeCount.get());
      }
      return future;
    }
  }
}
