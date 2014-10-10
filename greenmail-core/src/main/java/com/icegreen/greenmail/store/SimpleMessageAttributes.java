/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package com.icegreen.greenmail.store;


import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.util.GreenMailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Attributes of a Message in IMAP4rev1 style. Message
 * Attributes should be set when a message enters a mailbox.
 * <p> Note that the message in a mailbox have the same order using either
 * Message Sequence Numbers or UIDs.
 * <p> reinitialize() must be called on deserialization to reset Logger
 * <p/>
 * Reference: RFC 2060 - para 2.3
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */
public class SimpleMessageAttributes
        implements MailMessageAttributes {
    // Logging.
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final static String SP = " ";
    private final static String NIL = "NIL";
    private final static String Q = "\"";
    private final static String LB = "(";
    private final static String RB = ")";
    private final static boolean DEBUG = false;
    private final static String MULTIPART = "MULTIPART";
    private final static String MESSAGE = "MESSAGE";

    private int uid;
    private int messageSequenceNumber;
    private Date internalDate;
    private String internalDateString;
    private String bodyStructure;
    private String envelope;
    private int size;
    private int lineCount;
    public MailMessageAttributes[] parts;
    private List headers;

    //rfc822 or MIME header fields
    //arrays only if multiple values allowed under rfc822
    private String subject;
    private String[] from;
    private String[] sender;
    private String[] replyTo;
    private String[] to;
    private String[] cc;
    private String[] bcc;
    private String[] inReplyTo;
    private String[] date;
    private String[] messageID;
    private String contentType;
    private String primaryType = null;   // parsed from contentType
    private String secondaryType = null; // parsed from contentType
    private Set parameters;      // parsed from contentType
    private String contentID = null;
    private String contentDesc = null;
    private String contentEncoding = null;
    private String interalDateEnvelopeString = null;
    private Header contentDisposition = null;

    SimpleMessageAttributes() {
    }

    void setAttributesFor(MimeMessage msg) throws MessagingException {
        try {
            internalDate = msg.getSentDate();
        } catch (MessagingException me) {
            internalDate = new Date();
        }
        if (null == internalDate) {
            internalDate = new Date();
        }

        internalDateString = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss Z").format(internalDate);
        interalDateEnvelopeString = new MailDateFormat().format(internalDate);
        parseMimePart(msg);
        envelope = null;
        bodyStructure = null;
    }

    void setUID(int thisUID) {
        uid = thisUID;
    }

    /**
     * Parses key data items from a MimeMessage for seperate storage.
     * TODO this is a mess, and should be completely revamped.
     */
    void parseMimePart(MimePart part) throws MessagingException {
        size = GreenMailUtil.getBody(part).length();

        // Section 1 - Message Headers
        if (part instanceof MimeMessage) {
            try {
                subject = ((MimeMessage) part).getSubject();
            } catch (MessagingException me) {
//                if (DEBUG) getLogger().debug("Messaging Exception for getSubject: " + me);
            }
        }
        try {
            from = part.getHeader("From");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(From): " + me);
        }
        try {
            sender = part.getHeader("Sender");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Sender): " + me);
        }
        try {
            replyTo = part.getHeader("Reply To");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Reply To): " + me);
        }
        try {
            to = part.getHeader("To");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            cc = part.getHeader("Cc");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            bcc = part.getHeader("Bcc");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(To): " + me);
        }
        try {
            inReplyTo = part.getHeader("In Reply To");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(In Reply To): " + me);
        }
        try {
            date = part.getHeader("Date");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(Date): " + me);
        }
        try {
            messageID = part.getHeader("Message-ID");
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getHeader(messageID): " + me);
        }
        String contentTypeLine = null;
        try {
            contentTypeLine = part.getContentType();
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getContentType(): " + me);
        }
        if (contentTypeLine != null) {
            decodeContentType(contentTypeLine);
        }
        try {
            contentID = part.getContentID();
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getContentUD(): " + me);
        }
        try {
            contentDesc = part.getDescription();
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getDescription(): " + me);
        }
        try {
            contentEncoding = part.getEncoding();
            // default value.
            if (contentEncoding == null) {
                contentEncoding = "7BIT";
            }
        } catch (MessagingException me) {
//            if (DEBUG) getLogger().debug("Messaging Exception for getEncoding(): " + me);
        }

        try {
//            contentDisposition = part.getDisposition();
            contentDisposition = Header.create(part.getHeader("Content-Disposition"));
        } catch (MessagingException me) {
//                getLogger().debug("Messaging Exception for getEncoding(): " + me);
        }

        try {
            // TODO this doesn't work
            lineCount = getLineCount(part);
        } catch (MessagingException me) {
            me.printStackTrace();
//            if (DEBUG) getLogger().debug("Messaging Exception for getLineCount(): " + me);
        } catch (Exception e) {
            e.printStackTrace();
//            if (DEBUG) getLogger().debug("Exception for getLineCount(): " + e);
        }

        // Recurse through any embedded parts
        if (primaryType.equalsIgnoreCase(MULTIPART)) {
            MimeMultipart container;
            try {
                container = (MimeMultipart) part.getContent();
                int count = container.getCount();
                parts = new SimpleMessageAttributes[count];
                for (int i = 0; i < count; i++) {
                    BodyPart nextPart = container.getBodyPart(i);

                    if (nextPart instanceof MimePart) {
                        SimpleMessageAttributes partAttrs = new SimpleMessageAttributes();
                        partAttrs.parseMimePart((MimePart) nextPart);
                        parts[i] = partAttrs;

                    } else {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (primaryType.equalsIgnoreCase("message")) {
            if (secondaryType.equalsIgnoreCase("RFC822")) {
                //try {

                /*
                MimeMessageWrapper message = new MimeMessageWrapper(part.getInputStream());
                SimpleMessageAttributes msgAttrs = new SimpleMessageAttributes();
                msgAttrs.setAttributesFor(message);

                if (part instanceof MimeMessage) {
                    Comments out because I don't know what it should do here
                    MimeMessage msg1 = (MimeMessage) part;
                    MimeMessageWrapper message2 = new MimeMessageWrapper(msg1);
                    SimpleMessageAttributes msgAttrs2 = new SimpleMessageAttributes();
                    msgAttrs.setAttributesFor(message2);
                }

                parts = new SimpleMessageAttributes[1];
                parts[0] = msgAttrs;
                */
                //} catch (Exception e) {
                //getLogger().error("Error interpreting a message/rfc822: " + e);
                //e.printStackTrace();
                //}
            } else {
                log.warn("Unknown subtype of message encountered.");
            }
        }
    }

    private int getLineCount(MimePart part) throws MessagingException {
        return GreenMailUtil.getLineCount(GreenMailUtil.getBody(part));
    }

    /**
     * Builds IMAP envelope String from pre-parsed data.
     */
    String parseEnvelope() {
        List response = new ArrayList();
        //1. Date ---------------
        response.add(LB + Q + interalDateEnvelopeString + Q + SP);
        //2. Subject ---------------
        if (subject != null && (subject.length() != 0)) {
            response.add(Q + subject + Q + SP);
        } else {
            response.add(NIL + SP);
        }
        //3. From ---------------
        if (from != null && from.length > 0) {
            response.add(LB);
            for (int i = 0; i < from.length; i++) {
                response.add(parseAddress(from[i]));
            }
            response.add(RB);
        } else {
            response.add(NIL);
        }
        response.add(SP);
        //4. Sender ---------------
        if (sender != null && sender.length > 0) {
//            if (DEBUG) getLogger().debug("parsingEnvelope - sender[0] is: " + sender[0]);
            //Check for Netscape feature - sender is local part only
            if (sender[0].indexOf('@') == -1) {
                response.add(LB + (String) response.get(3) + RB); //first From address
            } else {
                response.add(LB);
                for (int i = 0; i < sender.length; i++) {
                    response.add(parseAddress(sender[i]));
                }
                response.add(RB);
            }
        } else {
            if (from != null && from.length > 0) {
                response.add(LB + (String) response.get(3) + RB); //first From address
            } else {
                response.add(NIL);
            }
        }
        response.add(SP);
        if (replyTo != null && replyTo.length > 0) {
            if (replyTo[0].indexOf('@') == -1) {
                response.add(LB + (String) response.get(3) + RB); //first From address
            } else {
                response.add(LB);
                for (int i = 0; i < replyTo.length; i++) {
                    response.add(parseAddress(replyTo[i]));
                }
                response.add(RB);
            }
        } else {
            if (from != null && from.length > 0) {
                response.add(LB + (String) response.get(3) + RB); //first From address
            } else {
                response.add(NIL);
            }
        }
        response.add(SP);
        if (to != null && to.length > 0) {
            response.add(LB);
            for (int i = 0; i < to.length; i++) {
                response.add(parseAddress(to[i]));
            }
            response.add(RB);
        } else {
            response.add(NIL);
        }
        response.add(SP);
        if (cc != null && cc.length > 0) {
            response.add(LB);
            for (int i = 0; i < cc.length; i++) {
                response.add(parseAddress(cc[i]));
            }
            response.add(RB);
        } else {
            response.add(NIL);
        }
        response.add(SP);
        if (bcc != null && bcc.length > 0) {
            response.add(LB);
            for (int i = 0; i < bcc.length; i++) {
                response.add(parseAddress(bcc[i]));
            }
            response.add(RB);
        } else {
            response.add(NIL);
        }
        response.add(SP);
        if (inReplyTo != null && inReplyTo.length > 0) {
            response.add(inReplyTo[0]);
        } else {
            response.add(NIL);
        }
        response.add(SP);
        if (messageID != null && messageID.length > 0) {
            response.add(Q + messageID[0] + Q);
        } else {
            response.add(NIL);
        }
        response.add(RB);

        StringBuilder buf = new StringBuilder(16 * response.size());
        for (int j = 0; j < response.size(); j++) {
            buf.append((String) response.get(j));
        }

        return buf.toString();
    }

    /**
     * Parses a String email address to an IMAP address string.
     */
    String parseAddress(String address) {
        int comma = address.indexOf(',');
        StringBuilder buf = new StringBuilder();
        if (comma == -1) { //single address
            buf.append(LB);
            InternetAddress netAddr = null;
            try {
                netAddr = new InternetAddress(address);
            } catch (AddressException ae) {
                return null;
            }
            String personal = netAddr.getPersonal();
            if (personal != null && (personal.length() != 0)) {
                buf.append(Q).append(personal).append(Q);
            } else {
                buf.append(NIL);
            }
            buf.append(SP);
            buf.append(NIL); // should add route-addr
            buf.append(SP);
            try {
                MailAddress mailAddr = new MailAddress(netAddr.getAddress());
                buf.append(Q).append(mailAddr.getUser()).append(Q);
                buf.append(SP);
                buf.append(Q).append(mailAddr.getHost()).append(Q);
            } catch (Exception pe) {
                buf.append(NIL + SP + NIL);
            }
            buf.append(RB);
        } else {
            buf.append(parseAddress(address.substring(0, comma)));
            buf.append(SP);
            buf.append(parseAddress(address.substring(comma + 1)));
        }
        return buf.toString();
    }

    /**
     * Decode a content Type header line into types and parameters pairs
     */
    void decodeContentType(String rawLine) {
        int slash = rawLine.indexOf('/');
        if (slash == -1) {
//            if (DEBUG) getLogger().debug("decoding ... no slash found");
            return;
        } else {
            primaryType = rawLine.substring(0, slash).trim();
        }
        int semicolon = rawLine.indexOf(';');
        if (semicolon == -1) {
//            if (DEBUG) getLogger().debug("decoding ... no semicolon found");
            secondaryType = rawLine.substring(slash + 1).trim();
            return;
        }
        // have parameters
        secondaryType = rawLine.substring(slash + 1, semicolon).trim();
        Header h = new Header(rawLine);
        parameters = h.getParams();
    }

    String parseBodyFields() {
        StringBuffer buf = new StringBuffer();
        getParameters(buf);
        buf.append(SP);
        if (contentID == null) {
            buf.append(NIL);
        } else {
            buf.append(Q).append(contentID).append(Q);
        }
        buf.append(SP);
        if (contentDesc == null) {
            buf.append(NIL);
        } else {
            buf.append(Q).append(contentDesc).append(Q);
        }
        buf.append(SP);
        if (contentEncoding == null) {
            buf.append(NIL);
        } else {
            buf.append(Q).append(contentEncoding).append(Q);
        }
        buf.append(SP);
        buf.append(size);
        return buf.toString();
    }

    private void getParameters(StringBuffer buf) {
        if (parameters == null || parameters.isEmpty()) {
            buf.append(NIL);
        } else {
            buf.append(LB);
            Iterator it = parameters.iterator();
            while (it.hasNext()) {
                buf.append((String) it.next());
                // Space separated
                if (it.hasNext()) buf.append(SP);
            }
            buf.append(RB);
        }
    }

    /**
     * Produce the IMAP formatted String for the BodyStructure of a pre-parsed MimeMessage
     * TODO handle extension elements - Content-disposition, Content-Language and other parameters.
     */
    String parseBodyStructure(boolean includeExtension) {
        try {
            String fields = parseBodyFields();
            StringBuffer buf = new StringBuffer();
            buf.append(LB);
            if (primaryType.equalsIgnoreCase("Text")) {
                buf.append("\"TEXT\" \"");
                buf.append(secondaryType.toUpperCase());
                buf.append("\" ");
                buf.append(fields);
                buf.append(' ');
                buf.append(lineCount);

                // is:    * 1 FETCH (BODYSTRUCTURE ("Text" "plain" NIL NIL NIL NIL    4  -1))
                // wants: * 1 FETCH (BODYSTRUCTURE ("text" "plain" NIL NIL NIL "8bit" 6  1  NIL NIL NIL))
                // or:    * 1 FETCH (BODYSTRUCTURE ("text" "plain" NIL NIL NIL "7bit" 28 1 NIL NIL NIL))

            } else if (primaryType.equalsIgnoreCase(MESSAGE) && secondaryType.equalsIgnoreCase("rfc822")) {
                buf.append("\"MESSAGE\" \"RFC822\" ");
                buf.append(fields).append(SP);
//                setupLogger(parts[0]); // reset transient logger
                buf.append(parts[0].getEnvelope()).append(SP);
                buf.append(parts[0].getBodyStructure(false)).append(SP);
                buf.append(lineCount);
            } else if (primaryType.equalsIgnoreCase(MULTIPART)) {
                for (int i = 0; i < parts.length; i++) {
//                    setupLogger(parts[i]); // reset transient getLogger()
                    buf.append(parts[i].getBodyStructure(includeExtension));
                }
                buf.append(SP + Q).append(secondaryType).append(Q);
            } else {
                //1. primary type -------
                buf.append('\"');
                buf.append(primaryType.toUpperCase());
                buf.append('\"');
                //2. sec type -------
                buf.append(" \"");
                buf.append(secondaryType.toUpperCase());
                buf.append('\"');
                //3. params -------
                buf.append(' ');
                getParameters(buf);
                //4. body id -------
                buf.append(' ');
                buf.append(NIL);
                //5. content desc -------
                buf.append(' ');
                if (null != contentDesc) {
                    buf.append('\"');
                    buf.append(contentDesc);
                    buf.append('\"');
                } else {
                    buf.append(NIL);
                }
                //6. encoding -------
                buf.append(' ');
                if (null != contentEncoding) {
                    buf.append('\"');
                    buf.append(contentEncoding);
                    buf.append('\"');
                } else {
                    buf.append(NIL);
                }
                //7. size -------
                buf.append(' ');
                buf.append(size);
            }

            if (includeExtension) {
                //extension is different for multipart and single parts
                if (primaryType.equalsIgnoreCase(MULTIPART)) {
                    //8. ext1 params -------
                    buf.append(' ');
                    getParameters(buf);
                    //9. ext2 disposition -------
                    buf.append(' ');
                    if (null != contentDisposition) {
                        buf.append(contentDisposition);
                    } else {
                        buf.append(NIL);
                    }
                    //10. ext3 language -------
                    buf.append(' ');
                    buf.append(NIL);
                } else {
                    // ext1 md5 -------
                    buf.append(' ');
                    buf.append(NIL);
                    // ext2 disposition -------
                    buf.append(' ');
                    if (null != contentDisposition) {
                        buf.append(contentDisposition);
                    } else {
                        buf.append(NIL);
                    }
                    //ext3 language -------
                    buf.append(' ');
                    buf.append(NIL);
                }
            }

            buf.append(RB);
            return buf.toString();
        } catch (Exception e) {
//            getLogger().error("Exception while parsing BodyStrucuture: " + e);
            e.printStackTrace();
            throw new RuntimeException("Exception in parseBodyStructure");
        }
    }

    /**
     * Provides the current Message Sequence Number for this message. MSNs
     * change when messages are expunged from the mailbox.
     *
     * @return int a positive non-zero integer
     */
    public int getMessageSequenceNumber() {
        return messageSequenceNumber;
    }

    void setMessageSequenceNumber(int newMsn) {
        messageSequenceNumber = newMsn;
    }


    /**
     * Provides the unique identity value for this message. UIDs combined with
     * a UIDValidity value form a unique reference for a message in a given
     * mailbox. UIDs persist across sessions unless the UIDValidity value is
     * incremented. UIDs are not copied if a message is copied to another
     * mailbox.
     *
     * @return int a 32-bit value
     */
    public int getUID() {
        return uid;
    }

    /**
     * Provides the date and time at which the message was received. In the
     * case of delivery by SMTP, this SHOULD be the date and time of final
     * delivery as defined for SMTP. In the case of messages copied from
     * another mailbox, it shuld be the internalDate of the source message. In
     * the case of messages Appended to the mailbox, example drafts,  the
     * internalDate is either specified in the Append command or is the
     * current dat and time at the time of the Append.
     *
     * @return Date imap internal date
     */
    public Date getInternalDate() {
        return internalDate;
    }

    public String getInternalDateAsString() {
        return internalDateString;
    }

    /**
     * Provides the sizeof the message in octets.
     *
     * @return int number of octets in message.
     */
    public int getSize() {
        return size;
    }

    /**
     * Provides the Envelope structure information for this message. This is a parsed representation of the rfc-822 envelope information. This is not to be confused with the SMTP envelope!
     *
     * @return String satisfying envelope syntax in rfc 2060.
     */
    public String getEnvelope() {
        return parseEnvelope();
    }

    /**
     * Provides the Body Structure information for this message. This is a parsed representtion of the MIME structure of the message.
     *
     * @return String satisfying body syntax in rfc 2060.
     */
    public String getBodyStructure(boolean includeExtensions) {
        return parseBodyStructure(includeExtensions);
    }


    //~ inner class
    private static class Header {
        String value;
        Set params = null;

        public Header(String line) {
            String[] strs = line.split(";");
            value = strs[0];
            if (0 != strs.length) {
                params = new HashSet();
                for (int i = 1; i < strs.length; i++) {
                    String p = strs[i].trim();
                    int e = p.indexOf('=');
                    String key = p.substring(0, e);
                    String value = p.substring(e + 1, p.length());
                    p = Q + strip(key) + Q + SP + Q + strip(value) + Q;
                    params.add(p);
                }
            }
        }

        public Set getParams() {
            return params;
        }

        private String strip(String s) {
            return s.replaceAll("\\\"", "");
        }

        public String toString() {
            StringBuilder ret = new StringBuilder();
            if (null == params) {
                ret.append(Q).append(value).append(Q);
            } else {
                ret.append(LB);
                ret.append(Q).append(value).append(Q + SP);
                ret.append(LB);
                int i = 0;
                for (Iterator iterator = params.iterator(); iterator.hasNext();) {
                    if (i++ > 0) {
                        ret.append(SP);
                    }
                    String s = (String) iterator.next();
                    ret.append(s);
                }
                ret.append(RB);
                ret.append(RB);
            }
            return ret.toString();
        }

        public static Header create(String[] header) {
            if (null == header || 0 == header.length) {
                return null;
            }
            if (header.length > 1) {
                throw new IllegalArgumentException("Header creation assumes only one occurrence of header");
            }
            return new Header(header[0]);
        }
    }
}
