package edu.onu.ddechev.utils;

import java.util.function.Supplier;

public class ProfilingUtil {

    public static class ProfilingResult<T> {
        private final T result;
        private final long executionTime;

        ProfilingResult(T result, long executionTime) {
            this.result = result;
            this.executionTime = executionTime;
        }

        public T getResult() {
            return result;
        }

        public long getExecutionTime() {
            return executionTime;
        }
    }

    private ProfilingUtil() {
    }

    public static <T> ProfilingResult<T> executionTime(Supplier<T> function) {
        long time = System.currentTimeMillis();
        T result = function.get();
        return new ProfilingResult<>(result, System.currentTimeMillis() - time);
    }
}
