package org.tinyradius.netty;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * RadiusQueue class
 */
public class RadiusQueue<T extends Comparable<T>> {

    private final List<Set<T>> q;

    /**
     * @param initialCapacity
     * @param hashSize
     * @param comparator
     */
    public RadiusQueue(int initialCapacity, int hashSize, Comparator<T> comparator) {
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        if (hashSize < 1)
            throw new IllegalArgumentException();
        if (comparator == null)
            throw new NullPointerException("comparator cannot be null");
        q = new ArrayList<>(1 << hashSize);
        for (int i = 0; i < 1 << hashSize; i++)
            q.add(new ConcurrentSkipListSet<>(comparator));
    }

    /**
     * @param initialCapacity the initial capacity for this queue
     * @param hashSize
     * @throws IllegalArgumentException if initialCapacity is less than 1
     */
    public RadiusQueue(int initialCapacity, int hashSize) {
        this(initialCapacity, hashSize, new SimpleRadiusQueueComparator<>());
    }

    /**
     * @param initialCapacity the initial capacity for this queue
     * @throws IllegalArgumentException if initialCapacity is less than 1
     */
    public RadiusQueue(int initialCapacity) {
        this(initialCapacity, 8);
    }

    /**
     *
     */
    public RadiusQueue() {
        this(1024);
    }

    /**
     * @param value
     * @param hash
     * @return
     */
    public T add(T value, int hash) {
        if (value == null)
            throw new NullPointerException("value cannot be null");
        int mask = q.size() - 1;
        Set<T> set = q.get(hash & mask);
        set.add(value);
        return value;
    }

    /**
     * @param value
     * @return
     */
    public boolean remove(T value, int hash) {
        if (value == null)
            throw new NullPointerException("value cannot be null");
        int mask = q.size() - 1;
        Set<T> set = q.get(hash & mask);
        return set.remove(value);
    }

    /**
     * @param hash
     * @return
     */
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