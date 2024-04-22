package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Sample {

    public static void main(String[] args) throws Exception
    {
        int maxSleepTime = 1000;
        Random random = new Random();
        AtomicInteger value = new AtomicInteger();
        List<String> calculatedValues = new ArrayList<>();
        Supplier<String> process = () -> { try { Thread.sleep(random.nextInt(maxSleepTime)); System.out.println("Stage 1 Running!"); } catch (InterruptedException e) { e.printStackTrace(); } return Integer.toString(value.getAndIncrement()); };
        List<CompletableFuture<String>> stage1 = IntStream.range(0, 10).mapToObj(val -> CompletableFuture.supplyAsync(process)).collect(Collectors.toList());
        List<CompletableFuture<String>> stage2 = null;//stage1.stream().map(Test::appendNumber).collect(Collectors.toList());
        List<CompletableFuture<String>> stage3 = null;//stage2.stream().map(Test::printIfCancelled).collect(Collectors.toList());
        CompletableFuture<Void> awaitAll = null;//CompletableFuture.allOf(stage2.toArray(new CompletableFuture[0]));
        try
        {
            /*Wait 1/2 the time, some should be complete. Some not complete -> TimeoutException*/
            awaitAll.get(maxSleepTime / 2, TimeUnit.MILLISECONDS);
        }
        catch(TimeoutException ex)
        {
            for(CompletableFuture<String> toCancel : stage2)
            {
                boolean irrelevantValue = false;
                if(!toCancel.isDone())
                    toCancel.cancel(irrelevantValue);
                else
                    calculatedValues.add(toCancel.join());
            }
        }
        System.out.println("All futures Cancelled! But some Stage 1's may still continue printing anyways.");
        System.out.println("Values returned as of cancellation: " + calculatedValues);
        Thread.sleep(maxSleepTime);
    }

    private static CompletableFuture<String> appendNumber(CompletableFuture<String> baseFuture)
    {
        return baseFuture.thenApply(val -> {  System.out.println("Stage 2 Running"); return "#" + val; });
    }

    private static CompletableFuture<String> printIfCancelled(CompletableFuture<String> baseFuture)
    {
        return baseFuture.thenApply(val ->  { System.out.println("Stage 3 Running!"); return val; }).exceptionally(ex -> { System.out.println("Stage 3 Cancelled!"); return ex.getMessage(); });
    }
}
