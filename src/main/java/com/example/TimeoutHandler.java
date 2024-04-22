package com.example;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TimeoutHandler {

    public <T> CompletableFuture<List<T>> getAllUnlessTimeout3(List<CompletableFuture<T>> futuresList, Duration timeout) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> allFutureResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));

        try {
            // Wait for all futures to complete within the timeout
            allFutureResult.get(timeout.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            // If any exception occurred during completion, rethrow it
            throw e;
        } catch (TimeoutException e) {
            // If a timeout occurred, complete the allFuturesResult exceptionally with a TimeoutException
            allFutureResult.completeExceptionally(e);
        }

        return allFutureResult.thenApplyAsync(__ -> futuresList.stream()
                .filter(f->!f.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .collect(Collectors.<T>toList()));
    }

    public <T> List<T> getAllUnlessTimeout2(List<CompletableFuture<T>> futuresList, long timeout, TimeUnit unit) throws TimeoutException, ExecutionException, InterruptedException {
        // Check if the futuresList is empty, throw an IllegalArgumentException if it is
        if (futuresList.isEmpty()) {
            throw new IllegalArgumentException("futuresList cannot be empty");
        }

        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(
                futuresList.toArray(new CompletableFuture[futuresList.size()]));

        try {
            // Wait for all futures to complete within the timeout
            allFuturesResult.get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            // If any exception occurred during completion, rethrow it
            throw e;
        } catch (TimeoutException e) {
            // If a timeout occurred, complete the allFuturesResult exceptionally with a TimeoutException
            allFuturesResult.completeExceptionally(e);
            throw e;
        }

        // Combine the results of successful futures
        return futuresList.stream()
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
    public <T> List<T> getAllUnlessTimeout(List<CompletableFuture<T>> futuresList, long timeout, TimeUnit unit) throws Throwable {
        // Check if the futuresList is empty, throw an IllegalArgumentException if it is
        if (futuresList.isEmpty()) {
            throw new IllegalArgumentException("futuresList cannot be empty");
        }

        // Create a CompletableFuture for handling timeout
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();
        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(
                futuresList.toArray(new CompletableFuture[futuresList.size()]))
                .orTimeout(timeout, unit)
                .exceptionally(throwable -> {
                    timeoutFuture.completeExceptionally(new TimeoutException("Timeout occurred"));
                    return null;
                });

        try {
            // Wait for all futures to complete or timeout
            allFuturesResult.join();
        } catch (CompletionException e) {
            // If any exception occurred during completion or timeout, rethrow it
            timeoutFuture.cancel(false);
            throw e.getCause();
        }

        // Check if timeout occurred
        if (timeoutFuture.isCompletedExceptionally()) {
            throw new TimeoutException("Timeout occurred while waiting for all futures");
        }

        // Combine the results of successful futures
        return futuresList.stream()
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
}
