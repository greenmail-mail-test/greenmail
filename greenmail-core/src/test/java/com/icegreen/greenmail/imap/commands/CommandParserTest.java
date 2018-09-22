package com.icegreen.greenmail.imap.commands;

import com.icegreen.greenmail.imap.commands.parsers.CommandParser;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandParserTest {
    @Test
    public void test() {
        assertTrue(CommandParser.isCrOrLf('\n'));
        assertTrue(CommandParser.isCrOrLf('\r'));
        assertFalse(CommandParser.isCrOrLf('\t'));
    }
}
