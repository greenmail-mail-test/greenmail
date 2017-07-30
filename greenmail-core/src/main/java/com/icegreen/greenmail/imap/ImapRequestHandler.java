/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.imap.commands.CommandParser;
import com.icegreen.greenmail.imap.commands.ImapCommand;
import com.icegreen.greenmail.imap.commands.ImapCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public final class ImapRequestHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private ImapCommandFactory imapCommands = new ImapCommandFactory();
    private CommandParser parser = new CommandParser();
    private static final String REQUEST_SYNTAX = "Protocol Error: Was expecting <tag SPACE command [arguments]>";

    /**
     * This method parses POP3 commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     *
     * @return whether additional commands are expected.
     */
    public boolean handleRequest(InputStream input,
                                 OutputStream output,
                                 ImapSession session)
            throws ProtocolException {
        ImapRequestLineReader request = new ImapRequestLineReader(input, output);
        try {
            request.nextChar();
        } catch (ProtocolException e) {
            return false;
        }

        ImapResponse response = new ImapResponse(output);

        doProcessRequest(request, response, session);

        // Consume the rest of the line, throwing away any extras. This allows us
        // to clean up after a protocol error.
        request.consumeLine();

        return true;
    }

    private void doProcessRequest(ImapRequestLineReader request,
                                  ImapResponse response,
                                  ImapSession session) {
        String tag;
        String commandName;

        try {
            tag = parser.tag(request);
        } catch (ProtocolException e) {
            response.badResponse(REQUEST_SYNTAX);
            return;
        }
        response.setTag(tag);

        try {
            commandName = parser.atom(request);
        } catch (ProtocolException e) {
            response.commandError(REQUEST_SYNTAX);
            return;
        }

        log.debug("C: tag={}, command={}", tag, commandName);

        ImapCommand command = imapCommands.getCommand(commandName);
        if (command == null) {
            log.error("Command '{}' not valid", commandName);
            response.commandError("Invalid command.");
            return;
        }

        if (!command.validForState(session.getState())) {
            log.error("Command '{}' not valid in this state {}", commandName, session.getState());
            response.commandFailed(command, "Command not valid in this state");
            return;
        }

        command.process(request, response, session);
    }


}
