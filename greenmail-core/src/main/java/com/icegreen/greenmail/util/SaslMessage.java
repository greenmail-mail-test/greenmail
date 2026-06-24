package com.icegreen.greenmail.util;

/**
 * SASL PLAIN mechanism message.
 * <p>
 * See <a href="https://tools.ietf.org/html/rfc4616">PLAIN</a>
 */
public class SaslMessage {
    private final String authzid;
    private final String authcid;
    private final String passwd;

    private SaslMessage(String authzid, String authcid, String passwd) {
        this.authzid = authzid;
        this.authcid = authcid;
        this.passwd = passwd;
    }

    public String getAuthzid() {
        return authzid;
    }

    public String getAuthcid() {
        return authcid;
    }

    public String getPasswd() {
        return passwd;
    }

    /**
     * Parses a SASL mechanism message.
     * <p>
     * message   = [authzid] UTF8NUL authcid UTF8NUL passwd
     * authcid   = 1*SAFE ; MUST accept up to 255 octets
     * authzid   = 1*SAFE ; MUST accept up to 255 octets
     * passwd    = 1*SAFE ; MUST accept up to 255 octets
     * UTF8NUL   = %x00 ; UTF-8 encoded NUL character
     * <p>
     * SAFE      = UTF1 / UTF2 / UTF3 / UTF4
     * ;; any UTF-8 encoded Unicode character except NUL
     *
     * @param message the SASL message
     */
    public static SaslMessage parse(String message) {
        // The decoded authzid/authcid/passwd are echoed back verbatim in line-oriented
        // POP3/SMTP error responses (e.g. the POP3 "-ERR Authentication failed: User <...>"
        // line), so a field carrying CR or LF would forge additional response lines. The
        // base64 transport lets such octets through even though the command line itself is
        // CRLF-terminated, so reject them at the parser.
        if (containsLineBreak(message)) {
            throw new IllegalArgumentException("Invalid SASL message: CR and LF are not allowed");
        }
        String[] parts = message.split("\u0000");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Expected authorization-id\\0authentication-id\\0passwd but got only " + parts.length + " parts : " + message);
        }
        return new SaslMessage(parts[0], parts[1], parts[2]);
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf((char) 13) >= 0 || value.indexOf((char) 10) >= 0;
    }
}
