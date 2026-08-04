package com.icegreen.greenmail.store;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SimpleMessageAttributes} computes RFC822.SIZE and
 * BODYSTRUCTURE lineCount from the <em>full</em> MIME part bytes (headers +
 * blank line + body), matching the requirements of RFC 2045 §6.7 and
 * RFC 3501 §7.4.2.
 */
public class SizeAndLineCountTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static SimpleMessageAttributes attributesFor(String rawAscii) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session,
                new ByteArrayInputStream(rawAscii.getBytes(StandardCharsets.US_ASCII)));
        return new SimpleMessageAttributes(msg, new Date());
    }

    /**
     * Parses the last integer token out of the BODYSTRUCTURE body-fields block
     * for a simple (non-multipart) message.  In the IMAP BODYSTRUCTURE syntax
     * for a text/* part the trailing integer before the closing ')' is the
     * lineCount field.
     * <p>
     * e.g. ("text" "plain" NIL NIL NIL "7BIT" 42 3)
     *                                           ^^ size  ^ lineCount
     */
    private static int extractLineCount(String bodyStructure) {
        // Strip outer parens, split on whitespace, last token is lineCount.
        String inner = bodyStructure.trim();
        if (inner.startsWith("(")) inner = inner.substring(1);
        if (inner.endsWith(")")) inner = inner.substring(0, inner.length() - 1);
        String[] tokens = inner.trim().split("\\s+");
        return Integer.parseInt(tokens[tokens.length - 1]);
    }

    /**
     * Counts the number of '\n' bytes in the given raw ASCII string exactly as
     * the implementation does, so expected values are self-consistent with the
     * actual wire representation produced by {@code MimeMessage.writeTo()}.
     */
    private static int countLf(String raw) {
        int n = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '\n') n++;
        }
        return n;
    }

    // -----------------------------------------------------------------------
    // RFC822.SIZE tests
    // -----------------------------------------------------------------------

    @Test
    public void sizeMatchesFullPartByteCount() throws Exception {
        // Build the raw message exactly as it will arrive on the wire.
        String raw =
                "From: sender@example.com\r\n" +
                "Subject: size test\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "Hello World\r\n";

        SimpleMessageAttributes attrs = attributesFor(raw);

        // The expected size is the byte count of what writeTo() produces, which
        // mirrors exactly what the implementation measures.
        Session session = Session.getInstance(new Properties());
        MimeMessage reference = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reference.writeTo(baos);
        int expectedSize = baos.size();

        assertThat(attrs.getSize())
                .as("RFC822.SIZE must equal the byte count of the serialised part")
                .isEqualTo(expectedSize);
    }

    @Test
    public void sizeMustIncludeHeaders() throws Exception {
        // Body alone is "Hi\r\n" = 4 bytes. A correct implementation must
        // return significantly more because headers are included.
        String raw =
                "From: a@b.com\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "Hi\r\n";

        int size = attributesFor(raw).getSize();

        assertThat(size)
                .as("RFC822.SIZE must cover headers, not just the body")
                .isGreaterThan(4);
    }

    // -----------------------------------------------------------------------
    // lineCount tests – surfaced via BODYSTRUCTURE
    // -----------------------------------------------------------------------

    @Test
    public void lineCountForPlainTextIncludesHeaderLines() throws Exception {
        // A message whose body is a single line: if lineCount were body-only it
        // would be 1.  With headers it must be higher.
        String raw =
                "From: a@b.com\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "one body line\r\n";

        SimpleMessageAttributes attrs = attributesFor(raw);
        String bs = attrs.getBodyStructure(false);
        int lineCount = extractLineCount(bs);

        // Expected: all '\n' in the full serialised part, i.e. at least the
        // two header lines + blank separator + body line = 4.
        assertThat(lineCount)
                .as("lineCount must reflect the full part (headers + body), not just the body")
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    public void lineCountMatchesNewlinesInFullPart() throws Exception {
        // Construct a message with a known, fixed body so we can predict the
        // exact lineCount value by counting '\n' in the raw bytes.
        String raw =
                "From: sender@example.com\r\n" +
                "Subject: lc test\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "line one\r\n" +
                "line two\r\n" +
                "line three\r\n";

        // Re-serialise through writeTo() to get the canonical bytes (jakarta
        // mail may reorder or fold headers).
        Session session = Session.getInstance(new Properties());
        MimeMessage reference = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reference.writeTo(baos);
        int expectedLineCount = countLf(baos.toString(StandardCharsets.US_ASCII.name()));

        SimpleMessageAttributes attrs = attributesFor(raw);
        String bs = attrs.getBodyStructure(false);
        int lineCount = extractLineCount(bs);

        assertThat(lineCount)
                .as("BODYSTRUCTURE lineCount must equal the number of \\n bytes in the full serialised part")
                .isEqualTo(expectedLineCount);
    }

    @Test
    public void lineCountIsNotBodyOnlyCount() throws Exception {
        // If lineCount were body-only, a 1-line body would yield lineCount == 1.
        // With headers present, the true value must be strictly greater.
        String raw =
                "From: a@b.com\r\n" +
                "To: b@c.com\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                "single line body\r\n";

        String bs = attributesFor(raw).getBodyStructure(false);
        int lineCount = extractLineCount(bs);

        assertThat(lineCount)
                .as("lineCount must be > 1 because headers add lines beyond the single body line")
                .isGreaterThan(1);
    }

    // -----------------------------------------------------------------------
    // Non-text parts – lineCount not emitted in BODYSTRUCTURE, but size still
    // applies.
    // -----------------------------------------------------------------------

    @Test
    public void sizeIsCorrectForNonTextPart() throws Exception {
        String raw =
                "From: a@b.com\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "\r\n" +
                "binarydata\r\n";

        Session session = Session.getInstance(new Properties());
        MimeMessage reference = new MimeMessage(session,
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.US_ASCII)));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reference.writeTo(baos);
        int expectedSize = baos.size();

        assertThat(attributesFor(raw).getSize())
                .as("RFC822.SIZE must be correct for non-text parts too")
                .isEqualTo(expectedSize);
    }
}
