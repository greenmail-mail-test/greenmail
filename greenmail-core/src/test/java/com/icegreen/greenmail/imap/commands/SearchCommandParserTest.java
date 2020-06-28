package com.icegreen.greenmail.imap.commands;

import java.io.ByteArrayInputStream;
import java.nio.charset.CharacterCodingException;
import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.*;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchCommandParserTest {
    @Test
    public void testSmallerParseCommand() throws CharacterCodingException, ProtocolException {
        SearchTerm expectedTerm = new SizeTerm(ComparisonTerm.LT, 5);
        SearchTerm searchTerm = parse("SMALLER 5");

        assertEquals(expectedTerm, searchTerm);
    }

    @Test
    public void testLargerParseCommand() throws CharacterCodingException, ProtocolException {
        SearchTerm expectedTerm = new SizeTerm(ComparisonTerm.GT, 5);
        SearchTerm searchTerm = parse("LARGER 5");

        assertEquals(expectedTerm, searchTerm);
    }

    @Test
    public void testSmallerAndLargerParseCommand() throws CharacterCodingException, ProtocolException {
        SearchTerm expectedTerm = new AndTerm(new SizeTerm(ComparisonTerm.LT, 5), new SizeTerm(ComparisonTerm.GT, 3));
        SearchTerm searchTerm = parse("SMALLER 5 LARGER 3");

        assertEquals(expectedTerm, searchTerm);
    }

    @Test
    public void testAndSubjectOrToFrom() throws CharacterCodingException, ProtocolException, AddressException {
        SearchTerm expectedTerm = new AndTerm(new SearchTerm[]{
                new SubjectTerm("Greenmail"),
                new OrTerm(
                        new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to@localhost")),
                        new FromTerm(new InternetAddress("from@localhost"))
                ),
                SearchTermBuilder.create(SearchKey.ALL).build()});

        // SUBJECT Greenmail (OR TO from@localhost FROM from@localhost) ALL
        SearchTerm searchTerm = parse("SUBJECT Greenmail OR TO to@localhost FROM from@localhost ALL");

        assertEquals(expectedTerm, searchTerm);
    }

    private SearchTerm parse(String line) throws CharacterCodingException, ProtocolException {
        final byte[] bytes = (line.endsWith("\n") ? line : (line + '\n')).getBytes();
        ByteArrayInputStream ins = new ByteArrayInputStream(bytes);
        return new SearchCommandParser().searchTerm(new ImapRequestLineReader(ins, null));
    }
}
