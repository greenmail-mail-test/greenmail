package com.icegreen.greenmail.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Helper for handling encodings.
 */
public class EncodingUtil {
    /**
     * Constant for 8-Bit encoding, which can be resembled by {@value #EIGHT_BIT_ENCODING}
     */
    public static final String EIGHT_BIT_ENCODING = "ISO-8859-1";
    /**
     * Predefined Charset for 8-Bit encoding.
     */
    public static final Charset CHARSET_EIGHT_BIT_ENCODING = StandardCharsets.ISO_8859_1;

    private EncodingUtil() {
        // No instantiation.
    }

    /**
     * Converts the string of given content to an input stream.
     *
     * @param content the string content.
     * @param charset the charset for conversion.
     * @return the stream (should be closed by invoker).
     */
    public static InputStream toStream(String content, Charset charset) {
        byte[] bytes = content.getBytes(charset);
        return new ByteArrayInputStream(bytes);
    }

    public static String toString(InputStream is, Charset charset) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = is.read(buffer)) != -1) {
                data.write(buffer, 0, length);
            }
            return data.toString(charset.name());
        } catch (IOException e) {
            throw new IllegalStateException("Can not convert stream to string of charset " + charset, e);
        }
    }

    /**
     * Decodes the base64 encoded string to a string.
     *
     * @param encoded the base64 encoded value
     * @return a string of the decoded value (UTF-8)
     */
    public static String decodeBase64(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
