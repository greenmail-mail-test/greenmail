package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CommandParserTest {
    @Test
    public void test() {
        assertThat(CommandParser.isCrOrLf('\n')).isTrue();
        assertThat(CommandParser.isCrOrLf('\r')).isTrue();
        assertThat(CommandParser.isCrOrLf('\t')).isFalse();
    }

    @Test
    public void consumeQuotedParsesRegularContent() throws ProtocolException {
        assertThat(consumeQuoted("\"hello world\"\r\n")).isEqualTo("hello world");
        assertThat(consumeQuoted("\"a\\\"b\"\r\n")).isEqualTo("a\"b");
    }

    @Test
    public void consumeQuotedRejectsEmbeddedCr() {
        assertThatThrownBy(() -> consumeQuoted("\"a\rb\"\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    @Test
    public void consumeQuotedRejectsEmbeddedLf() {
        assertThatThrownBy(() -> consumeQuoted("\"INBOX\r\n* 9 EXISTS\"\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    private static String consumeQuoted(String line) throws ProtocolException {
        ByteArrayInputStream in = new ByteArrayInputStream(line.getBytes(StandardCharsets.ISO_8859_1));
        return new CommandParser().consumeQuoted(new ImapRequestLineReader(in, null));
    }
}
