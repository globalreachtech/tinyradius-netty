package org.tinyradius.netty;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * RadiusQueue class
 */
public class RadiusQueue<T extends Comparable<T>> {

    private final List<Set<T>> q;

    public RadiusQueue(int hashSize, Comparator<T> comparator) {
        if (hashSize < 1)
            throw new IllegalArgumentException();
        requireNonNull(comparator, "comparator cannot be null");

        q = Stream.generate(() -> new ConcurrentSkipListSet<>(comparator))
                .limit(1 << hashSize)
                .collect(Collectors.toList());
    }

    public RadiusQueue(int hashSize) {
        this( hashSize, new SimpleRadiusQueueComparator<>());
    }

    public RadiusQueue() {
        this(8);
    }

    public T add(T value, int hash) {
        requireNonNull(value, "value cannot be null");

        int mask = q.size() - 1;
        Set<T> set = q.get(hash & mask);
        set.add(value);
        return value;
    }

    public boolean remove(T value, int hash) {
        requireNonNull(value, "value cannot be null");

        int mask = q.size() - 1;
        Set<T> set = q.get(hash & mask);
        return set.remove(value);
    }

    public Set<T> get(int hash) {
        int mask = q.size() - 1;
        return Collections.unmodifiableSet(q.get(hash & mask));
    }

    private static class SimpleRadiusQueueComparator<T extends Comparable<T>> implements Comparator<T> {
        public int compare(T o1, T o2) {
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;

            return o1.compareTo(o2);
        }
    }

}