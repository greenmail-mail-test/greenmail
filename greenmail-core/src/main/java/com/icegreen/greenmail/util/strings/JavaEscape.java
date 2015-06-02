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

import java.io.IOException;
import java.io.Writer;


/**
 * <p>
 *   Utility class for performing Java escape/unescape operations.
 * </p>
 *
 * <strong><u>Configuration of escape/unescape operations</u></strong>
 *
 * <p>
 *   <strong>Escape</strong> operations can be (optionally) configured by means of:
 * </p>
 * <ul>
 *   <li><em>Level</em>, which defines how deep the escape operation must be (what
 *       chars are to be considered eligible for escaping, depending on the specific
 *       needs of the scenario). Its values are defined by the {@link JavaEscapeLevel}
 *       enum.</li>
 * </ul>
 * <p>
 *   Unbescape does not define a <em>'type'</em> for Java escaping (just a <em>level</em>) because,
 *   given the way Unicode Escapes work in Java, there is no possibility to choose whether we want to escape, for
 *   example, a <em>tab character</em> (U+0009) as a Single Escape Char (<tt>&#92;t</tt>) or as a Unicode Escape
 *   (<tt>&#92;u0009</tt>). Given Unicode Escapes are processed by the compiler and not the runtime, using
 *   <tt>&#92;u0009</tt> instead of <tt>&#92;t</tt> would really insert a tab character inside our source
 *   code before compiling, which is not equivalent to inserting <tt>"&#92;t"</tt> in a <tt>String</tt> literal.
 * </p>
 * <p>
 *   <strong>Unescape</strong> operations need no configuration parameters. Unescape operations
 *   will always perform <em>complete</em> unescape of SECs (<tt>&#92;n</tt>),
 *   u-based (<tt>&#92;u00E1</tt>) and octal (<tt>&#92;341</tt>) escapes.
 * </p>
 *
 * <strong><u>Features</u></strong>
 *
 * <p>
 *   Specific features of the Java escape/unescape operations performed by means of this class:
 * </p>
 * <ul>
 *   <li>The Java basic escape set is supported. This <em>basic set</em> consists of:
 *         <ul>
 *           <li>The <em>Single Escape Characters</em>:
 *               <tt>&#92;b</tt> (<tt>U+0008</tt>),
 *               <tt>&#92;t</tt> (<tt>U+0009</tt>),
 *               <tt>&#92;n</tt> (<tt>U+000A</tt>),
 *               <tt>&#92;f</tt> (<tt>U+000C</tt>),
 *               <tt>&#92;r</tt> (<tt>U+000D</tt>),
 *               <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
 *               <tt>&#92;&#39;</tt> (<tt>U+0027</tt>) and
 *               <tt>&#92;&#92;</tt> (<tt>U+005C</tt>). Note <tt>&#92;&#39;</tt> is not really needed in
 *               String literals (only in Character literals), so it won't be used until escape level 3.
 *           </li>
 *           <li>
 *               Two ranges of non-displayable, control characters (some of which are already part of the
 *               <em>single escape characters</em> list): <tt>U+0000</tt> to <tt>U+001F</tt>
 *               and <tt>U+007F</tt> to <tt>U+009F</tt>.
 *           </li>
 *         </ul>
 *   </li>
 *   <li>U-based hexadecimal escapes (a.k.a. <em>unicode escapes</em>) are supported both in escape
 *       and unescape operations: <tt>&#92;u00E1</tt>.</li>
 *   <li>Octal escapes are supported, though only in unescape operations: <tt>&#92;071</tt>. These are not supported
 *       in escape operations because the use of octal escapes is not recommended by the Java Language Specification
 *       (it's usage is allowed mainly for C compatibility reasons).</li>
 *   <li>Support for the whole Unicode character set: <tt>&#92;u0000</tt> to <tt>&#92;u10FFFF</tt>, including
 *       characters not representable by only one <tt>char</tt> in Java (<tt>&gt;&#92;uFFFF</tt>).</li>
 * </ul>
 *
 * <strong><u>Specific features of Unicode Escapes in Java</u></strong>
 *
 * <p>
 *   The way Unicode Escapes work in Java is different to other languages like e.g. JavaScript. In Java,
 *   these UHEXA escapes are processed by the compiler itself, and therefore resolved before any other
 *   type of escapes. Besides, UHEXA escapes can appear anywhere in the code, not only String literals.
 *   This means that, while in JavaScript <tt>'a&#92;u005Cna'</tt> would be displayed as <tt>a&#92;na</tt>,
 *   in Java <tt>"a&#92;u005Cna"</tt> would in fact be displayed in two lines:
 *   <tt>a</tt>+<tt>&lt;LF&gt;</tt>+<tt>a</tt>.
 * </p>
 * <p>
 *   Going even further, this is perfectly valid Java code:
 * </p>
 * <code>
 *   final String hello = &#92;u0022Hello, World!&#92;u0022;
 * </code>
 * <p>
 *   Also, Java allows to write any number of <tt>'u'</tt> characters in this type of escapes, like
 *   <tt>&#92;uu00E1</tt> or even <tt>&#92;uuuuuuuuu00E1</tt>. This is so in order to enable legacy
 *   compatibility with older code-processing tools that didn't support Unicode processing at all, which
 *   would fail when finding an Unicode escape like <tt>&#92;u00E1</tt>, but not <tt>&#92;uu00E1</tt>
 *   (because they would consider <tt>&#92;u</tt> as the escape). So this is valid Java code too:
 * </p>
 * <code>
 *   final String hello = &#92;uuuuuuuu0022Hello, World!&#92;u0022;
 * </code>
 * <p>
 *   In order to correctly unescape Java UHEXA escapes like <tt>"a&#92;u005Cna"</tt>, Unbescape will
 *   perform a two-pass process so that all unicode escapes are processed in the first pass, and then
 *   the single escape characters and octal escapes in the second pass.
 * </p>
 *
 * <strong><u>Input/Output</u></strong>
 *
 * <p>
 *   There are two different input/output modes that can be used in escape/unescape operations:
 * </p>
 * <ul>
 *   <li><em><tt>String</tt> input, <tt>String</tt> output</em>: Input is specified as a <tt>String</tt> object
 *       and output is returned as another. In order to improve memory performance, all escape and unescape
 *       operations <u>will return the exact same input object as output if no escape/unescape modifications
 *       are required</u>.</li>
 *   <li><em><tt>char[]</tt> input, <tt>java.io.Writer</tt> output</em>: Input will be read from a char array
 *       (<tt>char[]</tt>) and output will be written into the specified <tt>java.io.Writer</tt>.
 *       Two <tt>int</tt> arguments called <tt>offset</tt> and <tt>len</tt> will be
 *       used for specifying the part of the <tt>char[]</tt> that should be escaped/unescaped. These methods
 *       should be called with <tt>offset = 0</tt> and <tt>len = text.length</tt> in order to process
 *       the whole <tt>char[]</tt>.</li>
 * </ul>
 *
 * <strong><u>Glossary</u></strong>
 *
 * <dl>
 *   <dt>SEC</dt>
 *     <dd>Single Escape Character:
 *               <tt>&#92;b</tt> (<tt>U+0008</tt>),
 *               <tt>&#92;t</tt> (<tt>U+0009</tt>),
 *               <tt>&#92;n</tt> (<tt>U+000A</tt>),
 *               <tt>&#92;f</tt> (<tt>U+000C</tt>),
 *               <tt>&#92;r</tt> (<tt>U+000D</tt>),
 *               <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
 *               <tt>&#92;&#39;</tt> (<tt>U+0027</tt>) and
 *               <tt>&#92;&#92;</tt> (<tt>U+005C</tt>). Note <tt>&#92;&#39;</tt> is not really needed in
 *               String literals (only in Character literals), so it won't be used until escape level 3.
 *     </dd>
 *   <dt>UHEXA escapes</dt>
 *     <dd>Also called <em>u-based hexadecimal escapes</em> or simply <em>unicode escapes</em>:
 *         complete representation of unicode codepoints up to <tt>U+FFFF</tt>, with <tt>&#92;u</tt>
 *         followed by exactly four hexadecimal figures: <tt>&#92;u00E1</tt>. Unicode codepoints &gt;
 *         <tt>U+FFFF</tt> can be represented in Java by mean of two UHEXA escapes (a
 *         <em>surrogate pair</em>).</dd>
 *   <dt>Octal escapes</dt>
 *     <dd>Octal representation of unicode codepoints up to <tt>U+00FF</tt>, with <tt>&#92;</tt>
 *         followed by up to three octal figures: <tt>&#92;071</tt>. Though up to three octal figures
 *         are allowed, octal numbers &gt; <tt>377</tt> (<tt>0xFF</tt>) are not supported. These are not supported
 *         in escape operations because the use of octal escapes is not recommended by the Java Language Specification
 *         (it's usage is allowed mainly for C compatibility reasons).</dd>
 *   <dt>Unicode Codepoint</dt>
 *     <dd>Each of the <tt>int</tt> values conforming the Unicode code space.
 *         Normally corresponding to a Java <tt>char</tt> primitive value (codepoint &lt;= <tt>&#92;uFFFF</tt>),
 *         but might be two <tt>char</tt>s for codepoints <tt>&#92;u10000</tt> to <tt>&#92;u10FFFF</tt> if the
 *         first <tt>char</tt> is a high surrogate (<tt>&#92;uD800</tt> to <tt>&#92;uDBFF</tt>) and the
 *         second is a low surrogate (<tt>&#92;uDC00</tt> to <tt>&#92;uDFFF</tt>).</dd>
 * </dl>
 *
 * <strong><u>References</u></strong>
 *
 * <p>
 *   The following references apply:
 * </p>
 * <ul>
 *   <li><a href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html" target="_blank">The Java 7 Language
 *       Specification - Chapter 3: Lexical Structure.</a> [oracle.com]</li>
 *   <li><a href="http://arity23.blogspot.com/2013/04/secrets-of-scala-lexer-1-uuuuunicode.html"
 *       target="_blank">Secrets of the Scala Lexer 1: &#92;uuuuunicode</a> [blogspot.com]</li>
 *   <li><a href="http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html"
 *       target="_blank">Supplementary characters in the Java Platform</a> [oracle.com]</li>
 * </ul>
 *
 * @author Daniel Fern&aacute;ndez
 *
 * @since 1.0.0
 *
 */
