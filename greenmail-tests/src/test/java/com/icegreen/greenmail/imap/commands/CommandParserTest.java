package com.icegreen.greenmail.imap.commands;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandParserTest {
    @Test
    void test() {
        assertThat(CommandParser.isCrOrLf('\n')).isTrue();
        assertThat(CommandParser.isCrOrLf('\r')).isTrue();
        assertThat(CommandParser.isCrOrLf('\t')).isFalse();
    }
}
