package com.icegreen.greenmail.imap.commands;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class AtomSpecialValidationTest {
    private static class TestCommandParser extends CommandParser {
        @Override
        public boolean isAtomSpecial(char next) {
            return super.isAtomSpecial(next);
        }
    }

    @Test
    public void testIsAtomSpecial() {
        TestCommandParser parser = new TestCommandParser();

        // Standard specials
        assertThat(parser.isAtomSpecial('(')).isTrue();
        assertThat(parser.isAtomSpecial(')')).isTrue();
        assertThat(parser.isAtomSpecial('{')).isTrue();
        assertThat(parser.isAtomSpecial(' ')).isTrue();
        assertThat(parser.isAtomSpecial('%')).isTrue();
        assertThat(parser.isAtomSpecial('*')).isTrue();
        assertThat(parser.isAtomSpecial('"')).isTrue();
        assertThat(parser.isAtomSpecial(']')).isTrue();
        
        // Missing backslash in current implementation
        assertThat(parser.isAtomSpecial('\\')).as("Backslash should be an atom special").isTrue();

        // CTL characters (0x00-0x1F and 0x7F)
        for (int i = 0; i <= 0x1F; i++) {
            assertThat(parser.isAtomSpecial((char) i))
                .as("CTL character 0x%02X should be an atom special", i)
                .isTrue();
        }
        assertThat(parser.isAtomSpecial((char) 0x7F))
            .as("CTL character 0x7F (DEL) should be an atom special")
            .isTrue();
            
        // Valid atom chars
        assertThat(parser.isAtomSpecial('A')).isFalse();
        assertThat(parser.isAtomSpecial('z')).isFalse();
        assertThat(parser.isAtomSpecial('0')).isFalse();
        assertThat(parser.isAtomSpecial('!')).isFalse();
    }
}
