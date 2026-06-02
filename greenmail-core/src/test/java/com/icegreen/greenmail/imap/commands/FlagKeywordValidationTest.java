package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.store.MessageFlags;
import jakarta.mail.Flags;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FlagKeywordValidationTest {

    private Flags flagList(String input) throws ProtocolException {
        ImapRequestLineReader reader = new ImapRequestLineReader(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII)),
                new ByteArrayOutputStream());
        return new CommandParser().flagList(reader);
    }

    @Test
    public void acceptsValidKeyword() throws ProtocolException {
        Flags flags = flagList("(\\Seen foobar)\r\n");
        assertThat(flags.contains(Flags.Flag.SEEN)).isTrue();
        assertThat(flags.getUserFlags()).containsExactly("foobar");
        // Echoed value stays inside the parenthesized flag list.
        assertThat(MessageFlags.format(flags)).doesNotContain("\"");
    }

    @Test
    public void rejectsKeywordWithQuotedSpecial() {
        assertThatThrownBy(() -> flagList("(\\Seen ab\"cd)\r\n"))
                .isInstanceOf(ProtocolException.class);
    }

    @Test
    public void rejectsKeywordWithBackslash() {
        assertThatThrownBy(() -> flagList("(a\\b)\r\n"))
                .isInstanceOf(ProtocolException.class);
    }

    @Test
    public void rejectsKeywordWithListClose() {
        assertThatThrownBy(() -> flagList("(foo]bar)\r\n"))
                .isInstanceOf(ProtocolException.class);
    }
}
