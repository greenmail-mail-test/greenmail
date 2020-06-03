package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.junit.Before;
import org.junit.Test;

import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SizeTerm;
import java.io.ByteArrayInputStream;
import java.nio.charset.CharacterCodingException;

import static org.junit.Assert.assertEquals;

public class SearchCommandParserTest {

    private SearchCommandParser parser;

    @Before
    public void beforeEachTest() {
        parser = new SearchCommandParser();
    }

    @Test
    public void testSmallerParseCommand() throws CharacterCodingException, ProtocolException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("SMALLER 5\n" .getBytes());
        ImapRequestLineReader reader = new ImapRequestLineReader(inputStream, null);

        SearchTerm expectedTerm = new SizeTerm(ComparisonTerm.LT, 5);

        SearchTerm searchTerm = parser.searchTerm(reader);

        assertEquals(expectedTerm, searchTerm);
    }

    @Test
    public void testLargerParseCommand() throws CharacterCodingException, ProtocolException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("LARGER 5\n" .getBytes());
        ImapRequestLineReader reader = new ImapRequestLineReader(inputStream, null);

        SearchTerm expectedTerm = new SizeTerm(ComparisonTerm.GT, 5);

        SearchTerm searchTerm = parser.searchTerm(reader);

        assertEquals(expectedTerm, searchTerm);
    }

    @Test
    public void testSmallerAndLargerParseCommand() throws CharacterCodingException, ProtocolException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("SMALLER 5 LARGER 3\n" .getBytes());
        ImapRequestLineReader reader = new ImapRequestLineReader(inputStream, null);

        SearchTerm expectedTerm = new AndTerm(new SizeTerm(ComparisonTerm.LT, 5), new SizeTerm(ComparisonTerm.GT, 3));

        SearchTerm searchTerm = parser.searchTerm(reader);

        assertEquals(expectedTerm, searchTerm);
    }
}