public final class JavaEscape {


    /**
     * <p>
     *   Perform a Java level 1 (only basic set) <strong>escape</strong> operation
     *   on a <tt>String</tt> input.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the Java basic escape set:
     * </p>
     * <ul>
     *   <li>The <em>Single Escape Characters</em>:
     *       <tt>&#92;b</tt> (<tt>U+0008</tt>),
     *       <tt>&#92;t</tt> (<tt>U+0009</tt>),
     *       <tt>&#92;n</tt> (<tt>U+000A</tt>),
     *       <tt>&#92;f</tt> (<tt>U+000C</tt>),
     *       <tt>&#92;r</tt> (<tt>U+000D</tt>),
     *       <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
     *       <tt>&#92;&#39;</tt> (<tt>U+0027</tt>) and
     *       <tt>&#92;&#92;</tt> (<tt>U+005C</tt>). Note <tt>&#92;&#39;</tt> is not really needed in
     *       String literals (only in Character literals), so it won't be used until escape level 3.
     *   </li>
     *   <li>
     *       Two ranges of non-displayable, control characters (some of which are already part of the
     *       <em>single escape characters</em> list): <tt>U+0000</tt> to <tt>U+001F</tt>
     *       and <tt>U+007F</tt> to <tt>U+009F</tt>.
     *   </li>
     * </ul>
     * <p>
     *   This method calls {@link #escapeJava(String, JavaEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>level</tt>:
     *       {@link JavaEscapeLevel#LEVEL_1_BASIC_ESCAPE_SET}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if <tt>text</tt> is <tt>null</tt>.
     */
    public static String escapeJavaMinimal(final String text) {
        return escapeJava(text, JavaEscapeLevel.LEVEL_1_BASIC_ESCAPE_SET);
    }


