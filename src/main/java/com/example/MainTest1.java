package com.example;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.allOf;

public class MainTest1 {

    private Instant start;

    public static void main(String[] args) {

        MainTest1 main = new MainTest1();
        main.start();
    }

    public void start() {
        String req1 = "http://localhost:8080/testing";
        String req2 = "http://127.0.0.1:8095/testing2";

        ExecutorService exec = Executors.newCachedThreadPool();

        start = Instant.now();
        CompletableFuture<String> comp1 = orTimeout(CompletableFuture.supplyAsync(() -> doReq(req1), exec),3, TimeUnit.SECONDS);
        CompletableFuture<String> comp2 = orTimeout(CompletableFuture.supplyAsync(() -> doReq(req2), exec),3, TimeUnit.SECONDS);

        List<CompletableFuture<String>> completables = List.of(comp1, comp2);

        System.out.println("Waiting completables");

        //List<String> r = allOfOrTimeout(completables, 3, TimeUnit.SECONDS), 1, TimeUnit.SECONDS).join();
        List<String> r = getAll(completables).join();
        Instant end = Instant.now();
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
        CompletableFuture<Void> allFuturesResult = allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));
        try {
            allFuturesResult.get(timeout, unit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return futuresList.stream()
            .filter(future -> future.isDone() && !future.isCompletedExceptionally()) // keep only the ones completed
            .map(CompletableFuture::join) // get the value from the completed future
            .collect(Collectors.<T>toList()); // collect as a list
    }

    public <T> CompletableFuture<List<T>> getAllUnlessTimeout(List<CompletableFuture<T>> futuresList, long timeout, TimeUnit unit) {
        CompletableFuture<Void> allFutureResult = allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));

        allFutureResult.completeOnTimeout(null, timeout, unit);

        return allFutureResult.thenApplyAsync(__ -> futuresList.stream()
                .filter(CompletableFuture::isDone)
                .map(CompletableFuture::join)
                .collect(Collectors.<T>toList()))
                .handle((result, ex) -> {
                    if (ex != null) {
                        System.err.println("Exception: " + ex.getMessage());
                        return null;
                    } else {
                        return result;
                    }
                });
    }

    public <T> CompletableFuture<List<T>> allOfOrTimeout(List<CompletableFuture<T>> futuresList, long timeout, TimeUnit unit) {
        CompletableFuture<Void> allFutureResult = allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));

        allFutureResult.orTimeout(timeout, unit).exceptionallyCompose(throwable -> {
            if (throwable instanceof TimeoutException) {
                final var msg = String.format(
                        "The Something timed-out in %d %s.",
                        timeout, unit
                );
                return CompletableFuture.failedFuture(new TimeoutException(msg));
            }
            return CompletableFuture.failedFuture(throwable);
        });

        return allFutureResult.thenApplyAsync(__ -> futuresList.stream()
                        .filter(CompletableFuture::isDone)
                        .map(CompletableFuture::join)
                        .collect(Collectors.<T>toList()));
    }

    public <T> CompletableFuture<List<T>> getAll(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<Void> allFutureResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));

        return allFutureResult
                .thenApply(__ -> futuresList.stream()
                        .filter(CompletableFuture::isDone)
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    public static <T> CompletableFuture<T> orTimeout(
            CompletableFuture<T> cf,
            long timeout, TimeUnit unit
    ) {
        return cf.orTimeout(timeout, unit)
                .exceptionallyCompose(throwable -> {
            if (throwable instanceof TimeoutException) {
                final var msg = String.format("Timed out %d %s.",
                        timeout, unit
                );
                return CompletableFuture.failedFuture(new TimeoutException(msg));
            }
            return CompletableFuture.failedFuture(throwable);
        });
    }
}