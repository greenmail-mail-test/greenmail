/*
 * =============================================================================
 * 
 *   Copyright (c) 2014, The UNBESCAPE team (http://www.unbescape.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package com.icegreen.greenmail.util.strings;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;


/**
 * <p>
 *   Internal class in charge of performing the real escape/unescape operations.
 * </p>
 *
 * @author Daniel Fern&aacute;ndez
 *
 * @since 1.0.0
 *
 */
final class JavaEscapeUtil {



    /*
     * JAVA ESCAPE/UNESCAPE OPERATIONS
     * -------------------------------
     *
     *   See: http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html
     *        http://arity23.blogspot.com.es/2013/04/secrets-of-scala-lexer-1-uuuuunicode.html
     *
     *   (Note that, in the following examples, and in order to avoid escape problems during the compilation
     *    of this class, the backslash symbol is replaced by '%')
     *
     *   - SINGLE ESCAPE CHARACTERS (SECs):
     *        U+0008 -> %b
     *        U+0009 -> %t
     *        U+000A -> %n
     *        U+000C -> %f
     *        U+000D -> %r
     *        U+0022 -> %"
     *        U+0027 -> %' [ NOT USED IN ESCAPE OF STRINGS IF LEVEL < 3 ]
     *        U+005C -> %%
     *   - UNICODE ESCAPE [UHEXA]
     *        Characters <= U+FFFF: %u????
     *        Characters > U+FFFF : %u????%u???? (surrogate character pair)
     *   - OCTAL ESCAPE: %377 [NOT USED IN ESCAPE - Use is not recommended by the JLS, exists for C compatibility]
     *
     *
     *   ------------------------
     *
     *   NOTE: The way Unicode Escapes work in Java is different to other languages like e.g. JavaScript. In Java,
     *         these UHEXA escapes are processed by the compiler itself, and therefore resolved before any other
     *         type of escapes. Besides, UHEXA escapes can appear anywhere in the code, not only String literals.
     *         This means that, while in JavaScript 'a\u005Cna' would be displayed as 'a\na', in Java "a\u005Cna"
     *         would in fact be displayed in two lines: 'a'+LF+'a'.
     *         Going even further, this is perfectly valid Java code:
     *
     *             final String hello = \u0022Hello, World!\u0022;
     *
     *         Also, Java allows to write any number of 'u' characters in this type of escapes, like \uu00E1 or even
     *         \uuuuuuuuu00E1. This is so in order to enable legacy compatibility with older code-processing tools
     *         that didn't support Unicode processing at all, which would fail when finding an Unicode escape
     *         like \u00E1, but not \uu00E1 (because they would consider backslash+'u' as the escape).
     *         So yes, this is valid Java code too:
     *
     *             final String hello = \uuuuuuuu0022Hello, World!\u0022;
     *
     *         In order to correctly unescape Java UHEXA escapes like "a\u005Cna", Unbescape will perform a two-pass
     *         process so that all unicode escapes are processed in the first pass, and then the single escape
     *         characters and octal escapes in the second pass.
     *
     *   ------------------------
     *
     *   NOTE: Unbescape does not define a 'type' for Java escaping (just a level) because, given what's explained
     *         above about Unicode Escapes, there is not the possibility to choose whether we want to escape, for
     *         example, '\t' (U+0009) as a SEC ('\t') or as a Unicode Escape ('\u0009'). Given Unicode Escapes are
     *         processed by the compiler, using it instead of the SEC would really insert a tab character inside our
     *         source code, which is not equivalent to the '\t' syntax (and might be actually invalid).
     *
     *   ------------------------
     *
     */



    /*
     * Prefixes defined for use in escape and unescape operations
     */
    private static final char ESCAPE_PREFIX = '\\';
    private static final char ESCAPE_UHEXA_PREFIX2 = 'u';
    private static final char[] ESCAPE_UHEXA_PREFIX = "\\u".toCharArray();

