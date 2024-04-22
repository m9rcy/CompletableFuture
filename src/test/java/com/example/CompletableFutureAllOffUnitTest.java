package com.example;

import static com.example.CompletableFutureUtil.within;
import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class CompletableFutureAllOffUnitTest {

    @Test
    void whenCallingJoin_thenBlocksThreadAndGetValue() {
        CompletableFuture<String> future = waitAndReturn(1_000, "Harry");
        assertEquals("Harry", future.join());
    }

    @Test
    void whenCallingJoin_thenBlocksThreadAndThrowException() {
        CompletableFuture<String> futureError = waitAndThrow(1_000);
        assertThrows(RuntimeException.class, futureError::join);
    }

    @Test
    void whenCallingJoinTwoTimes_thenBlocksThreadAndGetValues() {
        CompletableFuture<String> f1 = waitAndReturn(1_000, "Harry");
        CompletableFuture<String> f2 = waitAndReturn(2_000, "Ron");

        assertEquals("Harry", f1.join());
        assertEquals("Ron", f2.join());
    }


    @Test
    void whenCallingAllOfJoin_thenBlocksThreadAndGetValues() throws ExecutionException, InterruptedException {
        CompletableFuture<String> f1 = waitAndReturn(1_000, "Harry");
        CompletableFuture<String> f2 = waitAndReturn(2_000, "Ron");

        CompletableFuture<Void> combinedFutures1 = CompletableFuture.allOf(f1, f2);
        CompletableFuture<?> combinedFutures = within(f1, Duration.ofSeconds(6));
        //combinedFutures.join();

        final CompletableFuture<?> responseFuture = within(
                combinedFutures1, Duration.ofSeconds(5));
        responseFuture
                .exceptionally(throwable -> {
                    System.out.println("Unrecoverable error" +throwable);
                    return null;
                });

        responseFuture.get();
        assertEquals("Harry", f1.join());
        assertEquals("Ron", f2.join());
    }

    @Test
    void whenCallingAllOfJoin_thenBlocksThreadAndGetValues__() throws ExecutionException, InterruptedException {
        CompletableFuture<String> f1 = waitAndReturn(1_000, "Harry");
        CompletableFuture<String> f2 = waitAndReturn(2_000, "Ron");

        CompletableFuture<Void> combinedFutures1 = within(CompletableFuture.allOf(f1, f2), Duration.ofSeconds(5));
//        combinedFutures1.whenComplete((v,ex) -> { if (ex!=null) out.println(ex.getMessage());});
//        combinedFutures1.get();


        combinedFutures1.join();


        //assertEquals("Harry", f1.join());
        //assertEquals("Ron", f2.join());
    }

    @Test
    void whenCallingJoinInaLoop_thenProcessesDataPartially() {
        CompletableFuture<String> f1 = waitAndReturn(1_000, "Harry");
        CompletableFuture<String> f2 = waitAndThrow(2_000);
        CompletableFuture<String> f3 = waitAndReturn(1_000, "Ron");

        assertThrows(RuntimeException.class, () -> Stream.of(f1, f2, f3)
            .map(CompletableFuture::join)
            .forEach(this::sayHello));
    }

    @Test
    void whenCallingAllOfJoin_thenFailsForAll() {
        CompletableFuture<String> f1 = waitAndReturn(1_000, "Harry");
        CompletableFuture<String> f2 = waitAndThrow(2_000);
        CompletableFuture<String> f3 = waitAndReturn(1_000, "Ron");

        assertThrows(RuntimeException.class, () -> CompletableFuture.allOf(f1, f2, f3)
            .join());
    }

    @Test
    void whenCallingExceptionally_thenRecoversWithDefaultValue() {
        CompletableFuture<String> f1 = waitAndReturn(1_000, "Harry");
        CompletableFuture<String> f2 = waitAndThrow(2_000);
        CompletableFuture<String> f3 = waitAndReturn(1_000, "Ron");

        CompletableFuture<String> names = CompletableFuture.allOf(f1, f2, f3)
            .thenApply(__ -> f1.join() + "," + f2.join() + "," + f3.join())
            .exceptionally(err -> {
                System.out.println("oops, there was a problem! " + err.getMessage());
                return "names not found!";
            });

        assertEquals("names not found!", names.join());
    }

    @Test
    void whenCallingExceptionally_thenRecoversWithDefaultValue__() {
        CompletableFuture<String> f1 = waitAndReturn(1_000, "Harry");
        CompletableFuture<String> f2 = waitAndThrow(2_000);
        CompletableFuture<String> f3 = waitAndReturn(1_000, "Ron");

        CompletableFuture<String> names = CompletableFutureUtil.allOfTerminateOnTimeout(Duration.ofSeconds(1), f1, f2, f3)
                .thenApply(__ -> f1.join() + "," + f2.join() + "," + f3.join())
                .exceptionally(err -> {
                    System.out.println("oops, there was a problem! " + err.getMessage());
                    return "names not found!";
                });


        assertEquals("names not found!", names.join());
    }

    private void sayHello(String name) {
        System.out.println(LocalDateTime.now() + " - " + name);
    }

    private CompletableFuture<String> waitAndReturn(long millis, String value) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                //                Thread.sleep() is commented to avoid slowing down the pipeline
                //                Thread.sleep(millis);
                return value;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<String> waitAndThrow(long millis) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                //                Thread.sleep() is commented to avoid slowing down the pipeline
                //                Thread.sleep(millis);
            } finally {
                throw new RuntimeException();
            }
        });
    }

}