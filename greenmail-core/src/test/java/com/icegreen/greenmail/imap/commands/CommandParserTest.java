package com.icegreen.greenmail.imap.commands;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommandParserTest {
    @Test
    public void test() {
        assertThat(CommandParser.isCrOrLf('\n')).isTrue();
        assertThat(CommandParser.isCrOrLf('\r')).isTrue();
        assertThat(CommandParser.isCrOrLf('\t')).isFalse();
    }
}
