package com.example;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompletableFutureExample {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            // Simulate some computation
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Simulate a successful result
            return "Result from future1";
        });

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            // Simulate some computation
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Simulate an exception
            throw new RuntimeException("Exception from future2");
        });

        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            // Simulate some computation
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Simulate a successful result
            return "Result from future3";
        });

        List<CompletableFuture<String>> futuresList = Stream.of(future1, future2, future3)
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutureResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));

        try {
            // Use get with timeout to wait for all futures to complete
            allFutureResult.get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            System.out.println("Timeout occurred while waiting for all futures to complete.");
        } catch (ExecutionException e) {
            // Handle ExecutionException if any future completes exceptionally
            System.out.println("Exception occurred: " + e.getCause().getMessage());
        }

        // Now, you can iterate over the futuresList and handle individual exceptions
        for (CompletableFuture<String> future : futuresList) {
            if (future.isCompletedExceptionally()) {
                System.out.println("Future completed exceptionally: " + future.toString());
                System.out.println("Future completed exceptionally: " + future);
            } else {
                try {
                    String result = future.get(); // This will throw ExecutionException if the future completed exceptionally
                    System.out.println("Result: " + result);
                } catch (ExecutionException e) {
                    // Handle individual future exceptions
                    Throwable cause = e.getCause();
                    System.out.println("Exception occurred: " + cause.getMessage());
                }
            }
        }
    }
}
