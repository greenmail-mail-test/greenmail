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

    @Test
    public void consumeLiteralParsesRegularContent() throws ProtocolException {
        assertThat(consumeLiteral("{5+}\r\nhello")).isEqualTo("hello");
        assertThat(consumeLiteral("{0+}\r\n")).isEmpty();
    }

    @Test
    public void consumeLiteralReadsPayloadLargerThanOneChunk() throws ProtocolException {
        StringBuilder payload = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            payload.append((char) ('a' + i % 26));
        }
        assertThat(consumeLiteral("{" + payload.length() + "+}\r\n" + payload))
            .isEqualTo(payload.toString());
    }

    @Test
    public void consumeLiteralRejectsMissingOctetCount() {
        assertThatThrownBy(() -> consumeLiteral("{+}\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    @Test
    public void consumeLiteralRejectsNegativeOctetCount() {
        assertThatThrownBy(() -> consumeLiteral("{-1+}\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    @Test
    public void consumeLiteralRejectsNonNumericOctetCount() {
        assertThatThrownBy(() -> consumeLiteral("{1a+}\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    @Test
    public void consumeLiteralRejectsOutOfRangeOctetCount() {
        assertThatThrownBy(() -> consumeLiteral("{9999999999+}\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    /**
     * An announced octet count must not size a buffer on its own: the reader has to fail on the
     * truncated payload rather than reserve the announced amount of memory up front.
     */
    @Test
    public void consumeLiteralDoesNotReserveMemoryForAnAnnouncedPayload() {
        long usedBefore = usedMemory();
        assertThatThrownBy(() -> consumeLiteral("{1000000000+}\r\n"))
            .isInstanceOf(ProtocolException.class);
        assertThat(usedMemory() - usedBefore).isLessThan(100L * 1024 * 1024);
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    @Test
    public void consumeLongRejectsMissingNumber() {
        assertThatThrownBy(() -> consumeLong(">\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    @Test
    public void consumeLongRejectsOutOfRangeNumber() {
        assertThatThrownBy(() -> consumeLong("99999999999999999999\r\n"))
            .isInstanceOf(ProtocolException.class);
    }

    private static String consumeLiteral(String line) throws ProtocolException {
        ByteArrayInputStream in = new ByteArrayInputStream(line.getBytes(StandardCharsets.ISO_8859_1));
        return new CommandParser().consumeLiteral(new ImapRequestLineReader(in, null));
    }

    private static long consumeLong(String line) throws ProtocolException {
        ByteArrayInputStream in = new ByteArrayInputStream(line.getBytes(StandardCharsets.ISO_8859_1));
        return new CommandParser().consumeLong(new ImapRequestLineReader(in, null));
    }
}
