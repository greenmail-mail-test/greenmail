package com.icegreen.greenmail.util;

import org.junit.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class MaxSizeLinkedHashMapTest {
    private static final int TEST_MAX_SIZE = 8;
    private MaxSizeLinkedHashMap<Integer, Integer> map = new MaxSizeLinkedHashMap<>(TEST_MAX_SIZE);

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeMaxSize() {
        new MaxSizeLinkedHashMap<>(-1 * Math.abs(new Random().nextInt(Integer.MAX_VALUE - 1) + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectZeroMaxSize() {
        new MaxSizeLinkedHashMap<>(0);
    }

    @Test
    public void shouldRejectLessThanZeroMaxSize() {
        try {
            new MaxSizeLinkedHashMap<>(-1);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("The maxSize must be greater than 0: -1");
        }
    }

    @Test
    public void shouldNotExceedMaxSize() {
        // When
        for (int i = 0; i < TEST_MAX_SIZE * 2; i++) {
            map.put(i, i);
        }

        // Then
        assertThat(map.size()).isEqualTo(TEST_MAX_SIZE);
    }
}