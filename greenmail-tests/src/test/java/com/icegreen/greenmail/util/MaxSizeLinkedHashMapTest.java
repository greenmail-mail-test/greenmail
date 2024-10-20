package com.icegreen.greenmail.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Random;

class MaxSizeLinkedHashMapTest {
    private static final int TEST_MAX_SIZE = 8;
    private final MaxSizeLinkedHashMap<Integer, Integer> map = new MaxSizeLinkedHashMap<>(TEST_MAX_SIZE);

    @Test
    void shouldRejectNegativeMaxSize() {
        int maxSize =  -1 * Math.abs(new Random().nextInt(Integer.MAX_VALUE - 1) + 1);
        assertThatThrownBy(() -> new MaxSizeLinkedHashMap<>(maxSize)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectZeroMaxSize() {
        assertThatThrownBy(() -> new MaxSizeLinkedHashMap<>(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectLessThanZeroMaxSize() {
        try {
            new MaxSizeLinkedHashMap<>(-1);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("The maxSize must be greater than 0: -1");
        }
    }

    @Test
    void shouldNotExceedMaxSize() {
        // When
        for (int i = 0; i < TEST_MAX_SIZE * 2; i++) {
            map.put(i, i);
        }

        // Then
        assertThat(map).hasSize(TEST_MAX_SIZE);
    }
}
