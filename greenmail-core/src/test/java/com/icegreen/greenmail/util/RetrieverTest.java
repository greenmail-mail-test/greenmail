package com.icegreen.greenmail.util;

import org.junit.Test;

public class RetrieverTest {
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullArg() {
        new Retriever(null);
    }
}