    /*
     * Small utility char arrays for hexadecimal conversion.
     */
    private static char[] HEXA_CHARS_UPPER = "0123456789ABCDEF".toCharArray();
    private static char[] HEXA_CHARS_LOWER = "0123456789abcdef".toCharArray();


    /*
     * Structures for holding the Single Escape Characters
     */
    private static int SEC_CHARS_LEN = '\\' + 1; // 0x5C + 1 = 0x5D
    private static char SEC_CHARS_NO_SEC = '*';
    private static char[] SEC_CHARS;

    /*
     * Structured for holding the 'escape level' assigned to chars (not codepoints) up to ESCAPE_LEVELS_LEN.
     * - The last position of the ESCAPE_LEVELS array will be used for determining the level of all
     *   codepoints >= (ESCAPE_LEVELS_LEN - 1)
     */
    private static final char ESCAPE_LEVELS_LEN = 0x9f + 2; // Last relevant char to be indexed is 0x9f
    private static final byte[] ESCAPE_LEVELS;



    static {

        /*
         * Initialize Single Escape Characters
         */
        SEC_CHARS = new char[SEC_CHARS_LEN];
        Arrays.fill(SEC_CHARS,SEC_CHARS_NO_SEC);
        SEC_CHARS[0x08] = 'b';
        SEC_CHARS[0x09] = 't';
        SEC_CHARS[0x0A] = 'n';
        SEC_CHARS[0x0C] = 'f';
        SEC_CHARS[0x0D] = 'r';
        SEC_CHARS[0x22] = '"';
        // Escaping the apostrophe is only required in character literals, but we are escaping
        // string literals, so we don't really need this escape if level < 3
        SEC_CHARS[0x27] = '\'';
        SEC_CHARS[0x5C] = '\\';



        /*
         * Initialization of escape levels.
         * Defined levels :
         *
         *    - Level 1 : Basic escape set
         *    - Level 2 : Basic escape set plus all non-ASCII
         *    - Level 3 : All non-alphanumeric characters
         *    - Level 4 : All characters
         *
         */
        ESCAPE_LEVELS = new byte[ESCAPE_LEVELS_LEN];

        /*
         * Everything is level 3 unless contrary indication.
         */
        Arrays.fill(ESCAPE_LEVELS, (byte)3);

        /*
         * Everything non-ASCII is level 2 unless contrary indication.
         */
        for (char c = 0x80; c < ESCAPE_LEVELS_LEN; c++) {
            ESCAPE_LEVELS[c] = 2;
        }

        /*
         * Alphanumeric characters are level 4.
         */
        for (char c = 'A'; c <= 'Z'; c++) {
            ESCAPE_LEVELS[c] = 4;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            ESCAPE_LEVELS[c] = 4;
        }
        for (char c = '0'; c <= '9'; c++) {
            ESCAPE_LEVELS[c] = 4;
        }

        /*
         * Simple Escape Character will be level 1 (always escaped)
         */
        ESCAPE_LEVELS[0x08] = 1;
        ESCAPE_LEVELS[0x09] = 1;
        ESCAPE_LEVELS[0x0A] = 1;
        ESCAPE_LEVELS[0x0C] = 1;
        ESCAPE_LEVELS[0x0D] = 1;
        ESCAPE_LEVELS[0x22] = 1;
        // Escaping the apostrophe is only required in character literals, but we are escaping
        // string literals, so we don't really need this escape if level < 3
        ESCAPE_LEVELS[0x27] = 3;
        ESCAPE_LEVELS[0x5C] = 1;

        /*
         * Java defines one ranges of non-displayable, control characters: U+0000 to U+001F.
         * Additionally, the U+007F to U+009F range is also escaped (which is allowed).
         */
        for (char c = 0x00; c <= 0x1F; c++) {
            ESCAPE_LEVELS[c] = 1;
        }
        for (char c = 0x7F; c <= 0x9F; c++) {
            ESCAPE_LEVELS[c] = 1;
        }

    }



