package com.example;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.allOf;

public class NewTest {

    private Instant start;

    public static void main(String[] args) {

        NewTest main = new NewTest();
        main.start();
    }

    public void start() {
        String req1 = "http://localhost:8080/testing";
        String req2 = "http://127.0.0.1:8095/testing2";

        ExecutorService exec = Executors.newCachedThreadPool();

        start = Instant.now();
        CompletableFuture<String> comp1 = CompletableFuture.supplyAsync(() -> doReq(req1), exec);
        CompletableFuture<String> comp2 = CompletableFuture.supplyAsync(() -> doReq(req2), exec);
        CompletableFuture<String> comp3 = CompletableFuture.supplyAsync(() -> { throw new IllegalArgumentException("Not here!");}, exec);

        List<CompletableFuture<String>> completables = List.of(comp1, comp3, comp2);

        System.out.println("Waiting completables");

        List<String> r = getAllCompleted(completables, 3, TimeUnit.SECONDS);
        //allOfUnlessFailed(comp1, comp3, comp2).join();
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
                .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .collect(Collectors.<T>toList());
    }

    public static CompletableFuture<Void> allOfUnlessFailed(CompletableFuture<?> ...futures) {
        CompletableFuture<?>[] rest = Stream.of(futures)
                .filter(f -> !f.isDone() || f.isCompletedExceptionally())
                .toArray(CompletableFuture[]::new);
        if (rest.length == 0)
            return CompletableFuture.completedFuture(null);
        return CompletableFuture.anyOf(rest)
                .thenCompose(__ -> allOfUnlessFailed(rest));
    }
}
