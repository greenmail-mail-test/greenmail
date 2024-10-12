package com.icegreen.greenmail.util;


import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaslMessageTest {
    @Test
    void testParse() {
        final SaslMessage saslMessage = SaslMessage.parse("authzid\u0000authcid\u0000passwd");
        assertThat(saslMessage.getAuthzid()).isEqualTo("authzid");
        assertThat(saslMessage.getAuthcid()).isEqualTo("authcid");
        assertThat(saslMessage.getPasswd()).isEqualTo("passwd");
    }

    @Test
    void testParseWithouthAuthzid() {
        final SaslMessage saslMessage = SaslMessage.parse("\u0000authcid\u0000passwd");
        assertThat(saslMessage.getAuthzid()).isEmpty();
        assertThat(saslMessage.getAuthcid()).isEqualTo("authcid");
        assertThat(saslMessage.getPasswd()).isEqualTo("passwd");
    }
}