    private JavaEscapeUtil() {
        super();
    }




    static char[] toUHexa(final int codepoint) {
        final char[] result = new char[4];
        result[3] = HEXA_CHARS_UPPER[codepoint % 0x10];
        result[2] = HEXA_CHARS_UPPER[(codepoint >>> 4) % 0x10];
        result[1] = HEXA_CHARS_UPPER[(codepoint >>> 8) % 0x10];
        result[0] = HEXA_CHARS_UPPER[(codepoint >>> 12) % 0x10];
        return result;
    }



    /*
     * Perform an escape operation, based on String, according to the specified level.
     */
    static String escape(final String text, final JavaEscapeLevel escapeLevel) {

        if (text == null) {
            return null;
        }

        final int level = escapeLevel.getEscapeLevel();

        StringBuilder strBuilder = null;

        final int offset = 0;
        final int max = text.length();

        int readOffset = offset;

        for (int i = offset; i < max; i++) {

            final int codepoint = Character.codePointAt(text, i);


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (codepoint <= (ESCAPE_LEVELS_LEN - 2) && level < ESCAPE_LEVELS[codepoint]) {
                continue;
            }

            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (codepoint > (ESCAPE_LEVELS_LEN - 2) && level < ESCAPE_LEVELS[ESCAPE_LEVELS_LEN - 1]) {

                if (Character.charCount(codepoint) > 1) {
                    // This is to compensate that we are actually escaping two char[] positions with a single codepoint.
                    i++;
                }

                continue;

            }


            /*
             * At this point we know for sure we will need some kind of escape, so we
             * can increase the offset and initialize the string builder if needed, along with
             * copying to it all the contents pending up to this point.
             */

            if (strBuilder == null) {
                strBuilder = new StringBuilder(max + 20);
            }

            if (i - readOffset > 0) {
                strBuilder.append(text, readOffset, i);
            }

            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually reading two char[] positions with a single codepoint.
                i++;
            }

            readOffset = i + 1;


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Peform the real escape, attending the different combinations of SECs and UHEXA
             *
             * -----------------------------------------------------------------------------------------
             */

            if (codepoint < SEC_CHARS_LEN) {
                // We will try to use a SEC

                final char sec = SEC_CHARS[codepoint];

                if (sec != SEC_CHARS_NO_SEC) {
                    // SEC found! just write it and go for the next char
                    strBuilder.append(ESCAPE_PREFIX);
                    strBuilder.append(sec);
                    continue;
                }

            }

            /*
             * No SEC-escape was possible, so we need uhexa escape.
             */

            if (Character.charCount(codepoint) > 1) {
                final char[] codepointChars = Character.toChars(codepoint);
                strBuilder.append(ESCAPE_UHEXA_PREFIX);
                strBuilder.append(toUHexa(codepointChars[0]));
                strBuilder.append(ESCAPE_UHEXA_PREFIX);
                strBuilder.append(toUHexa(codepointChars[1]));
                continue;
            }

            strBuilder.append(ESCAPE_UHEXA_PREFIX);
            strBuilder.append(toUHexa(codepoint));

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: return the original String object if no escape was actually needed. Otherwise
         *                 append the remaining unescaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (strBuilder == null) {
            return text;
        }

        if (max - readOffset > 0) {
            strBuilder.append(text, readOffset, max);
        }

        return strBuilder.toString();

    }





