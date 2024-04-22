package com.example;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CompletableFutureUtil {

    static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public static CompletableFuture allOfTerminateOnFailure(CompletableFuture<?>... futures) {
        CompletableFuture<?> failure = new CompletableFuture();
        for (CompletableFuture<?> f: futures) {
            f.exceptionally(ex -> {
                failure.completeExceptionally(ex);
                return null;
            });
        }

        failure.exceptionally(ex -> {
            Arrays.stream(futures).forEach(f -> f.cancel(true));
            return null;
        });

        return CompletableFuture.anyOf(failure, CompletableFuture.allOf(futures));
    }

    public static <T> CompletableFuture<T> within(CompletableFuture<T> future, Duration duration) {
        final CompletableFuture<T> timeout = failAfter(duration);
        return future.applyToEither(timeout, Function.identity());
    }

    public static <T> CompletableFuture<T> failAfter(Duration duration) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        SCHEDULER.schedule(() -> {
            final TimeoutException ex = new TimeoutException("Timeout after " + duration);
            return promise.completeExceptionally(ex);
        }, duration.toMillis(), MILLISECONDS);
        return promise;
    }

    @SafeVarargs
    public static <T> CompletableFuture<Object> allOfTerminateOnTimeout(Duration timeout, CompletableFuture<T>... futures) {
        CompletableFuture<?> timeoutFuture = failAfter(timeout);
        for (CompletableFuture<?> f: futures) {
            f.exceptionally(ex -> {
                timeoutFuture.completeExceptionally(ex);
                return null;
            });
        }

        timeoutFuture.exceptionally(throwable -> {
            Arrays.stream(futures).forEach(f -> f.cancel(true));
            return null;
        });

        return CompletableFuture.anyOf(timeoutFuture, CompletableFuture.allOf(futures));
    }

    public static <T> CompletableFuture<Object> allOfTerminateOnTimeout1(Duration timeout, CompletableFuture<T>... futures) {
        CompletableFuture<?> timeoutFuture = failAfter(timeout);

        for (CompletableFuture<?> f: futures) {
            f.exceptionally(ex -> {
                timeoutFuture.completeExceptionally(ex);
                return null;
            });
        }

        timeoutFuture.exceptionally(ex -> {
            Arrays.stream(futures).forEach(f -> f.cancel(true));
            return null;
        });

        return CompletableFuture.anyOf(timeoutFuture, CompletableFuture.allOf(futures));
    }
}
