package com.example;


import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class AllOfWithTimeoutTest {

    public static final int TIMEOUT_IN_MILLIS = 100;

    @Test
    public void allOfOrTimeout1() throws InterruptedException, ExecutionException, TimeoutException {
        getAllOfFuture().get(TIMEOUT_IN_MILLIS, MILLISECONDS);
    }

    @Test
    public void allOfOrTimeout2() throws ExecutionException, InterruptedException {
        getAllOfFuture().orTimeout(TIMEOUT_IN_MILLIS, MILLISECONDS);
    }

    @Test
    public void allOfOrTimeout3() throws ExecutionException, InterruptedException {
        //getAllOfFuture().orTimeout(TIMEOUT_IN_MILLIS, MILLISECONDS);
        getAllOfFuture().orTimeout(1, MILLISECONDS).exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
                // Handle the timeout exception here
                System.out.println("The operation timed out.");

            }
            return null;
        });

        getAllOfFuture().get();
    }

    private CompletableFuture<Void> getAllOfFuture() {
        return CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> sleep(100)),
            CompletableFuture.runAsync(() -> sleep(200)),
            CompletableFuture.runAsync(() -> sleep(300)),
            CompletableFuture.runAsync(() -> sleep(400)),
            CompletableFuture.runAsync(() -> sleep(500)),
            CompletableFuture.runAsync(() -> sleep(600)),
            CompletableFuture.runAsync(() -> sleep(700)),
            CompletableFuture.runAsync(() -> sleep(800))
        );
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
            System.out.format("Had a nap for %s milliseconds.\r\n", millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    };

    static void completeFutures(CompletableFuture<?>... completableFutures) throws ExecutionException {
        try {
            CompletableFuture.allOf(completableFutures).get(1, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final TimeoutException e) {
            for (CompletableFuture<?> cf : completableFutures) {
                System.out.println(e.getMessage());
                //cf.completeExceptionally(e);
                cf.cancel(true);
            }
        }
    }
}
