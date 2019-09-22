/*
 * Copyright (c) 2014 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the Apache license 2.0
 * This file has been modified by the copyright holder.
 * Original file can be found at http://james.apache.org
 */
package com.icegreen.greenmail.imap;

import com.icegreen.greenmail.imap.commands.ImapCommand;
import com.icegreen.greenmail.store.MessageFlags;
import com.icegreen.greenmail.util.EncodingUtil;
import com.icegreen.greenmail.util.InternetPrintWriter;

import javax.mail.Flags;
import java.io.OutputStream;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class ImapResponse implements ImapConstants {
    private InternetPrintWriter writer;
    private String tag = UNTAGGED;

    public ImapResponse(OutputStream output) {
        this.writer = InternetPrintWriter.createForEncoding(output, true, EncodingUtil.CHARSET_EIGHT_BIT_ENCODING);
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Writes a standard tagged OK response on completion of a command.
     * Response is written as:
     * <pre>     a01 OK COMMAND_NAME completed.</pre>
     *
     * @param command The ImapCommand which was completed.
     */
    public void commandComplete(ImapCommand command) {
        commandComplete(command, null);
    }

    /**
     * Writes a standard tagged OK response on completion of a command,
     * with a response code (eg READ-WRITE)
     * Response is written as:
     * <pre>     a01 OK [responseCode] COMMAND_NAME completed.</pre>
     *
     * @param command      The ImapCommand which was completed.
     * @param responseCode A string response code to send to the client.
     */
    public void commandComplete(ImapCommand command, String responseCode) {
        tag();
        message(OK);
        responseCode(responseCode);
        commandName(command);
        message("completed.");
        end();
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is written as:
     * <pre>     a01 NO COMMAND_NAME failed. <reason></pre>
     *
     * @param command The ImapCommand which failed.
     * @param reason  A message describing why the command failed.
     */
    public void commandFailed(ImapCommand command, String reason) {
        commandFailed(command, null, reason);
    }

    /**
     * Writes a standard NO response on command failure, together with a
     * descriptive message.
     * Response is written as:
     * <pre>     a01 NO [responseCode] COMMAND_NAME failed. <reason></pre>
     *
     * @param command      The ImapCommand which failed.
     * @param responseCode The Imap response code to send.
     * @param reason       A message describing why the command failed.
     */
    public void commandFailed(ImapCommand command,
                              String responseCode,
                              String reason) {
        tag();
        message(NO);
        responseCode(responseCode);
        commandName(command);
        message("failed.");
        message(reason);
        end();
    }

    /**
     * Writes a standard BAD response on command error, together with a
     * descriptive message.
     * Response is written as:
     * <pre>     a01 BAD <message></pre>
     *
     * @param message The descriptive error message.
     */
    public void commandError(String message) {
        tag();
        message(BAD);
        message(message);
        end();
    }

    /**
     * Writes a standard untagged BAD response, together with a descriptive message.
     */
    public void badResponse(String message) {
        untagged();
        message(BAD);
        message(message);
        end();
    }

    /**
     * Writes an untagged OK response, with the supplied response code,
     * and an optional message.
     *
     * @param responseCode The response code, included in [].
     * @param message      The message to follow the []
     */
    public void okResponse(String responseCode, String message) {
        untagged();
        message(OK);
        responseCode(responseCode);
        message(message);
        end();
    }

    public void flagsResponse(Flags flags) {
        untagged();
        message("FLAGS");
        message(MessageFlags.format(flags));
        end();
    }

    public void existsResponse(int count) {
        untagged();
        message(count);
        message("EXISTS");
        end();
    }

    public void recentResponse(int count) {
        untagged();
        message(count);
        message("RECENT");
        end();
    }

    public void expungeResponse(int msn) {
        untagged();
        message(msn);
        message("EXPUNGE");
        end();
    }

    public void fetchResponse(int msn, String msgData) {
        untagged();
        message(msn);
        message("FETCH");
        message('(' + msgData + ')');
        end();
    }

    public void commandResponse(ImapCommand command, String message) {
        untagged();
        commandName(command);
        message(message);
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * request tag.
     *
     * @param message The message to write to the client.
     */
    public void taggedResponse(String message) {
        tag();
        message(message);
        end();
    }

    /**
     * Writes the message provided to the client, prepended with the
     * untagged marker "*".
     *
     * @param message The message to write to the client.
     */
    public void untaggedResponse(String message) {
        untagged();
        message(message);
        end();
    }

    public void byeResponse(String message) {
        untaggedResponse(BYE + SP + message);
    }

    private void untagged() {
        writer.print(UNTAGGED);
    }

    private void tag() {
        writer.print(tag);
    }

    private void commandName(ImapCommand command) {
        String name = command.getName();
        writer.print(SP);
        writer.print(name);
    }

    private void message(String message) {
        if (message != null && !message.isEmpty()) {
            writer.print(SP);
            writer.print(message);
        }
    }

    private void message(int number) {
        writer.print(SP);
        writer.print(number);
    }

    private void responseCode(String responseCode) {
        if (responseCode != null) {
            writer.print(" [");
            writer.print(responseCode);
            writer.print("]");
        }
    }

    private void end() {
        writer.println();
        writer.flush();
    }

    public void permanentFlagsResponse(Flags flags) {
        untagged();
        message(OK);
        responseCode("PERMANENTFLAGS " + MessageFlags.format(flags));
        end();
    }
}
