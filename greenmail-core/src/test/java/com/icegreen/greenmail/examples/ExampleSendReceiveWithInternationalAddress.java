package com.icegreen.greenmail.examples;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.util.PropUtil;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ExampleSendReceiveWithInternationalAddress {

    static final Properties properties;

    static {

        properties = new Properties();
        properties.put("mail.mime.address.strict", Boolean.FALSE.toString());
    }

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP_IMAP);

    @Test
    public void testSend() throws MessagingException, UnsupportedEncodingException {

        Session session = GreenMailUtil.getSession(ServerSetupTest.SMTP, properties);
        MimeMessage mimeMessage = new ExampleMimeMessage(session);
        mimeMessage.setSubject("subject");
        mimeMessage.setSentDate(new Date());
        mimeMessage.setFrom("múchätįldé@tìldę.oœ");
        mimeMessage.setRecipients(Message.RecipientType.TO, "用户@例子.广告");

        // The body text needs to be encoded if it contains non us-ascii characters
        mimeMessage.setText(MimeUtility.encodeText("用户@例子"));

        GreenMailUtil.sendMimeMessage(mimeMessage);

        // Decoding the body text to verify equality
        String decodedText = MimeUtility.decodeText(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]));
        assertEquals("用户@例子", decodedText);
    }

    private static class ExampleMimeMessage extends MimeMessage {

        public ExampleMimeMessage(Session session) {
            super(session);
        }

        // Current Java Mail version does not respect the "mail.mime.address.strict"
        // value when setting email addresses in a MimeMessage object (calls
        // InternetAddress.parse(String), which will always validate the
        // address based on rfc822) so we need to override this method to
        // make this example work.
        @Override
        public void setRecipients(Message.RecipientType type, String addresses) throws MessagingException {

            if (type == RecipientType.NEWSGROUPS) {

                if (addresses == null || addresses.length() == 0)
                    removeHeader("Newsgroups");
                else
                    setHeader("Newsgroups", addresses);

            } else {

                if (!isStrict()) {

                    InternetAddress address = new InternetAddress();
                    address.setAddress(addresses);
                    setAddressHeader(getHeaderName(type), new InternetAddress[]{address});

                } else {

                    setAddressHeader(getHeaderName(type),

                            addresses == null ? null : InternetAddress.parse(addresses));
                }
            }
        }

        // Current Java Mail version does not respect the "mail.mime.address.strict"
        // value when setting email addresses in a MimeMessage object (calls
        // InternetAddress.parse(String), which will always validate the
        // address based on rfc822) so we need to override this method to
        // make this example work.
        @Override
        public void setFrom(String address) throws MessagingException {

            if (address == null)
                    removeHeader("From");

            else {

                if (!isStrict()) {

                    InternetAddress internetAddress = new InternetAddress();
                    internetAddress.setAddress(address);
                    setAddressHeader("From", new InternetAddress[]{internetAddress});

                } else {

                    setAddressHeader("From", InternetAddress.parse(address));
                }
            }
        }

        private boolean isStrict() {
            return PropUtil.getBooleanSessionProperty(session, "mail.mime.address.strict", true);
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
                if (s == null)
                    removeHeader(name);
                else
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
            else if (type == MimeMessage.RecipientType.NEWSGROUPS)
                headerName = "Newsgroups";
            else
                throw new MessagingException("Invalid Recipient Type");
            return headerName;
        }
    }
}
