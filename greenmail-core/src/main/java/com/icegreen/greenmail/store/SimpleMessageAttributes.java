/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.store;


import java.util.*;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.*;

import com.icegreen.greenmail.mail.MailAddress;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.sun.mail.imap.protocol.INTERNALDATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attributes of a Message in IMAP4rev1 style. Message
 * Attributes should be set when a message enters a mailbox.
 * <p> Note that the message in a mailbox have the same order using either
 * Message Sequence Numbers or UIDs.
 * <p> reinitialize() must be called on deserialization to reset Logger
 *
 * Reference: RFC 2060 - para 2.3 https://www.ietf.org/rfc/rfc2060.txt
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */
public class SimpleMessageAttributes
        implements MailMessageAttributes {
    // Logging.
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String SP = " ";
    private static final String NIL = "NIL";
    private static final String Q = "\"";
    private static final String LB = "(";
    private static final String RB = ")";
    private static final String MULTIPART = "MULTIPART";
    private static final String MESSAGE = "MESSAGE";

    private int uid;
    private int messageSequenceNumber;
    private Date receivedDate;
    private String bodyStructure;
    private String envelope;
    private int size;
    private int lineCount;
    public MailMessageAttributes[] parts;
    private List<String> headers;

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
    private String primaryType;   // parsed from contentType
    private String secondaryType; // parsed from contentType
    private Set<String> parameters;      // parsed from contentType
    private String contentID;
    private String contentDesc;
    private String contentEncoding;
    private String receivedDateString;
    private String sentDateEnvelopeString;
    private Header contentDisposition;

    SimpleMessageAttributes(MimeMessage msg, Date receivedDate) throws MessagingException {
        Date sentDate = getSentDate(msg, receivedDate);

        if(null != receivedDate) {
            this.receivedDate = receivedDate;
            receivedDateString = INTERNALDATE.format(receivedDate);
        }
        if(null != sentDate) {
            sentDateEnvelopeString = new MailDateFormat().format(sentDate);
        }

        if (msg != null) {
            parseMimePart(msg);
        }
    }

    /**
     * Compute "sent" date
     *
     * @param msg        Message to take sent date from. May be null to use default
     * @param defaultVal Default if sent date is not present
     * @return Sent date or now if no date could be found
     */
    private static Date getSentDate(MimeMessage msg, Date defaultVal) {
        if (msg == null) {
            return defaultVal;
        }
        try {
            Date sentDate = msg.getSentDate();
            if (sentDate == null) {
                return defaultVal;
            } else {
                return sentDate;
            }
        } catch (MessagingException me) {
            return new Date();
        }
    }

    void setUID(int thisUID) {
        uid = thisUID;
    }

    /**
     * Parses key data items from a MimeMessage for seperate storage.
     * TODO this is a mess, and should be completely revamped.
     */
    void parseMimePart(MimePart part) throws MessagingException {
        final String body = GreenMailUtil.getBody(part);
        size = body.length();

        // Section 1 - Message Headers
        if (part instanceof MimeMessage) {
            try {
                String[] subjects = part.getHeader("Subject");
                if ((subjects != null) && (subjects.length > 0)) {
                    subject = subjects[0];
                }
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
            replyTo = part.getHeader("Reply-To");
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
            if (log.isDebugEnabled()) {
                log.debug("Can not create content disposition for part " + part, me);
            }
        }

        try {
            // TODO this doesn't work
            lineCount = GreenMailUtil.getLineCount(body);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Can not get line count for part " + part, e);
            }
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
                        SimpleMessageAttributes partAttrs = new SimpleMessageAttributes(null, receivedDate);
                        partAttrs.parseMimePart((MimePart) nextPart);
                        parts[i] = partAttrs;

                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Can not recurse through multipart content", e);
                }
            }
        } else if (primaryType.equalsIgnoreCase("message")) {
            if (secondaryType.equalsIgnoreCase("RFC822")) {
                parts = new SimpleMessageAttributes[1];
                try {
                    MimeMessage wrappedMessage = (MimeMessage) part.getContent();
                    if(log.isDebugEnabled()) {
                        log.debug("message type : " + wrappedMessage.getContentType());
                    }
                    parts[0] = new SimpleMessageAttributes(wrappedMessage, null);
                } catch (Exception e) {
                    throw new IllegalStateException("Can not extract part for "+primaryType+"/"+secondaryType, e);
                }
            } else {
                log.warn("Unknown/unhandled subtype {} of message encountered.", secondaryType);
            }
        }
    }

    /**
     * Builds IMAP envelope String from pre-parsed data.
     */
    private String parseEnvelope() {
        List<String> response = new ArrayList<>();
        //1. Date ---------------
        response.add(LB + Q + sentDateEnvelopeString + Q + SP);
        //2. Subject ---------------
        if (subject != null && (subject.length() != 0)) {
            response.add(Q + escapeHeader(subject) + Q + SP);
        } else {
            response.add(NIL + SP);
        }
        //3. From ---------------
        addAddressToEnvelopeIfAvailable(from, response);
        response.add(SP);
        //4. Sender ---------------
        addAddressToEnvelopeIfAvailableWithNetscapeFeature(sender, response);
        response.add(SP);
        addAddressToEnvelopeIfAvailableWithNetscapeFeature(replyTo, response);
        response.add(SP);
        addAddressToEnvelopeIfAvailable(to, response);
        response.add(SP);
        addAddressToEnvelopeIfAvailable(cc, response);
        response.add(SP);
        addAddressToEnvelopeIfAvailable(bcc, response);
        response.add(SP);
        if (inReplyTo != null && inReplyTo.length > 0) {
            response.add(inReplyTo[0]);
        } else {
            response.add(NIL);
        }
        response.add(SP);
        if (messageID != null && messageID.length > 0) {
            messageID[0] = escapeHeader(messageID[0]);
            response.add(Q + messageID[0] + Q);
        } else {
            response.add(NIL);
        }
        response.add(RB);

        StringBuilder buf = new StringBuilder(16 * response.size());
        for (String aResponse : response) {
            buf.append(aResponse);
        }

        return buf.toString();
    }

    private void addAddressToEnvelopeIfAvailableWithNetscapeFeature(String[] addresses, List<String> response) {
        if (addresses != null && addresses.length > 0) {
//            if (DEBUG) getLogger().debug("parsingEnvelope - sender[0] is: " + sender[0]);
            //Check for Netscape feature - sender is local part only
            if (addresses[0].indexOf('@') == -1) {
                response.add(LB + response.get(3) + RB); //first From address
            } else {
                response.add(LB);
                addAddressToEnvelope(addresses, response);
                response.add(RB);
            }
        } else {
            if (from != null && from.length > 0) {
                response.add(LB + response.get(3) + RB); //first From address
            } else {
                response.add(NIL);
            }
        }
    }

    private void addAddressToEnvelopeIfAvailable(String[] addresses, List<String> response) {
        if (addresses != null && addresses.length > 0) {
            response.add(LB);
            addAddressToEnvelope(addresses, response);
            response.add(RB);
        } else {
            response.add(NIL);
        }
    }

    private void addAddressToEnvelope(String[] addresses, List<String> response) {
        for (String address : addresses) {
            response.add(parseAddress(address));
        }
    }

    /**
     * Parses a String email address to an IMAP address string.
     */
    private String parseAddress(String address) {
        try {
            StringBuilder buf = new StringBuilder();
            InternetAddress[] netAddrs = InternetAddress.parseHeader(address, false);
            for (InternetAddress netAddr : netAddrs) {
                if (buf.length() > 0) {
                    buf.append(SP);
                }

                buf.append(LB);

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
                    // Remove quotes to avoid double quoting
                    MailAddress mailAddr = new MailAddress(netAddr.getAddress().replaceAll("\"", "\\\\\""));
                    buf.append(Q).append(mailAddr.getUser()).append(Q);
                    buf.append(SP);
                    buf.append(Q).append(mailAddr.getHost()).append(Q);
                } catch (Exception pe) {
                    buf.append(NIL + SP + NIL);
                }
                buf.append(RB);
            }

            return buf.toString();
        } catch (AddressException e) {
            throw new RuntimeException("Failed to parse address: " + address, e);
        }
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
        StringBuilder buf = new StringBuilder();
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

    private void getParameters(StringBuilder buf) {
        if (parameters == null || parameters.isEmpty()) {
            buf.append(NIL);
        } else {
            buf.append(LB);
            Iterator<String> it = parameters.iterator();
            while (it.hasNext()) {
                buf.append(it.next());
                // Space separated
                if (it.hasNext()) {
                    buf.append(SP);
                }
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
            StringBuilder buf = new StringBuilder();
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
                for (MailMessageAttributes part : parts) {
//                    setupLogger(parts[i]); // reset transient getLogger()
                    buf.append(part.getBodyStructure(includeExtension));
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
                if (null != contentID ) {
                    buf.append(Q).append(contentID).append(Q);
                } else {
                    buf.append(NIL);
                }
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
            throw new IllegalStateException("Can not parse body structure", e);
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

    @Override
    public Date getReceivedDate() {
        return receivedDate;
    }

    @Override
    public String getReceivedDateAsString() {
        return receivedDateString;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String getEnvelope() {
        return parseEnvelope();
    }

    @Override
    public String getBodyStructure(boolean includeExtensions) {
        return parseBodyStructure(includeExtensions);
    }


    /**
     * http://tools.ietf.org/html/rfc1806
     * http://tools.ietf.org/html/rfc2183 content disposition
     */
    private static class Header {
        String value;
        Set<String> params = null;

        public Header(String line) {
            String[] strs = line.split(";");
            if (0 != strs.length) {
                value = strs[0];
                params = new HashSet<>(strs.length);
                for (int i = 1; i < strs.length; i++) {
                    String p = strs[i].trim();
                    int e = p.indexOf('=');
                    String key = p.substring(0, e);
                    String val = p.substring(e + 1);
                    p = Q + strip(key) + Q + SP + Q + strip(val) + Q;
                    params.add(p);
                }
            }
        }

        public Set<String> getParams() {
            return params;
        }

        private String strip(String s) {
            return s.trim().replaceAll("\\\"", "");
        }

        @Override
        public String toString() {
            // https://tools.ietf.org/html/rfc3501#section-9 body-fld-dsp
            StringBuilder ret = new StringBuilder();
            if (null == params) {
                ret.append(Q).append(value).append(Q);
            } else {
                ret.append(LB);
                ret.append(Q).append(value).append(Q + SP);
                if (params.isEmpty()) {
                    ret.append(NIL);
                } else {
                    ret.append(LB);
                    int i = 0;
                    for (String param : params) {
                        if (i++ > 0) {
                            ret.append(SP);
                        }
                        ret.append(param);
                    }
                    ret.append(RB);
                }
                ret.append(RB);
            }
            return ret.toString();
        }

        public static Header create(String[] header) {
            if (null == header || 0 == header.length) {
                return null;
            }
            if (header.length > 1) {
                throw new IllegalArgumentException("Header creation assumes only one occurrence of header instead of " + header.length);
            }
            return new Header(header[0]);
        }
    }

    private String escapeHeader(final String text) {
        return MimeUtility.unfold(text).replace("\\", "\\\\").replace("\"", "\\\"");
    }

}
