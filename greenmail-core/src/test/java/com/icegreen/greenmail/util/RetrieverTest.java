package com.icegreen.greenmail.util;

import com.icegreen.greenmail.junit.GreenMailRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.fail;

public class RetrieverTest {
    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(new ServerSetup[]{
            ServerSetupTest.IMAP,
            ServerSetupTest.IMAPS,
            ServerSetupTest.SMTP,
            ServerSetupTest.SMTPS,
            ServerSetupTest.POP3,
            ServerSetupTest.POP3S
    });

    @Test
    public void testCreateRetriever() {
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

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullArg() {
        new Retriever(null);
    }
}
