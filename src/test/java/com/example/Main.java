package com.example;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.out;
import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.Collections.nCopies;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.*;

public class Main {

  private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);;

  private static final ExecutorService POOL = newFixedThreadPool(2);

  static String timePassed(long startEpoch) {
    return format("%4s", now().toEpochMilli() - startEpoch);
  }

  static String thread() {
    return currentThread().getName();
  }

  static String tabs(int number) {
    return join("", nCopies(number, "\t\t"));
  }

  static Runnable sleepAndRun(int id, int millis, long startEpoch) {
    return () -> {
      out.println(format("%s ms %s Task #%d [STARTED] in [T%s]", timePassed(startEpoch), tabs(id), id, thread()));

      try {
        Thread.sleep(millis);
      } catch (InterruptedException ignored) {
      }

      out.println(format("%s ms %s Task #%d [STOPPED] in [T%s]", timePassed(startEpoch), tabs(id), id, thread()));
    };
  }

  static Runnable sleepAndThrow(int id, int millis, long startEpoch) {
    return () -> {
      sleepAndRun(id, millis, startEpoch).run();
      throw new RuntimeException();
    };
  }

  static void showcaseCancellation(long startEpoch, boolean cancel){
    CompletableFuture<Void> c1 = runAsync(sleepAndThrow(1, 200, startEpoch), POOL);
    CompletableFuture<Void> c2 = runAsync(sleepAndRun(2, 300, startEpoch), POOL);
    CompletableFuture<Void> c2_1 = c2.thenRun(sleepAndRun(2, 2000, startEpoch));
    CompletableFuture<Void> c3 = runAsync(sleepAndRun(3, 1000, startEpoch), POOL);
    CompletableFuture<Void> c4 = runAsync(sleepAndRun(4, 1000, startEpoch), POOL);

    try {
      CompletableFuture<Void> all = allOf(c1, c2_1);
      c1.exceptionally(throwable -> {
        all.completeExceptionally(throwable);
        if (cancel) {
          c2_1.cancel(true);
        }
        return null;
      });
      all.join();
    } catch (Exception ignored) {
    }


//
//    try {
//      CompletableFuture<Void> all2 = allOf(c2_1, c3, c4);
//      executorService.schedule(() -> all2
//              .completeExceptionally(new TimeoutException("Timeout occurred")), 100, TimeUnit.MILLISECONDS);
//      assertThrows(ExecutionException.class, all2::get);
//    } catch (Exception ignored) {
//    }

    try {
      CompletableFuture<Void> all2 = allOf(c2_1, c3, c4);
      CompletableFuture<Void> future = c1.orTimeout(100, TimeUnit.MILLISECONDS);
      all2.orTimeout(100, TimeUnit.MILLISECONDS).whenComplete((v,ex) -> { if (ex!=null) out.println(ex.getMessage());});
      all2.join();
    } catch (Exception ignored) {
      out.println("Error" + ignored);
    }
  }

  public static void main(String[] args) {
    out.println("\n");
    out.println("This program shows the outcome of cancelling completable futures when failing fast.");

    out.println("\n");
    out.println("Without task #2 chain cancellation, tasks #3 & #4 will run sequentially.");
    showcaseCancellation(now().toEpochMilli(), false);
    
    out.println("\n");
    out.println("With task #2 chain cancellation, tasks #3 & #4 will run in parallel.");
    showcaseCancellation(now().toEpochMilli(), true);
    
    System.exit(0);
  }
}