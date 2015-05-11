package com.icegreen.greenmail.util;

import org.apache.commons.lang3.Validate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link LinkedHashMap} with a maximum size. Adding an {@link java.util.Map.Entry Entry} that would exceed this size
 * results in {@link #removeEldestEntry(Map.Entry) removing the eldest entry}.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class MaxSizeLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    /**
     * Constructs a MaxSizeLinkedHashMap with maximum size {@code maxSize}.
     *
     * @param maxSize The maximum size for this MaxSizeLinkedHashMap.
     * @throws IllegalArgumentException if {@code maxSize} is not positive.
     */
    public MaxSizeLinkedHashMap(final int maxSize) {
        Validate.isTrue(maxSize > 0, "The maxSize must be greater than 0: $d", maxSize);
        this.maxSize = maxSize;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation will return {@code true} if this MaxSizeLinkedHashMap has reached its maximum size, else {@code false}.
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
