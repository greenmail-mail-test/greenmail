package com.icegreen.greenmail.imap.commands;

import java.util.List;

import org.junit.Test;

import static com.icegreen.greenmail.imap.commands.IdRange.VALUE_WILDCARD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * See https://tools.ietf.org/html/rfc3501#page-53 regarding sequence-set
 */
public class IdRangeTest {
    @Test
    public void testParseRangeSequence() {
        String sequenceSetText = "1";
        assertThat(IdRange.SEQUENCE.matcher(sequenceSetText).matches()).isTrue();
        List<IdRange> sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertThat(sequenceSet).hasSize(1);
        assertThat(sequenceSet.get(0)).isEqualTo(new IdRange(1));

        sequenceSetText = "*";
        assertThat(IdRange.SEQUENCE.matcher(sequenceSetText).matches()).isTrue();
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertThat(sequenceSet).hasSize(1);
        assertThat(sequenceSet.get(0)).isEqualTo(new IdRange(VALUE_WILDCARD));

        sequenceSetText = "5:2"; // Order does not matter
        assertThat(IdRange.SEQUENCE.matcher(sequenceSetText).matches()).isTrue();
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertThat(sequenceSet).hasSize(1);
        assertThat(new IdRange(2,5)).isEqualTo(sequenceSet.get(0));

        sequenceSetText = "1,3";
        assertThat(IdRange.SEQUENCE.matcher(sequenceSetText).matches()).isTrue();
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertThat(sequenceSet).hasSize(2);
        assertThat(sequenceSet.get(0)).isEqualTo(new IdRange(1));
        assertThat(sequenceSet.get(1)).isEqualTo(new IdRange(3));

        sequenceSetText = "*:4,8";
        assertThat(IdRange.SEQUENCE.matcher(sequenceSetText).matches()).isTrue();
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertThat(sequenceSet).hasSize(2);
        assertThat(new IdRange(4, VALUE_WILDCARD)).isEqualTo(sequenceSet.get(0));
        assertThat(sequenceSet.get(1)).isEqualTo(new IdRange(8));

        sequenceSetText ="1,4:*";
        assertThat(IdRange.SEQUENCE.matcher(sequenceSetText).matches()).isTrue();
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertThat(sequenceSet).hasSize(2);
        assertThat(sequenceSet.get(0)).isEqualTo(new IdRange(1));
        assertThat(new IdRange(4, VALUE_WILDCARD)).isEqualTo(sequenceSet.get(1));

        sequenceSetText ="1,2:4,8";
        assertThat(IdRange.SEQUENCE.matcher(sequenceSetText).matches()).isTrue();
        sequenceSet = IdRange.parseRangeSequence(sequenceSetText);
        assertThat(sequenceSet).hasSize(3);
        assertThat(sequenceSet.get(0)).isEqualTo(new IdRange(1));
        assertThat(new IdRange(2, 4)).isEqualTo(sequenceSet.get(1));
        assertThat(sequenceSet.get(2)).isEqualTo(new IdRange(8));
    }
}
