package com.icegreen.greenmail.test.util;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/* Utility class that extends MimeMessage forcing every message-id to contain characters that need to be properly escaped
 * Javamail by default will set the message-id when the MimeMessage is being processed. 
 * See http://www.oracle.com/technetwork/java/faq-135477.html#msgid for more details
 */
public class GreenMailMimeMessage extends MimeMessage {

    public GreenMailMimeMessage(Session session) throws MessagingException {
        super(session);
    }

    @Override
    protected void updateMessageID() throws MessagingException {
        String messageID = "<11111.22222.3333.JavaMail.\"foo.bar\\domain\"@localhost>";
        setHeader("Message-ID", messageID);
    }

}
