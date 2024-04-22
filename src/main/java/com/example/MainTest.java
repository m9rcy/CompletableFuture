package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MainTest {

    private Instant start;

    public static void main(String[] args) {

        com.example.MainTest1 main = new com.example.MainTest1();
        main.start();
    }

    public void start() {
        TimeoutHandler timeoutHandler = new TimeoutHandler();
        String req1 = "http://localhost:8080/testing";
        String req2 = "http://127.0.0.1:8095/testing2";

        ExecutorService exec = Executors.newCachedThreadPool();

        start = Instant.now();
        CompletableFuture<String> comp1 = CompletableFuture.supplyAsync(() -> doReq(req1), exec);
        CompletableFuture<String> comp2 = CompletableFuture.supplyAsync(() -> doReq(req2), exec);

        List<CompletableFuture<String>> completables = List.of(comp1, comp2);

        System.out.println("Waiting completables");

//        List<String> r = null;
//        try {
//            r = timeoutHandler.getAllUnlessTimeout3(completables, Duration.ofSeconds(3)).join();
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }

//        List<String> r = null;
//        try {
//            r = getAllCompleted(completables, 3, TimeUnit.SECONDS);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (TimeoutException e) {
//            throw new RuntimeException(e);
//        }
//        List<String> r = getAllCompleted(completables, 3, TimeUnit.SECONDS);

        List<String> r = getAllUnlessTimeout5(completables, Duration.ofSeconds(1)).join();

//        List<String> r = null;
//        try {
//            r = getAllUnlessTimeout(completables, 1, TimeUnit.SECONDS);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }
       // List<String> r = getAllUnlessTimeout2(completables, 1, TimeUnit.SECONDS);
        Instant end = Instant.now();
        //System.out.println(" Took: " + DurationFormatUtils.formatDurationHMS(Duration.between(start, end).toMillis()));
        System.out.println(" Took: " + Duration.between(start, end).toMillis());

        System.out.println(r.size());
        r.forEach(System.out::println);
        exec.shutdown();
    }

    public String doReq(String request) {
        if (request.contains("localhost")) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "response1";
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "response2";
    }

    public <T> List<T> getAllCompleted(List<CompletableFuture<T>> futuresList, long timeout, TimeUnit unit) {
        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));
        try {
            allFuturesResult.get(timeout, unit);
        } catch (Exception e) {
            e.printStackTrace();
            //throw e;
        }
        return futuresList.stream()
            .filter(future -> future.isDone() && !future.isCompletedExceptionally()) // keep only the ones completed
            .map(CompletableFuture::join) // get the value from the completed future
            .collect(Collectors.<T>toList()); // collect as a list
    }

    public <T> List<T> getAllUnlessTimeout(List<CompletableFuture<T>> futuresList, long timeout, TimeUnit unit) throws ExecutionException {
        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));
        Throwable ex = null;
        try {
            allFuturesResult.get(timeout, unit);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            ex = e;
            for (CompletableFuture<T> toCancel : futuresList) {
                toCancel.completeExceptionally(e);
            }
        }

        boolean allCompletedExceptionally = futuresList.stream().allMatch(CompletableFuture::isCompletedExceptionally);
        if (allCompletedExceptionally) {
            throw new ExecutionException(ex);
        }

        return futuresList.stream()
                .filter(f -> !f.isCompletedExceptionally()) // keep only the ones without exception
                .map(CompletableFuture::join) // get the value from the completed future
                .collect(Collectors.<T>toList()); // collect as a list
    }

    public <T> List<T> getAllUnlessTimeout2(List<CompletableFuture<T>> futuresList, long timeout, TimeUnit unit) {
        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));
        List<T> successfulResults = new ArrayList<>();

        try {
            allFuturesResult.get(timeout, unit);
            // If all futures completed successfully, collect their results
            for (CompletableFuture<T> future : futuresList) {
                if (!future.isCompletedExceptionally()) {
                    successfulResults.add(future.get());
                }
            }
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            for (CompletableFuture<T> toCancel : futuresList) {
                toCancel.completeExceptionally(e);
            }
        }

        return successfulResults;
    }

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
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .collect(Collectors.<T>toList()));
    }

    public <T> CompletableFuture<List<T>> getAllUnlessTimeout(List<CompletableFuture<T>> futuresList, Duration timeout) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> allFutureResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));

        allFutureResult.completeOnTimeout(null, timeout.toSeconds(), TimeUnit.SECONDS);


        return allFutureResult.thenApplyAsync(__ -> futuresList.stream()
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .collect(Collectors.<T>toList()));
    }

    public <T> CompletableFuture<List<T>> getAllUnlessTimeout5(List<CompletableFuture<T>> futuresList, Duration timeout) {
        CompletableFuture<Void> allFutureResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));

        return allFutureResult
                .completeOnTimeout(null, timeout.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(__ -> futuresList.stream()
                        .filter(CompletableFuture::isDone)
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())).exceptionally(throwable->{
                    // Handle timeout exception here
                    throw new RuntimeException("Timeout occurred while waiting for all futures to complete.", throwable);
                });
    }

}