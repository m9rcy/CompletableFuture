package com.example;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletableFutureTimeoutExample {
    public static void main(String[] args) {
        // Create your CompletableFuture
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            // Simulate some long-running operation
            try {
                Thread.sleep(2000);
                System.out.println("Future 1 completed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            // Simulate some long-running operation
            try {
                Thread.sleep(3000);
                System.out.println("Future 2 completed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Combine the CompletableFutures using allOf
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(future1, future2);

        // Add a timeout using CompletableFuture.completeOnTimeout()
        CompletableFuture<Void> timeoutFuture = combinedFuture.completeOnTimeout(null, 1, TimeUnit.SECONDS);

        // Handle the completion or timeout
        timeoutFuture.whenComplete((result, throwable) -> {
            if (throwable instanceof TimeoutException) {
                System.out.println("Timeout occurred: " + throwable.getMessage());
            } else {
                System.out.println("All futures completed successfully");
            }
        });

        // Wait for the completion of the combined future or timeout
        try {
            timeoutFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
