package com.icegreen.greenmail.util;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    public void testParseRejectsCrlf() {
        String nul = String.valueOf((char) 0);
        String crlf = "" + (char) 13 + (char) 10;
        assertThatThrownBy(() -> SaslMessage.parse(nul + "auth" + crlf + "cid" + nul + "passwd"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
