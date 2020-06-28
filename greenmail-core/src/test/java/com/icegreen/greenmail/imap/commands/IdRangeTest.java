package com.icegreen.greenmail.imap.commands;

import java.util.List;

import org.junit.Test;

import static com.icegreen.greenmail.imap.commands.IdRange.VALUE_WILDCARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * See https://tools.ietf.org/html/rfc3501#page-53 regarding sequence-set
 */
public class IdRangeTest {
    @Test
    public void testParseRangeSequence() {
        String sequenceSetText = "1";
        assertTrue(IdRange.SEQUENCE.matcher(sequenceSetText).matches());
        List<IdRange> sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertEquals(1, sequenceSet.size());
        assertEquals(new IdRange(1), sequenceSet.get(0));

        sequenceSetText = "*";
        assertTrue(IdRange.SEQUENCE.matcher(sequenceSetText).matches());
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertEquals(1, sequenceSet.size());
        assertEquals(new IdRange(VALUE_WILDCARD), sequenceSet.get(0));

        sequenceSetText = "5:2"; // Order does not matter
        assertTrue(IdRange.SEQUENCE.matcher(sequenceSetText).matches());
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertEquals(1, sequenceSet.size());
        assertEquals(new IdRange(2,5), sequenceSet.get(0));

        sequenceSetText = "1,3";
        assertTrue(IdRange.SEQUENCE.matcher(sequenceSetText).matches());
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertEquals(2, sequenceSet.size());
        assertEquals(new IdRange(1), sequenceSet.get(0));
        assertEquals(new IdRange(3), sequenceSet.get(1));

        sequenceSetText = "*:4,8";
        assertTrue(IdRange.SEQUENCE.matcher(sequenceSetText).matches());
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertEquals(2, sequenceSet.size());
        assertEquals(new IdRange(4, VALUE_WILDCARD), sequenceSet.get(0));
        assertEquals(new IdRange(8), sequenceSet.get(1));

        sequenceSetText ="1,4:*";
        assertTrue(IdRange.SEQUENCE.matcher(sequenceSetText).matches());
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertEquals(2, sequenceSet.size());
        assertEquals(new IdRange(1), sequenceSet.get(0));
        assertEquals(new IdRange(4, VALUE_WILDCARD), sequenceSet.get(1));

        sequenceSetText ="1,2:4,8";
        assertTrue(IdRange.SEQUENCE.matcher(sequenceSetText).matches());
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertEquals(3, sequenceSet.size());
        assertEquals(new IdRange(1), sequenceSet.get(0));
        assertEquals(new IdRange(2,4), sequenceSet.get(1));
        assertEquals(new IdRange(8), sequenceSet.get(2));
    }
}
