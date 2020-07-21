package com.icegreen.greenmail.imap.commands;

import java.io.ByteArrayInputStream;
import java.nio.charset.CharacterCodingException;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.*;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchCommandParserTest {
    @Test
    public void testHeader() throws ProtocolException {
        SearchTerm expectedTerm = new AndTerm(
                new AndTerm(new SearchTerm[]{
                        new HeaderTerm("Message-ID", "<1627010197.0.1593681191102@[192.168.242.10]>"),
                        new FlagTerm(new Flags(Flags.Flag.SEEN), true)
                }),
                SearchTermBuilder.create(SearchKey.ALL).build()
        );

        SearchTerm searchTerm = parse("(HEADER Message-ID <1627010197.0.1593681191102@[192.168.242.10]> SEEN) ALL");

        assertThat(searchTerm).isEqualTo(expectedTerm);
    }

    @Test
    public void testSmallerParseCommand() throws ProtocolException {
        SearchTerm expectedTerm = new SizeTerm(ComparisonTerm.LT, 5);
        SearchTerm searchTerm = parse("SMALLER 5");

        assertThat(searchTerm).isEqualTo(expectedTerm);
    }

    @Test
    public void testLargerParseCommand() throws ProtocolException {
        SearchTerm expectedTerm = new SizeTerm(ComparisonTerm.GT, 5);
        SearchTerm searchTerm = parse("LARGER 5");

        assertThat(searchTerm).isEqualTo(expectedTerm);
    }

    @Test
    public void testSmallerAndLargerParseCommand() throws ProtocolException {
        SearchTerm expectedTerm = new AndTerm(new SizeTerm(ComparisonTerm.LT, 5), new SizeTerm(ComparisonTerm.GT, 3));
        SearchTerm searchTerm = parse("SMALLER 5 LARGER 3");

        assertThat(searchTerm).isEqualTo(expectedTerm);
    }

    @Test
    public void testAndSubjectOrToFrom() throws ProtocolException, AddressException {
        SearchTerm expectedTerm = new AndTerm(new SearchTerm[]{
                new SubjectTerm("Greenmail"),
                new OrTerm(
                        new RecipientTerm(Message.RecipientType.TO, new InternetAddress("to@localhost")),
                        new FromTerm(new InternetAddress("from@localhost"))
                ),
                SearchTermBuilder.create(SearchKey.ALL).build()});

        // SUBJECT Greenmail (OR TO from@localhost FROM from@localhost) ALL
        SearchTerm searchTerm = parse("SUBJECT Greenmail OR TO to@localhost FROM from@localhost ALL");

        assertThat(searchTerm).isEqualTo(expectedTerm);
    }

    private SearchTerm parse(String line) throws ProtocolException {
        final byte[] bytes = (line.endsWith("\n") ? line : (line + '\n')).getBytes();
        ByteArrayInputStream ins = new ByteArrayInputStream(bytes);
        return new SearchCommandParser().searchTerm(new ImapRequestLineReader(ins, null));
    }
}
