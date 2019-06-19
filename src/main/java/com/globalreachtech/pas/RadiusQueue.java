package com.globalreachtech.pas;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * RadiusQueue class
 */
public class RadiusQueue<T> {

    private final List<Set<T>> q;
    private final int mask;

    public RadiusQueue(int hashSize) {
        if (hashSize < 1)
            throw new IllegalArgumentException();

        q = Stream.generate(ConcurrentHashMap::<T>newKeySet)
                .limit(1 << hashSize)
                .collect(Collectors.toList());
        mask = q.size() - 1;
    }

    public RadiusQueue() {
        this(8);
    }

    public T add(T value, int hash) {
        requireNonNull(value, "value cannot be null");

        Set<T> set = q.get(hash & mask);
        set.add(value);
        return value;
    }

    public boolean remove(T value, int hash) {
        requireNonNull(value, "value cannot be null");

        Set<T> set = q.get(hash & mask);
        return set.remove(value);
    }

    public Set<T> get(int hash) {
        int mask = q.size() - 1;
        return Collections.unmodifiableSet(q.get(hash & mask));
    }
}