package com.icegreen.greenmail.util;

import org.junit.Test;

import java.util.Random;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MaxSizeLinkedHashMapTest {
    private static final int TEST_MAX_SIZE = 8;
    private MaxSizeLinkedHashMap<Integer, Integer> map = new MaxSizeLinkedHashMap<Integer, Integer>(TEST_MAX_SIZE);

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeMaxSize() {
        new MaxSizeLinkedHashMap<Object, Object>(-1 * Math.abs(new Random().nextInt(Integer.MAX_VALUE - 1) + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectZeroMaxSize() {
        new MaxSizeLinkedHashMap<Object, Object>(0);
    }

    @Test
    public void shouldNotExceedMaxSize() {
        // When
        for (int i = 0; i < TEST_MAX_SIZE * 2; i++) {
            map.put(i, i);
        }

        // Then
        assertThat(map.size(), is(equalTo(TEST_MAX_SIZE)));
    }
}