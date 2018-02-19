package com.icegreen.greenmail.imap.commands.parsers;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.util.GreenMailUtil;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.Date;

public class AppendCommandParser extends CommandParser {
    /**
     * If the next character in the request is a '(', tries to read
     * a "flag_list" argument from the request. If not, returns a
     * MessageFlags with no flags set.
     */
    public Flags optionalAppendFlags(ImapRequestLineReader request)
            throws ProtocolException {
        char next = request.nextWordChar();
        if (next == '(') {
            return flagList(request);
        } else {
            return null;
        }
    }

    /**
     * If the next character in the request is a '"', tries to read
     * a DateTime argument. If not, returns null.
     */
    public Date optionalDateTime(ImapRequestLineReader request)
            throws ProtocolException {
        char next = request.nextWordChar();
        if (next == '"') {
            return dateTime(request);
        } else {
            return null;
        }
    }

    /**
     * Reads a MimeMessage encoded as a string literal from the request.
     * TODO shouldn't need to read as a string and write out bytes
     * use FixedLengthInputStream instead. Hopefully it can then be dynamic.
     *
     * @param request The Imap APPEND request
     * @return A MimeMessage read off the request.
     */
    public MimeMessage mimeMessage(ImapRequestLineReader request)
            throws ProtocolException {
        request.nextWordChar();
        byte[] mail = consumeLiteralAsBytes(request);

        try {
            return GreenMailUtil.newMimeMessage(new ByteArrayInputStream(mail));
        } catch (Exception e) {
            throw new ProtocolException("Can not create new mime message", e);
        }
    }

}