    /**
     * <p>
     *   Perform a Java level 2 (basic set and all non-ASCII chars) <strong>escape</strong> operation
     *   on a <tt>String</tt> input.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The Java basic escape set:
     *         <ul>
     *           <li>The <em>Single Escape Characters</em>:
     *               <tt>&#92;b</tt> (<tt>U+0008</tt>),
     *               <tt>&#92;t</tt> (<tt>U+0009</tt>),
     *               <tt>&#92;n</tt> (<tt>U+000A</tt>),
     *               <tt>&#92;f</tt> (<tt>U+000C</tt>),
     *               <tt>&#92;r</tt> (<tt>U+000D</tt>),
     *               <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
     *               <tt>&#92;&#39;</tt> (<tt>U+0027</tt>) and
     *               <tt>&#92;&#92;</tt> (<tt>U+005C</tt>). Note <tt>&#92;&#39;</tt> is not really needed in
     *               String literals (only in Character literals), so it won't be used until escape level 3.
     *           </li>
     *           <li>
     *               Two ranges of non-displayable, control characters (some of which are already part of the
     *               <em>single escape characters</em> list): <tt>U+0000</tt> to <tt>U+001F</tt>
     *               and <tt>U+007F</tt> to <tt>U+009F</tt>.
     *           </li>
     *         </ul>
     *   </li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by using the Single Escape Chars whenever possible. For escaped
     *   characters that do not have an associated SEC, default to <tt>&#92;uFFFF</tt>
     *   Hexadecimal Escapes.
     * </p>
     * <p>
     *   This method calls {@link #escapeJava(String, JavaEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>level</tt>:
     *       {@link JavaEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if <tt>text</tt> is <tt>null</tt>.
     */
    public static String escapeJava(final String text) {
        return escapeJava(text, JavaEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET);
    }


