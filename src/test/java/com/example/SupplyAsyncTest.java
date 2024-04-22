package com.example;


import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SupplyAsyncTest {
    @Test
    public void supplyAsync() throws ExecutionException, InterruptedException {
        CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> "Hello, Future!");

        assertThat(completableFuture.get()).isEqualTo("Hello, Future!");
    }

    @Test
    public void thenApplyAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Future");

        completableFuture = completableFuture.thenApplyAsync((s) -> s.concat(" is awesome!"));

        assertThat(completableFuture.get()).isEqualTo("Future is awesome!");
    }

    @Test
    public void thenApplyAsyncWithExecutor() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Future", executorService);

        completableFuture = completableFuture.thenApplyAsync((s) -> s.concat(" is awesome!"), executorService);

        executorService.shutdown();

        assertThat(completableFuture.get()).isEqualTo("Future is awesome!");
    }

    @Test
    public void thenComposeAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<String> composedCompletableFuture = CompletableFuture
                .supplyAsync(() -> "Future")
                .thenComposeAsync(s -> CompletableFuture.supplyAsync(() -> s.concat(" is awesome!")));

        assertThat(composedCompletableFuture.get()).isEqualTo("Future is awesome!");
    }

    @Test
    public void thenCombineAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture1 = CompletableFuture.supplyAsync(() -> "Future");
        CompletableFuture<String> completableFuture2 = CompletableFuture.supplyAsync(() -> " is awesome!");

        CompletableFuture<String> combinedCompletableFuture = completableFuture1.thenCombineAsync(completableFuture2, (s1, s2) -> s1.concat(s2));

        assertThat(combinedCompletableFuture.get()).isEqualTo("Future is awesome!");
    }

    @Test
    public void allOf() throws ExecutionException, InterruptedException {
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Future");
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> " is awesome!");
        CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "Sleep";}).orTimeout(1, TimeUnit.SECONDS);
        //CompletableFutureUtil.failAfter(Duration.ofSeconds(10)
        CompletableFuture<String>[] cfs = new CompletableFuture[]{cf1, cf2, cf3};


        CompletableFuture<Void> allCf = CompletableFuture.allOf(cfs);
        allCf.get();

        String result = Arrays.stream(cfs).map(CompletableFuture::join).collect(Collectors.joining());
        assertThat(result).isEqualTo("Future is awesome!Sleep");
    }

    @Test
    public void anyOf() throws ExecutionException, InterruptedException {
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "Future");
        CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> " is awesome!");
        CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> "!");

        CompletableFuture<Object> anyCf = CompletableFuture.anyOf(cf1, cf2, cf3);
        System.out.println(anyCf.get());

        assertThat(anyCf).isDone();
    }

    @Test
    public void thenAcceptAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Future");

        completableFuture = completableFuture.thenApplyAsync((s) -> s.concat(" is awesome!"));
        CompletableFuture<Void> procedureFuture = completableFuture.thenAcceptAsync(System.out::println);

        assertThat(procedureFuture.get()).isNull();
    }

    @Test
    public void thenRunAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "Future");

        completableFuture = completableFuture.thenApplyAsync((s) -> s.concat(" is awesome!"));
        CompletableFuture<Void> procedureFuture = completableFuture.thenRunAsync(() -> System.out.println("!"));

        assertThat(procedureFuture.get()).isNull();
    }
}