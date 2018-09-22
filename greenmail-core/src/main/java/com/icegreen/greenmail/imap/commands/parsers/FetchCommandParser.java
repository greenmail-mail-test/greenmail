package com.icegreen.greenmail.imap.commands.parsers;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.imap.commands.parsers.fetch.BodyFetchElement;
import com.icegreen.greenmail.imap.commands.parsers.fetch.FetchRequest;
import com.icegreen.greenmail.imap.commands.parsers.fetch.Partial;

public class FetchCommandParser extends CommandParser {

    public FetchRequest fetchRequest(ImapRequestLineReader request) throws ProtocolException {
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
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
            } else if ("FULL".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
                fetch.setEnvelope(true);
                fetch.setBody(true);
            } else if ("ALL".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
                fetch.setEnvelope(true);
            } else if ("FLAGS".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
            } else if ("RFC822.SIZE".equalsIgnoreCase(name)) {
                fetch.setSize(true);
            } else if ("ENVELOPE".equalsIgnoreCase(name)) {
                fetch.setEnvelope(true);
            } else if ("INTERNALDATE".equalsIgnoreCase(name)) {
                fetch.setInternalDate(true);
            } else if ("BODY".equalsIgnoreCase(name)) {
                fetch.setBody(true);
            } else if ("BODYSTRUCTURE".equalsIgnoreCase(name)) {
                fetch.setBodyStructure(true);
            } else if ("UID".equalsIgnoreCase(name)) {
                fetch.setUid(true);
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
            request.dumpLine();
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