    /**
     * <p>
     *   Perform a (configurable) Java <strong>escape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   This method will perform an escape operation according to the specified
     *   {@link org.unbescape.java.JavaEscapeLevel} argument value.
     * </p>
     * <p>
     *   All other <tt>String</tt>-based <tt>escapeJava*(...)</tt> methods call this one with preconfigured
     *   <tt>level</tt> values.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be escaped.
     * @param level the escape level to be applied, see {@link org.unbescape.java.JavaEscapeLevel}.
     * @return The escaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no escaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if <tt>text</tt> is <tt>null</tt>.
     */
    public static String escapeJava(final String text, final JavaEscapeLevel level) {

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        return JavaEscapeUtil.escape(text, level);

    }




    /**
     * <p>
     *   Perform a Java level 1 (only basic set) <strong>escape</strong> operation
     *   on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   <em>Level 1</em> means this method will only escape the Java basic escape set:
     * </p>
     * <ul>
     *   <li>The <em>Single Escape Characters</em>:
     *       <tt>&#92;b</tt> (<tt>U+0008</tt>),
     *       <tt>&#92;t</tt> (<tt>U+0009</tt>),
     *       <tt>&#92;n</tt> (<tt>U+000A</tt>),
     *       <tt>&#92;f</tt> (<tt>U+000C</tt>),
     *       <tt>&#92;r</tt> (<tt>U+000D</tt>),
     *       <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
     *       <tt>&#92;&#39;</tt> (<tt>U+0027</tt>) and
     *       <tt>&#92;&#92;</tt> (<tt>U+005C</tt>). Note <tt>&#92;&#39;</tt> is not really needed in
     *       String literals (only in Character literals), so it won't be used until escape level 3.
     *   </li>
     *   <li>
     *       Two ranges of non-displayable, control characters (some of which are already part of the
     *       <em>single escape characters</em> list): <tt>U+0000</tt> to <tt>U+001F</tt>
     *       and <tt>U+007F</tt> to <tt>U+009F</tt>.
     *   </li>
     * </ul>
     * <p>
     *   This method calls {@link #escapeJava(char[], int, int, java.io.Writer, JavaEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>level</tt>:
     *       {@link JavaEscapeLevel#LEVEL_1_BASIC_ESCAPE_SET}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if <tt>text</tt> is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeJavaMinimal(final char[] text, final int offset, final int len, final Writer writer)
                                         throws IOException {
        escapeJava(text, offset, len, writer, JavaEscapeLevel.LEVEL_1_BASIC_ESCAPE_SET);
    }


    /**
     * <p>
     *   Perform a Java level 2 (basic set and all non-ASCII chars) <strong>escape</strong> operation
     *   on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   <em>Level 2</em> means this method will escape:
     * </p>
     * <ul>
     *   <li>The Java basic escape set:
     *         <ul>
     *           <li>The <em>Single Escape Characters</em>:
     *               <tt>&#92;b</tt> (<tt>U+0008</tt>),
     *               <tt>&#92;t</tt> (<tt>U+0009</tt>),
     *               <tt>&#92;n</tt> (<tt>U+000A</tt>),
     *               <tt>&#92;f</tt> (<tt>U+000C</tt>),
     *               <tt>&#92;r</tt> (<tt>U+000D</tt>),
     *               <tt>&#92;&quot;</tt> (<tt>U+0022</tt>),
     *               <tt>&#92;&#39;</tt> (<tt>U+0027</tt>) and
     *               <tt>&#92;&#92;</tt> (<tt>U+005C</tt>). Note <tt>&#92;&#39;</tt> is not really needed in
     *               String literals (only in Character literals), so it won't be used until escape level 3.
     *           </li>
     *           <li>
     *               Two ranges of non-displayable, control characters (some of which are already part of the
     *               <em>single escape characters</em> list): <tt>U+0000</tt> to <tt>U+001F</tt>
     *               and <tt>U+007F</tt> to <tt>U+009F</tt>.
     *           </li>
     *         </ul>
     *   </li>
     *   <li>All non ASCII characters.</li>
     * </ul>
     * <p>
     *   This escape will be performed by using the Single Escape Chars whenever possible. For escaped
     *   characters that do not have an associated SEC, default to <tt>&#92;uFFFF</tt>
     *   Hexadecimal Escapes.
     * </p>
     * <p>
     *   This method calls {@link #escapeJava(char[], int, int, java.io.Writer, JavaEscapeLevel)}
     *   with the following preconfigured values:
     * </p>
     * <ul>
     *   <li><tt>level</tt>:
     *       {@link JavaEscapeLevel#LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET}</li>
     * </ul>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if <tt>text</tt> is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeJava(final char[] text, final int offset, final int len, final Writer writer)
                                  throws IOException {
        escapeJava(text, offset, len, writer, JavaEscapeLevel.LEVEL_2_ALL_NON_ASCII_PLUS_BASIC_ESCAPE_SET);
    }


    /**
     * <p>
     *   Perform a (configurable) Java <strong>escape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   This method will perform an escape operation according to the specified
     *   {@link org.unbescape.java.JavaEscapeLevel} argument value.
     * </p>
     * <p>
     *   All other <tt>char[]</tt>-based <tt>escapeJava*(...)</tt> methods call this one with preconfigured
     *   <tt>level</tt> values.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be escaped.
     * @param offset the position in <tt>text</tt> at which the escape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be escaped.
     * @param writer the <tt>java.io.Writer</tt> to which the escaped result will be written. Nothing will
     *               be written at all to this writer if <tt>text</tt> is <tt>null</tt>.
     * @param level the escape level to be applied, see {@link org.unbescape.java.JavaEscapeLevel}.
     * @throws IOException if an input/output exception occurs
     */
    public static void escapeJava(final char[] text, final int offset, final int len, final Writer writer,
                                  final JavaEscapeLevel level)
                                  throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        if (level == null) {
            throw new IllegalArgumentException("The 'level' argument cannot be null");
        }

