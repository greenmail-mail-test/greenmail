package com.icegreen.greenmail;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SendReceiveWithInternationalAddressTest {

    static final Properties properties;

    static {

        properties = new Properties();
        properties.put("mail.mime.address.strict", Boolean.FALSE.toString());
    }

    @RegisterExtension
    static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP);

    @Test
    void testSend() throws MessagingException, IOException {
        Session session = GreenMailUtil.getSession(ServerSetupTest.SMTP, properties);
        MimeMessage mimeMessage = new MockInternationalizedMimeMessage(session);
        mimeMessage.setSubject("subject");
        mimeMessage.setSentDate(new Date());
        mimeMessage.setFrom("múchätįldé@tìldę.oœ");
        mimeMessage.setRecipients(Message.RecipientType.TO, "用户@例子.广告");
        mimeMessage.setRecipients(Message.RecipientType.CC, "θσερεχα@μπλε.ψομ");
        mimeMessage.setRecipients(Message.RecipientType.BCC, "राममो@हन.ईन्फो");

        // The body text needs to be encoded if it contains non us-ascii characters
        mimeMessage.setText("用户@例子","UTF-8");

        GreenMailUtil.sendMimeMessage(mimeMessage);

        // Decoding the body text to verify equality
        final MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getContentType()).isEqualTo("text/plain; charset=UTF-8");
        assertThat(receivedMessage.getContent()).isEqualTo("用户@例子");
    }

    // This is a mock message that doesn't implement the full functionality from MimeMessage.
    // This is only to illustrate the changes needed to make the test work.
    private static class MockInternationalizedMimeMessage extends MimeMessage {

        public MockInternationalizedMimeMessage(Session session) {
            super(session);
        }

        // Current Java Mail version does not respect the "mail.mime.address.strict"
        // value when setting email addresses in a MimeMessage object (calls
        // InternetAddress.parse(String), which will always validate the
        // address based on rfc822) so we need to override this method to
        // make this example work.
        @Override
        public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {

            InternetAddress address = new InternetAddress();
            address.setAddress(addresses);
            setAddressHeader(getHeaderName(type), new InternetAddress[]{address});
        }

        // Current Java Mail version does not respect the "mail.mime.address.strict"
        // value when setting email addresses in a MimeMessage object (calls
        // InternetAddress.parse(String), which will always validate the
        // address based on rfc822) so we need to override this method to
        // make this example work.
        @Override
        public void setFrom(String address) throws MessagingException {

            InternetAddress internetAddress = new InternetAddress();
            internetAddress.setAddress(address);
            setAddressHeader("From", new InternetAddress[]{internetAddress});
        }

        // Convenience method to set addresses
        private void setAddressHeader(String name, InternetAddress[] addresses)
                throws MessagingException {

            try {

                // Encoding the email addresses so they are correctly sent to the mail server.
                for (int i = 0; i < addresses.length; i++) {
                    String addStr = MimeUtility.encodeText(addresses[i].getAddress());
                    InternetAddress ia = new InternetAddress();
                    ia.setAddress(addStr);
                    addresses[i] = ia;
                }

                String s = InternetAddress.toString(addresses, name.length() + 2);
                setHeader(name, s);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        private String getHeaderName(Message.RecipientType type)
                throws MessagingException {
            String headerName;

            if (type == Message.RecipientType.TO)
                headerName = "To";
            else if (type == Message.RecipientType.CC)
                headerName = "Cc";
            else if (type == Message.RecipientType.BCC)
                headerName = "Bcc";
            else
                throw new MessagingException("Invalid Recipient Type");
            return headerName;
        }
    }
}
