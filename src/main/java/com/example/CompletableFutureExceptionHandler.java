package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompletableFutureExceptionHandler {
    
    public static <T> CompletableFuture<List<T>> allOfWithExceptions(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<Void> allFutureResult = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));

        return allFutureResult.thenApply(__ -> {
            List<Throwable> exceptions = futuresList.stream()
                    .filter(CompletableFuture::isCompletedExceptionally)
                    .map(future -> {
                        try {
                            future.join(); // this will throw ExecutionException
                        } catch (CompletionException e) {
                            return e.getCause();
                        }
                        return null;
                    })
                    .collect(Collectors.toList());

            if (!exceptions.isEmpty()) {
                throw new CompletionExceptionWithMultipleCauses("Multiple exceptions occurred", exceptions);
            }

            return (List<T>) new ArrayList<T>(futuresList.stream()
                    .filter(CompletableFuture::isDone)
                    .map(CompletableFuture::join)
                    .toList());
        });
    }

    static class CompletionExceptionWithMultipleCauses extends RuntimeException {
        private final List<Throwable> causes;

        public CompletionExceptionWithMultipleCauses(String message, List<Throwable> causes) {
            super(message);
            this.causes = causes;
        }

        public List<Throwable> getCauses() {
            return causes;
        }
    }

    public static void main(String[] args) {
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

        CompletableFuture<List<String>> resultFuture = allOfWithExceptions(futuresList);

        resultFuture.exceptionally(throwable -> {
            if (throwable instanceof CompletionExceptionWithMultipleCauses) {
                List<Throwable> causes = ((CompletionExceptionWithMultipleCauses) throwable).getCauses();
                System.out.println("Multiple exceptions occurred:");
                for (Throwable cause : causes) {
                    System.out.println(cause.getMessage());
                }
            } else {
                System.out.println("Unexpected exception occurred: " + throwable.getMessage());
            }
            return null;
        });

        // Retrieve the result
        try {
            List<String> resultList = resultFuture.get();
            System.out.println("Results:");
            for (String result : resultList) {
                System.out.println(result);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
