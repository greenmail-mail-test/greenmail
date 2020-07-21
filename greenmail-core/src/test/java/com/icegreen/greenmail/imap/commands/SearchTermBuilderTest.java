package com.icegreen.greenmail.imap.commands;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class SearchTermBuilderTest {
    static class Data {
        String uidSeq;
        long[] uidMatching;
        long[] uidNotMatching;

        public Data(String uidSeq, long[] uidMatching, long[] uidNotMatching) {
            this.uidSeq = uidSeq;
            this.uidMatching = uidMatching;
            this.uidNotMatching = uidNotMatching;
        }
    }

    @Test
    public void testUidSearchTerm() {
        Data[] data = new Data[]{
                new Data("1", new long[]{1L}, new long[]{0L, 2L}),
                new Data("1:*", new long[]{1L, 2L}, new long[]{0L}),
                new Data("4:*", new long[]{5L, 10L}, new long[]{3L})
        };
        for (Data d : data) {
            List<IdRange> uidSet = IdRange.parseRangeSequence(d.uidSeq);
            SearchTermBuilder.UidSearchTerm term = new SearchTermBuilder.UidSearchTerm(uidSet);
            for (long uidMatch : d.uidMatching) {
                assertThat(term.match(uidMatch)).as("Expected match for uidseq=" + d.uidSeq + " and uid=" + uidMatch).isTrue();
            }
            for (long uidNoMatch : d.uidNotMatching) {
                assertThat(term.match(uidNoMatch)).as("Expected no match for uidseq=" + d.uidSeq + " and uid=" + uidNoMatch).isFalse();
            }
        }
    }
}
