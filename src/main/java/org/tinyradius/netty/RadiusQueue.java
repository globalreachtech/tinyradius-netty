package org.tinyradius.netty;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RadiusQueue class
 */
public class RadiusQueue<T> {

    private Set<?>[] q;

    /**
     *
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
        q = new Set<?>[1 << hashSize];
        for (int i = 0; i < q.length; i++)
            q[i] = ConcurrentHashMap.newKeySet();
    }

    /**
     * @param initialCapacity the initial capacity for this queue
     * @param hashSize
     * @throws IllegalArgumentException if initialCapacity is less than 1
     */
    public RadiusQueue(int initialCapacity, int hashSize) {
        this(initialCapacity, hashSize, new SimpleRadiusQueueComparator<T>());
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
    @SuppressWarnings("unchecked")
    public T add(T value, int hash) {
        if (value == null)
            throw new NullPointerException("value cannot be null");
        int mask = q.length - 1;
        Set<T> set = (Set<T>)q[hash & mask];
        set.add(value);
        return value;
    }

    /**
     * @param value
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean remove(T value, int hash) {
        if (value == null)
            throw new NullPointerException("value cannot be null");
        int mask = q.length - 1;
        Set<T> set = (Set<T>) q[hash & mask];
        return set.remove(value);
    }

    /**
     *
     * @param hash
     * @return
     */
    @SuppressWarnings("unchecked")
    public Set<T> get(int hash) {
        int mask = q.length - 1;
        return Collections.unmodifiableSet((Set<T>)q[hash & mask]);
    }

    @SuppressWarnings("unchecked")
    private static class SimpleRadiusQueueComparator<T>
            implements Comparator<T> {
        public int compare(T o1, T o2) {
            if (o1 == null)
                return -1;
            if (o2 == null)
                return 1;

            if (!(o1 instanceof Comparable))
                throw new IllegalArgumentException();
            if (!(o2 instanceof Comparable))
                throw new IllegalArgumentException();

            return ((Comparable)o1).compareTo(o2);
        }
    }

}