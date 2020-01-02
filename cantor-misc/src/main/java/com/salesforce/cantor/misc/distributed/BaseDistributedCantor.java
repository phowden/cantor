package com.salesforce.cantor.misc.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.salesforce.cantor.common.CommonPreconditions.checkArgument;

public class BaseDistributedCantor<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseDistributedCantor.class);

    private static final long maxReadWaitMillis = 10_000L;
    private static final long maxWriteWaitMillis = 30_000L;
    // todo: bounded queue, rejection, etc
    private static final ExecutorService executor = Executors.newWorkStealingPool();

    private final double ratio;
    private final List<T> delegates;

    BaseDistributedCantor(final List<T> delegates, final double ratio) {
        checkArgument(delegates.size() * ratio > 0, "incompatible delegates/ratio");
        this.ratio = ratio;
        this.delegates = new ArrayList<>(delegates);
    }

    Collection<String> getDistributedNamespaces(final IOFunction<T, Collection<String>> namespaceCall) throws IOException {
        try {
            final List<Callable<Collection<String>>> callables = new ArrayList<>(delegates.size());
            for (final T delegate : delegates) {
                callables.add(tryCatch("namespaces", namespaceCall, delegate));
            }
            // blocks til all are done
            final List<Future<Collection<String>>> futures = executor.invokeAll(callables);
            final Set<String> allNamespaces = new HashSet<>();
            for (final Future<Collection<String>> future : futures) {
                final Collection<String> namespaces = future.get();
                if (namespaces != null && !namespaces.isEmpty()) {
                    allNamespaces.addAll(namespaces);
                }
            }

            return allNamespaces;
        } catch (final InterruptedException | ExecutionException e) {
            throw new IOException("interrupted waiting for namespaces", e);
        }
    }
    void write(final String name, final IOConsumer<T> writeFunction) throws IOException {
        writeAndGetResult(name, (t) -> {
            writeFunction.consume(t);
            return true;
        });
    }

    // todo: success ratio less than one can result in mismatches, inconsistencies, do we have solutions?
    <R> R writeAndGetResult(final String name, final IOFunction<T, R> func) throws IOException {
        final int requiredSuccess = (int) (delegates.size() * ratio);
        final List<Future<R>> futures = new ArrayList<>(delegates.size());
        for (final T delegate : delegates) {
            futures.add(executor.submit(tryCatch(name, func, delegate)));
        }

        final long deadlineTimestampMillis = System.currentTimeMillis() + maxWriteWaitMillis;
        long sleepTimeMillis = 1;
        R result = null;
        int successes = 0;
        while (successes < requiredSuccess && !futures.isEmpty()) {
            for (Iterator<Future<R>> iter = futures.iterator(); iter.hasNext();) {
                final Future<R> future = iter.next();
                try {
                    if (future.isDone()) {
                        final R val = future.get();
                        successes += val != null ? 1 : 0;
                        result = val;
                        iter.remove();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new IOException("exception getting results of distributed cantor writes", e);
                }
            }
            sleepTimeMillis = sleepAndBackoff(sleepTimeMillis, deadlineTimestampMillis);
        }

        if (successes < requiredSuccess) {
            // todo: rollback here? Is there a fix? pass a rollback function? can that be derived from all writes?
            throw new IOException(String.format("%s failed to write sufficient replicas for ratio %s: %s/%s",
                    name, ratio, successes, requiredSuccess));
        } else {
            logger.debug("{} succeeded with {}/{} successes", name, successes, requiredSuccess);
            return result;
        }
    }

    <R> R read(final String name, final IOFunction<T, R> func) throws IOException {
        final List<Future<R>> futures = new ArrayList<>(delegates.size());
        for (final T delegate : delegates) {
            futures.add(executor.submit(tryCatch(name, func, delegate)));
        }

        final long deadlineTimestampMillis = System.currentTimeMillis() + maxReadWaitMillis;
        long sleepTimeMillis = 1;
        R result = null;
        while (result == null && !futures.isEmpty()) {
            for (Iterator<Future<R>> iter = futures.iterator(); iter.hasNext();) {
                final Future<R> future = iter.next();
                try {
                    if (future.isDone()) {
                        // null can mean "value is null" or also "i don't have that data"
                        result = future.get();
                        iter.remove();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new IOException("exception reading from distributed cantor", e);
                }
            }
            sleepTimeMillis = sleepAndBackoff(sleepTimeMillis, deadlineTimestampMillis);
        }
        return result;
    }

    private <R> Callable<R> tryCatch(final String name, final IOFunction<T, R> function, final T delegate) {
        return () -> {
            try {
                logger.debug("executing read operation '{}' from {}", name, delegate);
                return function.apply(delegate);
            } catch (final IOException e) {
                logger.warn("io exception in read operation '{}': {}", name, e.getMessage());
                return null;
            }
        };
    }

    private long sleepAndBackoff(final long sleepTimeMillis, final long deadlineTimestampMillis) throws IOException {
        if (System.currentTimeMillis() > deadlineTimestampMillis) {
            throw new IOException("passed deadline timestamp waiting for distributed cantor response(s)");
        }
        try {
            Thread.sleep(sleepTimeMillis);
        } catch (InterruptedException e) {
            throw new IOException("interrupted waiting for distributed cantor response(s)");
        }
        return sleepTimeMillis * 2;
    }

    static interface IOFunction<T, R> {
        R apply(T t) throws IOException;
    }

    static interface IOConsumer<T> {
        void consume(T t) throws IOException;
    }
}
