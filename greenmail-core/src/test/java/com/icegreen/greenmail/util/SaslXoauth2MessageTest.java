package com.icegreen.greenmail.util;

import org.junit.Test;

import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SaslXoauth2MessageTest {
    @Test
    public void testValidAuthenticationString() {
        String user = "testUser";
        String token = "sampleToken123";
        String rawInput = "user=" + user + "\u0001auth=Bearer " + token + "\u0001\u0001";
        String encodedInput = Base64.getEncoder().encodeToString(rawInput.getBytes());

        SaslXoauth2Message message = SaslXoauth2Message.parseBase64Encoded(encodedInput);
        assertEquals(user, message.getUsername());
        assertEquals(token, message.getAccessToken());
    }

    @Test
    public void testInvalidBase64Input() {
        assertThrows(IllegalArgumentException.class, () -> SaslXoauth2Message.parseBase64Encoded("invalid_base64_string"));
    }
}
