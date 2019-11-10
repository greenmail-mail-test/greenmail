/* -------------------------------------------------------------------
* This software is released under the Apache license 2.0
* -------------------------------------------------------------------
*/
package com.icegreen.greenmail.util;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * A {@link LinkedHashMap} with a maximum size. Adding an {@link java.util.Map.Entry Entry} that would exceed this size
 * results in {@link #removeEldestEntry(Map.Entry) removing the eldest entry}.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author Raimund Klein <raimund.klein@gmx.de>
 */
public class MaxSizeLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    static final long serialVersionUID = 7736880402696612355L;

    private final int maxSize;

    /**
     * Constructs a MaxSizeLinkedHashMap with maximum size {@code maxSize}.
     *
     * @param maxSize The maximum size for this MaxSizeLinkedHashMap.
     * @throws IllegalArgumentException if {@code maxSize} is not positive.
     */
    public MaxSizeLinkedHashMap(final int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException(format("The maxSize must be greater than 0: %d", maxSize));
        }
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
