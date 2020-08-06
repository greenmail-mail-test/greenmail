package com.icegreen.greenmail.util;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SaslMessageTest {
    @Test
    public void testParse() {
        final SaslMessage saslMessage = SaslMessage.parse("authzid\u0000authcid\u0000passwd");
        assertThat(saslMessage.getAuthzid()).isEqualTo("authzid");
        assertThat(saslMessage.getAuthcid()).isEqualTo("authcid");
        assertThat(saslMessage.getPasswd()).isEqualTo("passwd");
    }

    @Test
    public void testParseWithouthAuthzid() {
        final SaslMessage saslMessage = SaslMessage.parse("\u0000authcid\u0000passwd");
        assertThat(saslMessage.getAuthzid()).isEmpty();
        assertThat(saslMessage.getAuthcid()).isEqualTo("authcid");
        assertThat(saslMessage.getPasswd()).isEqualTo("passwd");
    }
}
