package com.icegreen.greenmail.store;

import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentDispositionEscapeTest {
    @Test
    public void bodyStructureEscapesDispositionType() throws Exception {
        String raw = "Content-Type: application/octet-stream\r\n" +
                "Content-Disposition: attachment\"injected\r\n" +
                "Content-Transfer-Encoding: base64\r\n" +
                "\r\n" +
                "AAEC\r\n";
        MimeMessage msg = GreenMailUtil.newMimeMessage(raw);
        SimpleMessageAttributes attrs = new SimpleMessageAttributes(msg, new Date());

        String bodyStructure = attrs.getBodyStructure(true);

        // The sender-controlled disposition type must not break the IMAP quoted string.
        assertThat(bodyStructure).contains("attachment\\\"injected");
    }
}