    /*
     * Perform an escape operation, based on char[], according to the specified level.
     */
    static void escape(final char[] text, final int offset, final int len, final Writer writer,
                       final JavaEscapeLevel escapeLevel)
                       throws IOException {

        if (text == null || text.length == 0) {
            return;
        }

        final int level = escapeLevel.getEscapeLevel();

        final int max = (offset + len);

        int readOffset = offset;

        for (int i = offset; i < max; i++) {

            final int codepoint = Character.codePointAt(text, i);


            /*
             * Shortcut: most characters will be ASCII/Alphanumeric, and we won't need to do anything at
             * all for them
             */
            if (codepoint <= (ESCAPE_LEVELS_LEN - 2) && level < ESCAPE_LEVELS[codepoint]) {
                continue;
            }

            /*
             * Shortcut: we might not want to escape non-ASCII chars at all either.
             */
            if (codepoint > (ESCAPE_LEVELS_LEN - 2) && level < ESCAPE_LEVELS[ESCAPE_LEVELS_LEN - 1]) {

                if (Character.charCount(codepoint) > 1) {
                    // This is to compensate that we are actually escaping two char[] positions with a single codepoint.
                    i++;
                }

                continue;

            }


            /*
             * At this point we know for sure we will need some kind of escape, so we
             * copy all the contents pending up to this point.
             */

            if (i - readOffset > 0) {
                writer.write(text, readOffset, (i - readOffset));
            }

            if (Character.charCount(codepoint) > 1) {
                // This is to compensate that we are actually reading two char[] positions with a single codepoint.
                i++;
            }

            readOffset = i + 1;


            /*
             * -----------------------------------------------------------------------------------------
             *
             * Peform the real escape, attending the different combinations of SECs and UHEXA
             *
             * -----------------------------------------------------------------------------------------
             */

            if (codepoint < SEC_CHARS_LEN) {
                // We will try to use a SEC

                final char sec = SEC_CHARS[codepoint];

                if (sec != SEC_CHARS_NO_SEC) {
                    // SEC found! just write it and go for the next char
                    writer.write(ESCAPE_PREFIX);
                    writer.write(sec);
                    continue;
                }

            }

            /*
             * No SEC-escape was possible, so we need uhexa escape.
             */

            if (Character.charCount(codepoint) > 1) {
                final char[] codepointChars = Character.toChars(codepoint);
                writer.write(ESCAPE_UHEXA_PREFIX);
                writer.write(toUHexa(codepointChars[0]));
                writer.write(ESCAPE_UHEXA_PREFIX);
                writer.write(toUHexa(codepointChars[1]));
                continue;
            }

            writer.write(ESCAPE_UHEXA_PREFIX);
            writer.write(toUHexa(codepoint));

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: append the remaining unescaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (max - readOffset > 0) {
            writer.write(text, readOffset, (max - readOffset));
        }

    }








    /*
     * This methods (the two versions) are used instead of Integer.parseInt(str,radix) in order to avoid the need
     * to create substrings of the text being unescaped to feed such method.
     * -  No need to check all chars are within the radix limits - reference parsing code will already have done so.
     */

    static int parseIntFromReference(final String text, final int start, final int end, final int radix) {
        int result = 0;
        for (int i = start; i < end; i++) {
            final char c = text.charAt(i);
            int n = -1;
            for (int j = 0; j < HEXA_CHARS_UPPER.length; j++) {
                if (c == HEXA_CHARS_UPPER[j] || c == HEXA_CHARS_LOWER[j]) {
                    n = j;
                    break;
                }
            }
            result = (radix * result) + n;
        }
        return result;
    }

    static int parseIntFromReference(final char[] text, final int start, final int end, final int radix) {
        int result = 0;
        for (int i = start; i < end; i++) {
            final char c = text[i];
            int n = -1;
            for (int j = 0; j < HEXA_CHARS_UPPER.length; j++) {
                if (c == HEXA_CHARS_UPPER[j] || c == HEXA_CHARS_LOWER[j]) {
                    n = j;
                    break;
                }
            }
            result = (radix * result) + n;
        }
        return result;
    }





    static boolean isOctalEscape(final String text, final int start, final int end) {

        if (start >= end) {
            return false;
        }

        final char c1 = text.charAt(start);
        if (c1 < '0' || c1 > '7') {
            return false;
        }

        if (start + 1 >= end) {
            return (c1 != '0'); // It would not be an octal escape, but the U+0000 escape sequence.
        }

        final char c2 = text.charAt(start + 1);
        if (c2 < '0' || c2 > '7') {
            return (c1 != '0'); // It would not be an octal escape, but the U+0000 escape sequence.
        }

        if (start + 2 >= end) {
            return (c1 != '0' || c2 != '0'); // It would not be an octal escape, but the U+0000 escape sequence + '0'.
        }

        final char c3 = text.charAt(start + 2);
        if (c3 < '0' || c3 > '7') {
            return (c1 != '0' || c2 != '0'); // It would not be an octal escape, but the U+0000 escape sequence + '0'.
        }

        return (c1 != '0' || c2 != '0' || c3 != '0'); // Check it's not U+0000 (escaped) + '00'

    }


    static boolean isOctalEscape(final char[] text, final int start, final int end) {

        if (start >= end) {
            return false;
        }

        final char c1 = text[start];
        if (c1 < '0' || c1 > '7') {
            return false;
        }

        if (start + 1 >= end) {
            return (c1 != '0'); // It would not be an octal escape, but the U+0000 escape sequence.
        }

        final char c2 = text[start + 1];
        if (c2 < '0' || c2 > '7') {
            return (c1 != '0'); // It would not be an octal escape, but the U+0000 escape sequence.
        }

        if (start + 2 >= end) {
            return (c1 != '0' || c2 != '0'); // It would not be an octal escape, but the U+0000 escape sequence + '0'.
        }

        final char c3 = text[start + 2];
        if (c3 < '0' || c3 > '7') {
            return (c1 != '0' || c2 != '0'); // It would not be an octal escape, but the U+0000 escape sequence + '0'.
        }

        return (c1 != '0' || c2 != '0' || c3 != '0'); // Check it's not U+0000 (escaped) + '00'

    }





    /*
     * Perform the first step unescape operation based on String.
     */
    static String unicodeUnescape(final String text) {

        if (text == null) {
            return null;
        }

        StringBuilder strBuilder = null;

        final int offset = 0;
        final int max = text.length();

        int readOffset = offset;
        int referenceOffset = offset;

        for (int i = offset; i < max; i++) {

            final char c = text.charAt(i);

            /*
             * Check the need for an unescape operation at this point
             */

            if (c != ESCAPE_PREFIX || (i + 1) >= max) {
                continue;
            }

            int codepoint = -1;

            if (c == ESCAPE_PREFIX) {

                final char c1 = text.charAt(i + 1);

                if (c1 == ESCAPE_UHEXA_PREFIX2) {
                    // This can be a uhexa escape, we need exactly four more characters

                    int f = i + 2;
                    // First, discard any additional 'u' characters, which are allowed
                    while (f < max) {
                        final char cf = text.charAt(f);
                        if (cf != ESCAPE_UHEXA_PREFIX2) {
                            break;
                        }
                        f++;
                    }
                    int s = f;
                    // Parse the hexadecimal digits
                    while (f < (s + 4) && f < max) {
                        final char cf = text.charAt(f);
                        if (!((cf >= '0' && cf <= '9') || (cf >= 'A' && cf <= 'F') || (cf >= 'a' && cf <= 'f'))) {
                            break;
                        }
                        f++;
                    }

                    if ((f - s) < 4) {
                        // We weren't able to consume the required four hexa chars, leave it as slash+'u', which
                        // is invalid, and let the corresponding Java parser fail.
                        i++;
                        continue;
                    }

                    codepoint = parseIntFromReference(text, s, f, 16);

                    // Fast-forward to the first char after the parsed codepoint
                    referenceOffset = f - 1;

                    // Don't continue here, just let the unescape code below do its job

                } else {

                    // Other escape sequences will not be processed in this unescape step.
                    i++;
                    continue;

                }

            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * can increase the offset and initialize the string builder if needed, along with
             * copying to it all the contents pending up to this point.
             */

            if (strBuilder == null) {
                strBuilder = new StringBuilder(max + 5);
            }

            if (i - readOffset > 0) {
                strBuilder.append(text, readOffset, i);
            }

            i = referenceOffset;
            readOffset = i + 1;

            /*
             * --------------------------
             *
             * Peform the real unescape
             *
             * --------------------------
             */

            if (codepoint > '\uFFFF') {
                strBuilder.append(Character.toChars(codepoint));
            } else {
                strBuilder.append((char)codepoint);
            }

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: return the original String object if no unescape was actually needed. Otherwise
         *                 append the remaining escaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (strBuilder == null) {
            return text;
        }

        if (max - readOffset > 0) {
            strBuilder.append(text, readOffset, max);
        }

        return strBuilder.toString();

    }



    /*
     * Determine whether we will need unicode unescape or not, so that we avoid creating a writer object
     * if it is not needed.
     */
    static boolean requiresUnicodeUnescape(final char[] text, final int offset, final int len) {

        if (text == null) {
            return false;
        }

        final int max = (offset + len);

        for (int i = offset; i < max; i++) {

            final char c = text[i];

            if (c != ESCAPE_PREFIX || (i + 1) >= max) {
                continue;
            }

            if (c == ESCAPE_PREFIX) {

                final char c1 = text[i + 1];

                if (c1 == ESCAPE_UHEXA_PREFIX2) {
                    // This can be a uhexa escape
                    return true;
                }

            }

        }

        return false;

    }



    /*
     * Perform the first step unescape operation based on char[].
     *
     * NOTE: We should only be calling this if we already executed requiresUnicodeEscape and it returned true!
     */
    static void unicodeUnescape(final char[] text, final int offset, final int len, final Writer writer)
                                throws IOException {

        if (text == null) {
            return;
        }

        final int max = (offset + len);

        int readOffset = offset;
        int referenceOffset = offset;

        for (int i = offset; i < max; i++) {

            final char c = text[i];

            /*
             * Check the need for an unescape operation at this point
             */

            if (c != ESCAPE_PREFIX || (i + 1) >= max) {
                continue;
            }

            int codepoint = -1;

            if (c == ESCAPE_PREFIX) {

                final char c1 = text[i + 1];

                if (c1 == ESCAPE_UHEXA_PREFIX2) {
                    // This can be a uhexa escape, we need exactly four more characters

                    int f = i + 2;
                    // First, discard any additional 'u' characters, which are allowed
                    while (f < max) {
                        final char cf = text[f];
                        if (cf != ESCAPE_UHEXA_PREFIX2) {
                            break;
                        }
                        f++;
                    }
                    int s = f;
                    // Parse the hexadecimal digits
                    while (f < (s + 4) && f < max) {
                        final char cf = text[f];
                        if (!((cf >= '0' && cf <= '9') || (cf >= 'A' && cf <= 'F') || (cf >= 'a' && cf <= 'f'))) {
                            break;
                        }
                        f++;
                    }

                    if ((f - s) < 4) {
                        // We weren't able to consume the required four hexa chars, leave it as slash+'u', which
                        // is invalid, and let the corresponding Java parser fail.
                        i++;
                        continue;
                    }

                    codepoint = parseIntFromReference(text, s, f, 16);

                    // Fast-forward to the first char after the parsed codepoint
                    referenceOffset = f - 1;

                    // Don't continue here, just let the unescape code below do its job

                } else {

                    // Other escape sequences will not be processed in this unescape step.
                    i++;
                    continue;

                }

            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * can copy all the contents pending up to this point.
             */

            if (i - readOffset > 0) {
                writer.write(text, readOffset, (i - readOffset));
            }

            i = referenceOffset;
            readOffset = i + 1;

            /*
             * --------------------------
             *
             * Peform the real unescape
             *
             * --------------------------
             */

            if (codepoint > '\uFFFF') {
                writer.write(Character.toChars(codepoint));
            } else {
                writer.write((char) codepoint);
            }

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: append the remaining escaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (max - readOffset > 0) {
            writer.write(text, readOffset, (max - readOffset));
        }

    }






    /*
     * Perform an unescape operation based on String.
     */
    static String unescape(final String text) {

        if (text == null) {
            return null;
        }

        // Will be exactly the same object if no unicode escape was needed
        final String unicodeEscapedText = unicodeUnescape(text);

        StringBuilder strBuilder = null;

        final int offset = 0;
        final int max = unicodeEscapedText.length();

        int readOffset = offset;
        int referenceOffset = offset;

        for (int i = offset; i < max; i++) {

            final char c = unicodeEscapedText.charAt(i);

            /*
             * Check the need for an unescape operation at this point
             */

            if (c != ESCAPE_PREFIX || (i + 1) >= max) {
                continue;
            }

            int codepoint = -1;

            if (c == ESCAPE_PREFIX) {

                final char c1 = unicodeEscapedText.charAt(i + 1);

                switch (c1) {
                    case '0': if (!isOctalEscape(unicodeEscapedText,i + 1,max)) { codepoint = 0x00; referenceOffset = i + 1; }; break;
                    case 'b': codepoint = 0x08; referenceOffset = i + 1; break;
                    case 't': codepoint = 0x09; referenceOffset = i + 1; break;
                    case 'n': codepoint = 0x0A; referenceOffset = i + 1; break;
                    case 'f': codepoint = 0x0C; referenceOffset = i + 1; break;
                    case 'r': codepoint = 0x0D; referenceOffset = i + 1; break;
                    case '"': codepoint = 0x22; referenceOffset = i + 1; break;
                    case '\'': codepoint = 0x27; referenceOffset = i + 1; break;
                    case '\\': codepoint = 0x5C; referenceOffset = i + 1; break;
                }

                if (codepoint == -1) {

                    if (c1 >= '0' && c1 <= '7') {
                        // This can be a octal escape, we need at least 1 more char, and up to 3 more.

                        int f = i + 2;
                        while (f < (i + 4) && f < max) { // We need only a max of two more chars
                            final char cf = unicodeEscapedText.charAt(f);
                            if (!(cf >= '0' && cf <= '7')) {
                                break;
                            }
                            f++;
                        }

                        codepoint = parseIntFromReference(unicodeEscapedText, i + 1, f, 8);

                        if (codepoint > 0xFF) {
                            // Maximum octal escape char is FF. Ignore the last digit
                            codepoint = parseIntFromReference(unicodeEscapedText, i + 1, f - 1, 8);
                            referenceOffset = f - 2;
                        } else {
                            referenceOffset = f - 1;
                        }

                        // Don't continue here, just let the unescape code below do its job

                    } else {

                        // Other escape sequences are not allowed by Java. So we leave it as is
                        // and expect the corresponding Java parser to fail.
                        i++;
                        continue;

                    }

                }

            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * can increase the offset and initialize the string builder if needed, along with
             * copying to it all the contents pending up to this point.
             */

            if (strBuilder == null) {
                strBuilder = new StringBuilder(max + 5);
            }

            if (i - readOffset > 0) {
                strBuilder.append(unicodeEscapedText, readOffset, i);
            }

            i = referenceOffset;
            readOffset = i + 1;

            /*
             * --------------------------
             *
             * Peform the real unescape
             *
             * --------------------------
             */

            if (codepoint > '\uFFFF') {
                strBuilder.append(Character.toChars(codepoint));
            } else {
                strBuilder.append((char)codepoint);
            }

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: return the original String object if no unescape was actually needed. Otherwise
         *                 append the remaining escaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (strBuilder == null) {
            return unicodeEscapedText;
        }

        if (max - readOffset > 0) {
            strBuilder.append(unicodeEscapedText, readOffset, max);
        }

        return strBuilder.toString();

    }






    /*
     * Perform an unescape operation based on char[].
     */
    static void unescape(final char[] text, final int offset, final int len, final Writer writer)
                         throws IOException {

        if (text == null) {
            return;
        }

        char[] unicodeEscapedText = text;
        int unicodeEscapedOffset = offset;
        int unicodeEscapedLen = len;
        if (requiresUnicodeUnescape(text, offset, len)) {
            final CharArrayWriter charArrayWriter = new CharArrayWriter(len + 2);
            unicodeUnescape(text, offset, len, charArrayWriter);
            unicodeEscapedText = charArrayWriter.toCharArray();
            unicodeEscapedOffset = 0;
            unicodeEscapedLen = unicodeEscapedText.length;
        }

        final int max = (unicodeEscapedOffset + unicodeEscapedLen);

        int readOffset = unicodeEscapedOffset;
        int referenceOffset = unicodeEscapedOffset;

        for (int i = unicodeEscapedOffset; i < max; i++) {

            final char c = unicodeEscapedText[i];

            /*
             * Check the need for an unescape operation at this point
             */

            if (c != ESCAPE_PREFIX || (i + 1) >= max) {
                continue;
            }

            int codepoint = -1;

            if (c == ESCAPE_PREFIX) {

                final char c1 = unicodeEscapedText[i + 1];

                switch (c1) {
                    case '0': if (!isOctalEscape(unicodeEscapedText,i + 1,max)) { codepoint = 0x00; referenceOffset = i + 1; }; break;
                    case 'b': codepoint = 0x08; referenceOffset = i + 1; break;
                    case 't': codepoint = 0x09; referenceOffset = i + 1; break;
                    case 'n': codepoint = 0x0A; referenceOffset = i + 1; break;
                    case 'f': codepoint = 0x0C; referenceOffset = i + 1; break;
                    case 'r': codepoint = 0x0D; referenceOffset = i + 1; break;
                    case '"': codepoint = 0x22; referenceOffset = i + 1; break;
                    case '\'': codepoint = 0x27; referenceOffset = i + 1; break;
                    case '\\': codepoint = 0x5C; referenceOffset = i + 1; break;
                }

                if (codepoint == -1) {

                    if (c1 >= '0' && c1 <= '7') {
                        // This can be a octal escape, we need at least 1 more char, and up to 3 more.

                        int f = i + 2;
                        while (f < (i + 4) && f < max) { // We need only a max of two more chars
                            final char cf = unicodeEscapedText[f];
                            if (!(cf >= '0' && cf <= '7')) {
                                break;
                            }
                            f++;
                        }

                        codepoint = parseIntFromReference(unicodeEscapedText, i + 1, f, 8);

                        if (codepoint > 0xFF) {
                            // Maximum octal escape char is FF. Ignore the last digit
                            codepoint = parseIntFromReference(unicodeEscapedText, i + 1, f - 1, 8);
                            referenceOffset = f - 2;
                        } else {
                            referenceOffset = f - 1;
                        }

                        // Don't continue here, just let the unescape code below do its job

                    } else {

                        // Other escape sequences are not allowed by Java. So we leave it as is
                        // and expect the corresponding Java parser to fail.
                        i++;
                        continue;

                    }

                }

            }


            /*
             * At this point we know for sure we will need some kind of unescape, so we
             * can copy all the contents pending up to this point.
             */

            if (i - readOffset > 0) {
                writer.write(unicodeEscapedText, readOffset, (i - readOffset));
            }

            i = referenceOffset;
            readOffset = i + 1;

            /*
             * --------------------------
             *
             * Peform the real unescape
             *
             * --------------------------
             */

            if (codepoint > '\uFFFF') {
                writer.write(Character.toChars(codepoint));
            } else {
                writer.write((char)codepoint);
            }

        }


        /*
         * -----------------------------------------------------------------------------------------------
         * Final cleaning: append the remaining escaped text to the string builder and return.
         * -----------------------------------------------------------------------------------------------
         */

        if (max - readOffset > 0) {
            writer.write(unicodeEscapedText, readOffset, (max - readOffset));
        }


    }



}