        final int textLen = (text == null? 0 : text.length);

        if (offset < 0 || offset > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        if (len < 0 || (offset + len) > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        JavaEscapeUtil.escape(text, offset, len, writer, level);

    }








    /**
     * <p>
     *   Perform a Java <strong>unescape</strong> operation on a <tt>String</tt> input.
     * </p>
     * <p>
     *   No additional configuration arguments are required. Unescape operations
     *   will always perform <em>complete</em> Java unescape of SECs, u-based and octal escapes.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>String</tt> to be unescaped.
     * @return The unescaped result <tt>String</tt>. As a memory-performance improvement, will return the exact
     *         same object as the <tt>text</tt> input argument if no unescaping modifications were required (and
     *         no additional <tt>String</tt> objects will be created during processing). Will
     *         return <tt>null</tt> if <tt>text</tt> is <tt>null</tt>.
     */
    public static String unescapeJava(final String text) {
        return JavaEscapeUtil.unescape(text);
    }


    /**
     * <p>
     *   Perform a Java <strong>unescape</strong> operation on a <tt>char[]</tt> input.
     * </p>
     * <p>
     *   No additional configuration arguments are required. Unescape operations
     *   will always perform <em>complete</em> Java unescape of SECs, u-based and octal escapes.
     * </p>
     * <p>
     *   This method is <strong>thread-safe</strong>.
     * </p>
     *
     * @param text the <tt>char[]</tt> to be unescaped.
     * @param offset the position in <tt>text</tt> at which the unescape operation should start.
     * @param len the number of characters in <tt>text</tt> that should be unescaped.
     * @param writer the <tt>java.io.Writer</tt> to which the unescaped result will be written. Nothing will
     *               be written at all to this writer if <tt>text</tt> is <tt>null</tt>.
     * @throws IOException if an input/output exception occurs
     */
    public static void unescapeJava(final char[] text, final int offset, final int len, final Writer writer)
                                    throws IOException{
        if (writer == null) {
            throw new IllegalArgumentException("Argument 'writer' cannot be null");
        }

        final int textLen = (text == null? 0 : text.length);

        if (offset < 0 || offset > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        if (len < 0 || (offset + len) > textLen) {
            throw new IllegalArgumentException(
                    "Invalid (offset, len). offset=" + offset + ", len=" + len + ", text.length=" + textLen);
        }

        JavaEscapeUtil.unescape(text, offset, len, writer);

    }




    private JavaEscape() {
        super();
    }


}

