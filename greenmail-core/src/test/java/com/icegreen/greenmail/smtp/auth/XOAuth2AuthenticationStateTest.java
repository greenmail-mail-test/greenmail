package com.icegreen.greenmail.smtp.auth;

import org.junit.Test;

import java.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class XOAuth2AuthenticationStateTest {
    @Test
    public void testValidAuthenticationString() {
        String user = "testUser";
        String token = "sampleToken123";
        String rawInput = "user=" + user + "\u0001auth=Bearer " + token + "\u0001\u0001";
        String encodedInput = Base64.getEncoder().encodeToString(rawInput.getBytes());

        XOAuth2AuthenticationState authState = new XOAuth2AuthenticationState(encodedInput);

        assertEquals(user, authState.getUsername());
        assertEquals(token, authState.getAccessToken());
    }

    @Test
    public void testInvalidBase64Input() {
        String invalidBase64 = "invalid_base64_string";

        assertThrows(IllegalArgumentException.class, () -> {
            new XOAuth2AuthenticationState(invalidBase64);
        });
    }

}
