package com.icegreen.greenmail.imap.commands.parsers;

import com.icegreen.greenmail.imap.ImapRequestLineReader;
import com.icegreen.greenmail.imap.ProtocolException;
import com.icegreen.greenmail.imap.commands.StatusCommand;
import com.icegreen.greenmail.imap.commands.parsers.status.StatusDataItems;

public class StatusCommandParser extends CommandParser {
    public StatusDataItems statusDataItems(ImapRequestLineReader request)
            throws ProtocolException {
        StatusDataItems items = new StatusDataItems();

        request.nextWordChar();
        consumeChar(request, '(');
        CharacterValidator validator = new NoopCharValidator();
        String nextWord = consumeWord(request, validator);
        while (!nextWord.endsWith(")")) {
            addItem(nextWord, items);
            nextWord = consumeWord(request, validator);
        }
        // Got the closing ")", may be attached to a word.
        if (nextWord.length() > 1) {
            addItem(nextWord.substring(0, nextWord.length() - 1), items);
        }

        return items;
    }

    private void addItem(String nextWord, StatusDataItems items)
            throws ProtocolException {
        if (nextWord.equals(StatusCommand.MESSAGES)) {
            items.setMessages(true);
        } else if (nextWord.equals(StatusCommand.RECENT)) {
            items.setRecent(true);
        } else if (nextWord.equals(StatusCommand.UIDNEXT)) {
            items.setUidNext(true);
        } else if (nextWord.equals(StatusCommand.UIDVALIDITY)) {
            items.setUidValidity(true);
        } else if (nextWord.equals(StatusCommand.UNSEEN)) {
            items.setUnseen(true);
        } else {
            throw new ProtocolException("Unknown status item: '" + nextWord + '\'');
        }
    }
}
