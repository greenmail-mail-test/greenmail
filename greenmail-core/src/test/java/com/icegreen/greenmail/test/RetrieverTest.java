package com.icegreen.greenmail.test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.Retriever;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

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
        new Retriever(greenMail.getImap());
        new Retriever(greenMail.getImaps());
        new Retriever(greenMail.getPop3());
        new Retriever(greenMail.getPop3s());
        try {
            new Retriever(greenMail.getSmtp());
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
        try {
            new Retriever(greenMail.getSmtps());
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
