package edu.onu.ddechev.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ProfilingUtil {

    public static class ProfilingResult<T> {
        private final T result;
        private final double executionTime;

        ProfilingResult(T result, double executionTime) {
            this.result = result;
            this.executionTime = executionTime;
        }

        public T getResult() {
            return result;
        }

        public double getExecutionTime() {
            return executionTime;
        }
    }

    private ProfilingUtil() {
    }

    public static <T> ProfilingResult<T> executionTime(Supplier<T> function) {
        return executionTime(function, 25);
    }

    public static <T> ProfilingResult<T> executionTime(Supplier<T> function, Integer times) {
        List<Long> results = new ArrayList<>();
        T result = null;
        for (int attempt = 0; attempt < times; attempt ++) {
            long time = System.currentTimeMillis();
            result = function.get();
            results.add(System.currentTimeMillis() - time);
        }
        return new ProfilingResult<>(result, results.stream().mapToLong(Long::longValue).average().orElse(0.0));
    }
}
