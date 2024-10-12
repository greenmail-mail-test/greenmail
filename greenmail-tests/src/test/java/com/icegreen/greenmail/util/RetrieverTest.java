package com.icegreen.greenmail.util;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.junit5.GreenMailExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.fail;

class RetrieverTest {
    @RegisterExtension
    public static final GreenMailExtension greenMail = new GreenMailExtension(new ServerSetup[]{
            ServerSetupTest.IMAP,
            ServerSetupTest.IMAPS,
            ServerSetupTest.SMTP,
            ServerSetupTest.SMTPS,
            ServerSetupTest.POP3,
            ServerSetupTest.POP3S
    });

    @Test
    void testCreateRetriever() {
        try(Retriever r = new Retriever(greenMail.getImap())) {}
        try(Retriever r = new Retriever(greenMail.getImaps())) {}
        try(Retriever r = new Retriever(greenMail.getPop3())) {}
        try(Retriever r = new Retriever(greenMail.getPop3s())) {}
        try(Retriever r = new Retriever(greenMail.getSmtp())) {
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try(Retriever r = new Retriever(greenMail.getSmtps())) {
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    void testConstructorWithNullArg() {
        try( Retriever retriever = new Retriever(null)) {
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
