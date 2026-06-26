/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap.commands;

import static com.icegreen.greenmail.imap.ImapConstants.*;

import com.icegreen.greenmail.imap.*;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.util.GreenMailUtil;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Handles processing for the FETCH imap command.
 * <p/>
 * https://tools.ietf.org/html/rfc3501#section-6.4.5
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class FetchCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "FETCH";
    public static final String ARGS = "<message-set> <fetch-profile>";
    private static final Flags FLAGS_SEEN = new Flags(Flags.Flag.SEEN);
    private static final Pattern NUMBER_MATCHER = Pattern.compile("^\\d+$");

    private final FetchCommandParser fetchParser = new FetchCommandParser();

    FetchCommand() {
        super(NAME, ARGS);
    }

    @Override
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
        throws ProtocolException, FolderException {
        doProcess(request, response, session, false);
    }

    @Override
    public void doProcess(ImapRequestLineReader request,
                          ImapResponse response,
                          ImapSession session,
                          boolean useUids)
        throws ProtocolException, FolderException {
        IdRange[] idSet = fetchParser.parseIdRange(request);
        FetchRequest fetch = fetchParser.fetchRequest(request);
        fetchParser.endLine(request);

        if (useUids) {
            fetch.uid = true;
        }

        ImapSessionFolder mailbox = session.getSelected();
        long[] uids = mailbox.getMessageUids();
        for (long uid : uids) {
            int msn = mailbox.getMsn(uid);

            if ((useUids && includes(idSet, uid)) ||
                (!useUids && includes(idSet, msn))) {
                String msgData = getMessageData(useUids, fetch, mailbox, uid);
                response.fetchResponse(msn, msgData);
            }
        }

        // a wildcard search must include the last message if the folder is not empty,
        // as per https://tools.ietf.org/html/rfc3501#section-6.4.8
        long lastMessageUid = uids.length > 0 ? uids[uids.length - 1] : -1L;
        if (mailbox.getMessageCount() > 0 && includes(idSet, Long.MAX_VALUE) && !includes(idSet, lastMessageUid)) {
            String msgData = getMessageData(useUids, fetch, mailbox, lastMessageUid);
            response.fetchResponse(mailbox.getMsn(lastMessageUid), msgData);
        }

        boolean omitExpunged = !useUids;
        session.unsolicitedResponses(response, omitExpunged);
        response.commandComplete(this);
    }

    private String getMessageData(boolean useUids, FetchRequest fetch, ImapSessionFolder mailbox, long uid) throws FolderException {
        StoredMessage storedMessage = mailbox.getMessage(uid);
        return outputMessage(fetch, storedMessage, mailbox, useUids);
    }


    private String outputMessage(FetchRequest fetch, StoredMessage message,
                                 ImapSessionFolder folder, boolean useUids)
        throws FolderException {
        // Check if this fetch will cause the "SEEN" flag to be set on this message
        // If so, update the flags, and ensure that a flags response is included in the response.
        boolean ensureFlagsResponse = false;
        if (fetch.isSetSeen() && !message.isSet(Flags.Flag.SEEN)) {
            folder.setFlags(FLAGS_SEEN, true, message.getUid(), folder, useUids);
            message.setFlags(FLAGS_SEEN, true);
            ensureFlagsResponse = true;
        }

        StringBuilder response = new StringBuilder();

        // FLAGS response
        if (fetch.flags || ensureFlagsResponse) {
            response.append(" FLAGS ");
            response.append(MessageFlags.format(message.getFlags()));
        }

        // INTERNALDATE response
        if (fetch.internalDate) {
            response.append(" INTERNALDATE \"");
            // TODO format properly
            response.append(message.getAttributes().getReceivedDateAsString());
            response.append('\"');
        }

        // RFC822.SIZE response
        if (fetch.size) {
            response.append(" RFC822.SIZE ");
            response.append(message.getAttributes().getSize());
        }

        // ENVELOPE response
        if (fetch.envelope) {
            response.append(" ENVELOPE ");
            response.append(message.getAttributes().getEnvelope());
        }

        // BODY response
        if (fetch.body) {
            response.append(" BODY ");
            response.append(message.getAttributes().getBodyStructure(false));
        }

        // BODYSTRUCTURE response
        if (fetch.bodyStructure) {
            response.append(" BODYSTRUCTURE ");
            response.append(message.getAttributes().getBodyStructure(true));
        }

        // UID response
        if (fetch.uid) {
            response.append(" UID ");
            response.append(message.getUid());
        }

        // BODY part responses.
        Collection<BodyFetchElement> elements = fetch.getBodyElements();
        for (BodyFetchElement fetchElement : elements) {
            response.append(SP);
            response.append(fetchElement.getResponseName());
            final Partial partial = fetchElement.getPartial();
            if (null == partial) {
                response.append(SP);
            }

            // Various mechanisms for returning message body.
            String sectionSpecifier = fetchElement.getParameters();

            MimeMessage mimeMessage = message.getMimeMessage();
            try {
                handleBodyFetch(mimeMessage, sectionSpecifier, partial, response);
            } catch (Exception e) {
                throw new FolderException(e);
            }
        }

        if (response.length() > 0) {
            // Remove the leading " ".
            return response.substring(1);
        } else {
            return "";
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return Base64.getEncoder().encode(byteArrayOutputStream.toByteArray());
    }

    private void handleBodyFetch(MimeMessage mimeMessage,
                                 String sectionSpecifier,
                                 Partial partial,
                                 StringBuilder response) throws IOException, MessagingException {
        if (log.isDebugEnabled()) {
            log.debug("Fetching body part for section specifier {} and mime message (contentType={})",
                sectionSpecifier, mimeMessage.getContentType());
        }

        if (sectionSpecifier.isEmpty()) {
            // TODO - need to use an InputStream from the response here.
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            mimeMessage.writeTo(bout);
            byte[] bytes = bout.toByteArray();
            bytes = doPartial(partial, bytes, response);
            addLiteral(bytes, response);
        } else if ("HEADER".equalsIgnoreCase(sectionSpecifier)) {
            Enumeration<?> inum = mimeMessage.getAllHeaderLines();
            addHeaders(inum, response, partial);
        } else if (sectionSpecifier.startsWith("HEADER.FIELDS.NOT")) {
            String[] excludeNames = extractHeaderList(sectionSpecifier, "HEADER.FIELDS.NOT".length());
            Enumeration<?> inum = mimeMessage.getNonMatchingHeaderLines(excludeNames);
            addHeaders(inum, response, partial);
        } else if (sectionSpecifier.startsWith("HEADER.FIELDS ")) {
            String[] includeNames = extractHeaderList(sectionSpecifier, "HEADER.FIELDS ".length());
            Enumeration<?> inum = mimeMessage.getMatchingHeaderLines(includeNames);
            addHeaders(inum, response, partial);
        } else if (sectionSpecifier.endsWith("MIME")) {
            String[] strs = sectionSpecifier.trim().split("\\.");
            int partNumber = Integer.parseInt(strs[0]) - 1;
            MimeMultipart mp = (MimeMultipart) mimeMessage.getContent();
            byte[] bytes = GreenMailUtil.getHeaderAsBytes(mp.getBodyPart(partNumber));
            bytes = doPartial(partial, bytes, response);
            addLiteral(bytes, response);
        } else if ("TEXT".equalsIgnoreCase(sectionSpecifier)) {
            handleBodyFetchForText(mimeMessage, partial, response);
        } else {
            Object content = mimeMessage.getContent();
            if (content instanceof String) {
                handleBodyFetchForText(mimeMessage, partial, response);
            } else if (content instanceof InputStream) {
                byte[] bytes = readAllBytes((InputStream) content);
                bytes = doPartial(partial, bytes, response);
                addLiteral(bytes, response);
            } else {
                MimeMultipart mp = (MimeMultipart) content;
                BodyPart part = null;

                // Find part by number spec, eg "1" or "2.1" or "4.3.1" ...
                String spec = sectionSpecifier;

                int dotIdx = spec.indexOf('.');
                String pre = dotIdx < 0 ? spec : spec.substring(0, dotIdx);
                while (null != pre && NUMBER_MATCHER.matcher(pre).matches()) {
                    int partNumber = Integer.parseInt(pre) - 1;
                    if (null == part) {
                        part = mp.getBodyPart(partNumber);
                    } else {
                        // Content must be multipart
                        part = ((Multipart) part.getContent()).getBodyPart(partNumber);
                    }

                    dotIdx = spec.indexOf('.');
                    if (dotIdx > 0) { // Another sub part index?
                        spec = spec.substring(dotIdx + 1);
                        dotIdx = spec.indexOf('.');
                        if (dotIdx > 0) {
                            pre = spec.substring(0, dotIdx);
                        } else {
                            pre = spec;
                        }
                    } else {
                        pre = null;
                    }
                }

                if (null == part) {
                    throw new IllegalStateException("Got null for " + sectionSpecifier);
                }

                // A bit optimistic to only cover theses cases ... TODO
                if ("message/rfc822".equalsIgnoreCase(part.getContentType())) {
                    handleBodyFetch((MimeMessage) part.getContent(), spec, partial, response);
                } else if ("TEXT".equalsIgnoreCase(spec)) {
                    handleBodyFetchForText(mimeMessage, partial, response);
                } else {
                    byte[] bytes = GreenMailUtil.getBodyAsBytes(part);
                    bytes = doPartial(partial, bytes, response);
                    addLiteral(bytes, response);
                }
            }
        }
    }

    private void handleBodyFetchForText(MimeMessage mimeMessage, Partial partial, StringBuilder response) {
        byte[] bytes = GreenMailUtil.getBodyAsBytes(mimeMessage);
        bytes = doPartial(partial, bytes, response);
        addLiteral(bytes, response);
    }

    private byte[] doPartial(Partial partial, byte[] bytes, StringBuilder response) {
        if (null != partial) {
            int len = partial.computeLength(bytes.length);
            int start = partial.computeStart(bytes.length);
            byte[] newBytes = new byte[len];
            System.arraycopy(bytes, start, newBytes, 0, len);
            bytes = newBytes;
            response.append('<').append(partial.start).append('>');
        }
        return bytes;
    }

    private void addLiteral(byte[] bytes, StringBuilder response) {
        response.append('{');
        response.append(bytes.length);
        response.append('}');
        response.append("\r\n");

        for (byte b : bytes) {
            // See https://github.com/greenmail-mail-test/greenmail/issues/257
            final char c = (char) (b & 0xFF);
            response.append(c);
        }
    }

    // TODO should do this at parse time.
    private String[] extractHeaderList(String headerList, int prefixLen) {
        // Remove the trailing and leading ')('
        String tmp = headerList.substring(prefixLen + 1, headerList.length() - 1);
        return split(tmp, " ");
    }

    private String[] split(String value, String delimiter) {
        List<String> strings = new ArrayList<>();
        int startPos = 0;
        int delimPos;
        while ((delimPos = value.indexOf(delimiter, startPos)) != -1) {
            String sub = value.substring(startPos, delimPos);
            strings.add(sub);
            startPos = delimPos + 1;
        }
        String sub = value.substring(startPos);
        strings.add(sub);

        return strings.toArray(new String[0]);
    }

    private void addHeaders(Enumeration<?> inum, StringBuilder response, Partial partial) {
        StringBuilder buf = new StringBuilder();

        int count = 0;
        while (inum.hasMoreElements()) {
            String line = (String) inum.nextElement();
            count += line.length() + 2;
            buf.append(line).append("\r\n");
        }

        if (null != partial) {
            final String partialContent = buf.toString();
            int len = partial.computeLength(partialContent.length()); // TODO : Charset?
            int start = partial.computeStart(partialContent.length());

            response.append('<').append(partial.start).append('>');
            response.append(" {");
            response.append(len);
            response.append('}');
            response.append("\r\n");

            response.append(partialContent, start, start + len);
        } else {
            response.append("{");
            response.append(count);
            response.append('}');
            response.append("\r\n");

            response.append(buf);
        }
    }

    private static class FetchCommandParser extends CommandParser {

        FetchRequest fetchRequest(ImapRequestLineReader request)
            throws ProtocolException {
            FetchRequest fetch = new FetchRequest();

            // Parenthesis optional if single 'atom'
            char next = nextNonSpaceChar(request);
            boolean parenthesis = '(' == next;
            if (parenthesis) {
                consumeChar(request, '(');

                next = nextNonSpaceChar(request);
                while (next != ')') {
                    addNextElement(request, fetch);
                    next = nextNonSpaceChar(request);
                }

                consumeChar(request, ')');
            } else {
                // Single item
                addNextElement(request, fetch);
            }

            return fetch;
        }

        private void addNextElement(ImapRequestLineReader command, FetchRequest fetch)
            throws ProtocolException {
            char next = nextCharInLine(command);
            StringBuilder element = new StringBuilder();
            while (next != ' ' && next != '[' && next != ')' && !isCrOrLf(next)) {
                element.append(next);
                command.consume();
                next = command.nextChar();
            }
            String name = element.toString();
            // Simple elements with no '[]' parameters.
            if (next == ' ' || next == ')' || isCrOrLf(next)) {
                if ("FAST".equalsIgnoreCase(name)) {
                    fetch.flags = true;
                    fetch.internalDate = true;
                    fetch.size = true;
                } else if ("FULL".equalsIgnoreCase(name)) {
                    fetch.flags = true;
                    fetch.internalDate = true;
                    fetch.size = true;
                    fetch.envelope = true;
                    fetch.body = true;
                } else if ("ALL".equalsIgnoreCase(name)) {
                    fetch.flags = true;
                    fetch.internalDate = true;
                    fetch.size = true;
                    fetch.envelope = true;
                } else if ("FLAGS".equalsIgnoreCase(name)) {
                    fetch.flags = true;
                } else if ("RFC822.SIZE".equalsIgnoreCase(name)) {
                    fetch.size = true;
                } else if ("ENVELOPE".equalsIgnoreCase(name)) {
                    fetch.envelope = true;
                } else if ("INTERNALDATE".equalsIgnoreCase(name)) {
                    fetch.internalDate = true;
                } else if ("BODY".equalsIgnoreCase(name)) {
                    fetch.body = true;
                } else if ("BODYSTRUCTURE".equalsIgnoreCase(name)) {
                    fetch.bodyStructure = true;
                } else if ("UID".equalsIgnoreCase(name)) {
                    fetch.uid = true;
                } else if ("RFC822".equalsIgnoreCase(name)) {
                    fetch.add(new BodyFetchElement("RFC822", ""), false);
                } else if ("RFC822.HEADER".equalsIgnoreCase(name)) {
                    fetch.add(new BodyFetchElement("RFC822.HEADER", "HEADER"), true);
                } else if ("RFC822.TEXT".equalsIgnoreCase(name)) {
                    fetch.add(new BodyFetchElement("RFC822.TEXT", "TEXT"), false);
                } else {
                    throw new ProtocolException("Invalid fetch attribute: " + name);
                }
            } else {
                consumeChar(command, '[');

                StringBuilder sectionIdentifier = new StringBuilder();
                next = nextCharInLine(command);
                while (next != ']') {
                    sectionIdentifier.append(next);
                    command.consume();
                    next = nextCharInLine(command);
                }
                consumeChar(command, ']');

                String parameter = sectionIdentifier.toString();

                Partial partial = null;
                next = command.nextChar(); // Can be end of line if single option
                if ('<' == next) { // Partial eg <2000> or <0.1000>
                    partial = parsePartial(command);
                }

                if ("BODY".equalsIgnoreCase(name)) {
                    fetch.add(new BodyFetchElement("BODY[" + parameter + ']', parameter, partial), false);
                } else if ("BODY.PEEK".equalsIgnoreCase(name)) {
                    fetch.add(new BodyFetchElement("BODY[" + parameter + ']', parameter, partial), true);
                } else {
                    throw new ProtocolException("Invalid fetch attribute: " + name + "[]");
                }
            }
        }

        private Partial parsePartial(ImapRequestLineReader command) throws ProtocolException {
            consumeChar(command, '<');
            int size = (int) consumeLong(command); // Assume <start>
            int start = 0;
            if (command.nextChar() == '.') {
                consumeChar(command, '.');
                start = size; // Assume <start.size> , so switch fields
                size = (int) consumeLong(command);
            }
            consumeChar(command, '>');
            return Partial.as(start, size);
        }

        private char nextCharInLine(ImapRequestLineReader request)
            throws ProtocolException {
            char next = request.nextChar();
            if (isCrOrLf(next)) {
                throw new ProtocolException("Unexpected end of line (CR or LF).");
            }
            return next;
        }

        private char nextNonSpaceChar(ImapRequestLineReader request)
            throws ProtocolException {
            char next = request.nextChar();
            while (next == ' ') {
                request.consume();
                next = request.nextChar();
            }
            return next;
        }

    }

    private static class FetchRequest {
        boolean flags;
        boolean uid;
        boolean internalDate;
        boolean size;
        boolean envelope;
        boolean body;
        boolean bodyStructure;

        private boolean setSeen = false;

        private final Set<BodyFetchElement> bodyElements = new HashSet<>();

        public Collection<BodyFetchElement> getBodyElements() {
            return bodyElements;
        }

        public boolean isSetSeen() {
            return setSeen;
        }

        public void add(BodyFetchElement element, boolean peek) {
            if (!peek) {
                setSeen = true;
            }
            bodyElements.add(element);
        }
    }

    /**
     * See https://tools.ietf.org/html/rfc3501#page-55 : partial
     */
    private static class Partial {
        int start;
        int size;

        int computeLength(final int contentSize) {
            final int effectiveStart = computeStart(contentSize);
            if (size > 0) {
                return Math.min(size, contentSize - effectiveStart); // Only up to max available bytes
            } else {
                // Remaining bytes starting at the origin octet
                return contentSize - effectiveStart;
            }
        }

        int computeStart(final int contentSize) {
            return Math.min(Math.max(start, 0), contentSize);
        }

        public static Partial as(int start, int size) {
            Partial p = new Partial();
            p.start = start;
            p.size = size;
            return p;
        }
    }

    private static class BodyFetchElement {
        private final String name;
        private final String sectionIdentifier;
        private final Partial partial;

        public BodyFetchElement(String name, String sectionIdentifier) {
            this(name, sectionIdentifier, null);
        }

        public BodyFetchElement(String name, String sectionIdentifier, Partial partial) {
            this.name = name;
            this.sectionIdentifier = sectionIdentifier;
            this.partial = partial;
        }

        public String getParameters() {
            return this.sectionIdentifier;
        }

        public String getResponseName() {
            return this.name;
        }

        public Partial getPartial() {
            return partial;
        }
    }

}
