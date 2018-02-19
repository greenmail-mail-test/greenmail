package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.commands.parsers.search.SearchTermBuilder;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
                assertTrue("Expected match for uidseq=" + d.uidSeq + " and uid=" + uidMatch, term.match(uidMatch));
            }
            for (long uidNoMatch : d.uidNotMatching) {
                assertFalse("Expected no match for uidseq=" + d.uidSeq + " and uid=" + uidNoMatch, term.match(uidNoMatch));
            }
        }
    }
}
