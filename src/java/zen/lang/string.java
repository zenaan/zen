// zen.lang.string - in search of a proper Java string class
//
// Copyright (C) 2015 Zenaan Harkness
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
// 02110-1301, USA

package zen.lang;


import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.text.Segment;

import com.google.common.primitives.Ints;


/**
 * <p> A string in its simplest form is just a "cookie".
 *
 * <p> <a name="contents"></a><b>0. Contents</b><br>
 * <a href="#introduction">1. Introduction</a><br>
 * <a href="#understanding">2. Understanding characters</a><br>
 * <a href="#chars">3. Some Java "characters"</a><br>
 * <a href="#about">4. More about Java's limitations and this class</a><br>
 * <a href="#further">5. Further Reading</a><br>
 * <a href="#clubrules">6. UTF-8 Club Rules</a><br>
 *
 *
 * <p> <b>TLDR:</b><br>
 * Use UTF-8 strings (byte[]) everywhere possible especially between different
 * code bases and when communicating with filesystems, and as the always
 * preferred file encoding format in text files.  Use indexing of the UTF-8
 * string (byte[]) to speed up locating code points and graphemes within the
 * string (Java doesn't do this, so we've work to do).<br>
 * Refer <b><a href="http://utf8everywhere.org/">http://utf8everywhere.org/</a></b>
 *
 * <p>
 * <ol>
 *    <li> UTF-8 strings can be safely scanned for ASCII characters (the 7-bit
 *    set) which includes all normal punctuation and in particular directory
 *    separators and path separators in all known operating systems.
 *    <li> UTF-8 is also self-synchronizing in the face of one or more missing
 *    bytes from the stream; only those characters with missing or corrupted
 *    bytes will be in error and all other characters are read correctly - this
 *    is not the case for example with UTF-16 encoded strings.
 * </ol>
 *
 * <p> These two features alone (along with Unix and Linux compatibility) make
 * UTF-8 the most desirable text encoding to use in almost all cases!
 *
 *
 * <p> <a name="introduction"></a><b>1. <a href="#contents">Introduction</a></b>
 *
 * <p><b>A deficient string is any string not amenable to counting its
 * characters</b>, i.e. "characters as the user perceives them" or "graphemes".
 *
 * <p> {@code java.lang.String} is a deficient string class, no more than a
 * lowly cookie, incapable of counting the characters it stores for display, at
 * least when it comes to Unicode text.
 *
 * <p> This class is the beginnings of a proper Java string class, which might
 * be thought of as a UTF-8 encoded "grapheme string" - Java as of version 8.0
 * only has APIs to work with Unicode code points (codepoints) and not "visual
 * or displayed characters" such as those with multiple accents (Unicode
 * "combining characters").  Surprised? I was, thus this class.
 *
 * <p> The term "UTF-8 grapheme string" is a misnomer since UTF-8 defines a byte
 * sequence encoding of Unicode/UCS code points which include graphemes as well
 * as non-graphemes.
 *
 * <p> One or more (thankfully only adjacent) Unicode code points may constitute
 * a grapheme.
 *
 * <p> Cover your ears, I'm about to shout.  This string class simplifies
 * working with THE THING WE OFTEN NEED OUTSIDE OF THE STRING ITSELF, that is
 * graphemes.  Graphemes are those things a user perceives visually as a
 * character or "single letter" for the purposes of cursor movement, deletion,
 * insertion, selection, cutting and pasting.
 *
 * <p> <b>This class' current limitations are due to relying upon {@code
 * java.text.BreakIterator} and its {@code getCharacterInstance(Locale)} method,
 * which only provides Unicode code point breaks and not "visual" character
 * (grapheme) breaks.</b>
 *
 * <p> <b>Java as of version 8.0 does not provide Character methods such as
 * isCombiningCharacter(codepoint) nor isUnicodeControlCharacter(codepoint);
 * TODO - try using icu4j instead</b>
 *
 * <p> For comparison to Java, see
 * <a href="https://developer.apple.com/library/ios/documentation/Swift/Conceptual/Swift_Programming_Language/StringsAndCharacters.html">
 * Swift's Strings and Characters</a>, which is much younger, better and
 * certainly has advantage of the benefit of hindsight. Also Perl 6 seems to be
 * getting closer to competency as well, see
 * <a href="http://www.cattlegrid.info/blog/2014/12/graphemes-code-points-characters-and-bytes.html">
 * Graphemes, code points, characters and bytes</a>.
 *
 *
 * <p> <a name="understanding"></a><b>2. <a href="#contents">Understanding
 * Characters</a></b>
 *
 * <p>
 * <ol>
 *    <li> Programming language <b>types</b> such as {@code char}, {@code
 *    byte[]} and {@code int} store arbitrary values.  In a program such values
 *    may for example have context specific meaning such as a mapping to ASCII
 *    characters, Unicode code points or to Unicode UTF-16 surrogate halves
 *    ("paired 16 bit code units" halves, <a href="#surrogates">see below</a>)
 *    or even to graphemes, grapheme clusters, glyphs or any sequence of such or
 *    other things.
 *
 *    <li> The <b>Unicode</b> <a href="http://unicode.org/">standard</a>
 *    specifies Unicode code points, various
 *    code point encodings, encoding specific storage 'code units' and code unit
 *    sequences (how code points are represented in an encoding), combining
 *    characters and their normalization forms, the optional byte order mark
 *    (BOM), how sequences of code points map (algorithmically) to graphemes and
 *    more.
 *
 *    <li> A Unicode <b>code point</b> is <a
 *    href="http://unicode.org/glossary/#code_point">Any value in the Unicode
 *    codespace</a>. It represents one of a number of things,
 *    including characters of the world's languages/ scripts, Unicode control
 *    characters, the BOM, specials (I'm sure they are), locals, combining
 *    characters and more, see <a
 *    href="http://unicode.org/glossary/#code_point_type">code point type</a>.
 *
 *    <li> Code points are not graphemes though some code points map to
 *    graphemes depending on their context in a code point sequence.
 *
 *    <li> Code points were originally defined to be 31 bits but since ISO
 *    10646-2 in 2001 are promised to be at most 21 bits in size in the future,
 *    the consequence being all code points will be representable in a UTF-8
 *    byte stream with at most 4 bytes (4 * 8-bits).
 *
 *    <li> A Unicode <b>combining character</b> sometimes called
 *    <a href="http://unicode.org/glossary/#combining_character">combining
 *    mark</a> modifies another character;
 *    examples include the diacritical marks, International Phonetic Alphabet
 *    (IPA) symbols, IPA diacritics, the combining graphene joiner, double
 *    diacritics which are combined with (and are placed across) two characters,
 *    and more.  Multiple combinations of combining characters are valid to use
 *    in sequence.
 *    See Unicode's <a href="http://unicode.org/faq/char_combmark.html">
 *    Characters and Combining Marks</a>.
 *
 *    <li> A Unicode <b>grapheme</b> represents a <a
 *    href="http://unicode.org/glossary/#grapheme">user perceived character</a>
 *    which the user might move over with a text cursor using a "single
 *    character" cursor movement or might delete with a single press of the
 *    {@code <BACKSPACE>} or {@code <DELETE>} key.
 *
 *    <li> A <a href="http://unicode.org/glossary/#grapheme_cluster"><b>grapheme
 *    cluster</b></a> is the ideal way to represent "characters as the user
 *    perceives them" and we who had enjoyed Java's hegemony pine with grief at
 *    <a href="https://developer.apple.com/library/ios/documentation/Swift/Conceptual/Swift_Programming_Language/StringsAndCharacters.html#//apple_ref/doc/uid/TP40014097-CH7-ID296">
 *    Swift's competency</a> in this regard.
 *
 *    <li> At the time of typing this, the author is unable to clearly
 *    differentiate between graphemes and grapheme clusters.
 *
 *    <li> A grapheme is represented by a sequence of one or more code points,
 *    such as {@code LATIN CAPITAL LETTER A} ({@code U+0041}, "A") followed by
 *    {@code COMBINING ACUTE ACCENT} ({@code U+0301}, " ́" - this may not display
 *    as one might expect).
 *
 *    <li> Some letter with combining character combinations are also included
 *    as single code points, such as {@code LATIN CAPITAL LETTER A WITH ACUTE}
 *    ({@code U+00C1}, "Á").
 *
 *    <li> So some code points do not constitute and or are not part of the
 *    representation of any grapheme.
 *
 *    <li> <a href="http://www.unicode.org/versions/Unicode7.0.0/ch03.pdf#G49537">
 *    Unicode specifies</a> various
 *    <a href="http://unicode.org/glossary/#normalization"><b>nomalization</b></a>
 *    forms where combining characters are used where possible or avoided where
 *    possible, in a code point stream; this can provide for binary equivalence
 *    comparison.
 *    See also {@code java.text.Normalizer}.
 *
 *    <li> A <a href="http://unicode.org/glossary/#glyph"><b>glyph</b></a> is an
 *    element of a font which is displayed to the user within a textual or
 *    graphical display environment.
 *
 *    <li> One or more glyphs may be needed to correctly display a single
 *    grapheme, for example with a glyph corresponding to a combining character
 *    (represented by one code point) where the combining character follows and
 *    is intended to be overlayed visually with ("combined" with) the preceeding
 *    one or two characters (each represented by their own code points) in the
 *    code point sequence.
 *
 *    <li> A <b>font</b> specifies how graphemes, or visual characters as
 *    defined by some other standard, map to glyphs.  Only some fonts include
 *    glyphs for all displayable code points.  Only some font engines or display
 *    environments support combining characters.
 *
 *    <li> A <b>Java {@code char}</b> is not a grapheme, but some graphemes can
 *    be represented in a {@code char} instance or "variable".
 *
 *    <li><a name="surrogates"></a> A Java {@code char} is not even a code
 *    point, although:<br>
 *    a) it can represent some code points namely those in the Unicode
 *    <b>BMP</b> or "basic multilingual plane" which is the first <a
 *    href="http://unicode.org/glossary/#BMP_character">16-bit group</a> or
 *    Unicode "plane" of code points, and<br>
 *    b) it can also hold UTF-16 surrogate code point halves
 *    ("<b>surrogates</b>") for those code points outside the BMP.<br>
 *    If you're not impressed with Java's {@code char}s, neither am I - they're
 *    not very impressive at all.  {@code byte} - now there's a powerful type!
 *
 *    <li> A <a href="http://unicode.org/glossary/#surrogate_pair">well
 *    formed</a> <b>surrogate</b> is one half of a "paired 16 bit code units"
 *    code point, whereas an ill formed surrogate is <em>unpaired</em>, i.e.
 *    without its matching half, and is an error condition.<br>
 *    Refer the Unicode FAQ
 *    <a href="http://unicode.org/faq/utf_bom.html#utf16-2">What are
 *    surrogates?</a> and
 *    <a href="http://unicode.org/faq/utf_bom.html#utf8-5">How do I convert an
 *    unpaired UTF-16 surrogate to UTF-8?</a>
 *
 *    <li> A Unicode <a
 *    href="http://unicode.org/glossary/#character_encoding_scheme"><b>encoding</b>
 *    scheme</a> defines a bit format for storing, transmitting or otherwise
 *    representing code points in a digital medium.
 *
 *    <li> Encodings encode <a
 *    href="http://unicode.org/glossary/#code_point">code points</a> into a
 *    sequence of one or more Unicode <b>code units</b>.  A <a
 *    href="http://unicode.org/glossary/#code_unit">code unit</a> is a "storage
 *    unit" (8, 16 or 32 bit value) in an encoding.  Code unit format and
 *    sequencing are specific to an encoding.
 *
 *    <li> Unicode encodings include for example <a
 *    href="http://unicode.org/glossary/#UTF_8"><b>UTF-8</b></a>, <a
 *    href="http://unicode.org/glossary/#UTF_16LE">UTF-16LE</a> and <a
 *    href="http://unicode.org/glossary/#UTF_32">UTF-32</a>.  Some encodings
 *    cannot normally represent all unicode code points, for example <a
 *    href="http://unicode.org/glossary/#ASCII">ASCII</a>.
 *    UTF-8 can represent all code points.
 *
 *    <li> Due to endian issues, anything other than UTF-8 is not self
 *    describing without a <b>BOM</b> or <a
 *    href="http://unicode.org/glossary/#byte_order_mark"><b>byte order
 *    mark</b></a> also known originally as "zero-width non-breaking space"
 *    (this whitespace usage is still valid but since deprecated in Unicode 3.2
 *    oh happy joy).  The BOM <a
 *    href="http://utf8everywhere.org/#faq.boms">messes up</a> UTF-8 data
 *    streams where it's not expected or not otherwise handled correctly and the
 *    joy only increases.
 *
 *    <li> So <a href="http://utf8everywhere.org/#faq.boms">where at all
 *    possible</a> in UTF-8 streams (which we should almost always be using)
 *    <b>never use a BOM</b>.
 *
 *    <li> In Java e.g. at {@code java.lang.String#codePointAt(index)} the index
 *    points at a char value which the javadoc names as a "Unicode code unit".
 *    Beware as <b>this is neither a code point, character, grapheme nor
 *    glyph</b> - nothing but a lowly, miserably little char!  "Code unit" as
 *    used here in Java is just a fancy name for "I'm so damn cool I can spel
 *    UTF-16BE BMP code point in a char and UTF-16LE non-BMP code point
 *    surrogate half "code unit" in a char take that! Haha try and figure out
 *    which."  Resist the temptation to figure out the riddle.  Don't be fooled
 *    friends - it's useless for anything much at all except reminding us what a
 *    bad design decision Java's {@code String} class was (in hindsight).
 *
 *    <li> Next, {@code java.text.BreakIterator#getCharacterInstance(Locale)}
 *    returns break indices for (Unicode) code point boundaries and not for
 *    graphemes, characters, glyphs, chars or anything useful really.  This
 *    iterator, better than nothing I guess, is merely a code point
 *    surrogate-pair extractor for Java's UTF-16 strings, slapping all comers in
 *    the face with its irreverent disregard for invisibles, controls, specials,
 *    even combining characters and in fact anything remotely resembling
 *    graphemes, let alone those hallowed unreachable glyphs!  The use of the
 *    term "character" in this method's name is both misleading and wildly
 *    overloaded and horrendously overrated, especially in Java context!
 *
 *    <li> But wait it gets better - if you want to determine graphemes (let's
 *    not even talk about glyphs), nope that can't be done (at least as at Java
 *    8).
 *
 *    <li> Java's <b>String</b> class ostensibly stores a UTF-16 encoded code
 *    point sequence in a {@code char[]}.  There is nothing in principle
 *    stopping a UTF-8 byte[] implementation (which is evidently indicated in
 *    the hindsight of history) and this ought be nothing more than a piddly
 *    implementation detail, notwithstanding any legacy yet-to-be-converted
 *    API's and their snivelling performance issues - which would likely act as
 *    a rather practical and motivating todo list.  Righto - let's get cracking
 *    then shall we?  In the meantime, without handling graphemes, {@code
 *    java.lang.String} defeats its own purpose surprisingly well.
 *
 *    <li> Finally to highlight unequivocally the problems we programmers face,
 *    some graphemes are normally displayed using <b>wide glyphs</b>, some
 *    normally with <b>narrow glyphs</b> and others still can be either,
 *    according to the font designer's æsthetic vehemence (ok, ok, "or lack
 *    thereof" - happy?).
 *
 *    <li> In conclusion the appearance of a <b>displayed grapheme</b> (and in
 *    particular for programs targetting a text based environment) depends on
 *    the particular textual or graphical environment in use including the
 *    specific font and the characteristics of the grapheme's corresponding
 *    glyph (or combination of glyphs) in the font, being used in the
 *    environment to display that grapheme.<br>
 *    <b>For example</b> an {@code xterm} terminal is a textual display
 *    environment in which Java provides no API for determining glyph widths and
 *    we can't even determine grapheme boundaries yet - Java evidently has a way
 *    to go on the basic text processing front.<br>
 *    In Java we need to get to first base - determining the graphemes in a
 *    string!  Java just manages to figure out single code points but peters out
 *    before anything useful to users.
 *
 * </ol>
 *
 *
 * <p> <a name="chars"></a><b>3. <a href="#contents">Some Java "Characters"</a></b>
 *
 * <p> Some of the "character" concepts a Java programmer may have to handle
 * with JNI or otherwise:
 *
 * <ol>
 *    <li> 7-bit ASCII characters.
 *    <li> Java's {@code char} type or "native character" which as we now know
 *    can only store a BMP code point or UTF-16 code point surrogate half.
 *    <li> Unicode UTF-8 "code units" (8 bit values).
 *    <li> Unicode UTF-16 BMP code point code units (16 bit values), mildly
 *    analogous to ASCII values.
 *    <li> Unicode UTF-16 code point surrogate halves (code units).
 *    <li> Unicode UTF-32 code point code units.
 *    <li> C and C++ types {@code char}, {@code byte}, {@code int}, {@code
 *    wchar_t}, {@code char16_t}, {@code char32_t}, {@code wint_t} and more
 *    still.
 *    <li> Unicode code points as represented in a Java {@code int}.
 *    <li> Unicode code points as represented in a Java {@code char[]} of length
 *    1 or 2.
 *    <li> Unicode code points as distinguished by {@code char} boundaries as
 *    returned by {@code BreakIterator.getCharacterInstance(Locale)#next()}.
 *    <li> Unicode non character code points such as control characters, which
 *    must be avoided when needing to work with graphemes.
 *    <li> <b>Graphemes</b> or Unicode code point sequences which correspond to
 *    user perceived characters - good luck with that.
 *    <li> <b>Glyphs</b> or those elements of a font which correspond in some
 *    way to code points and can be combined to display a representation of
 *    graphemes and are displayed to a user in a textual and or graphical
 *    display environment.
 * </ol>
 *
 *
 * <p> <a name="about"></a><b>4. <a href="#contents">More about Java's
 * limitations and this class</a></b>
 *
 * <p> This Java class is an "alternative string" rough draft.
 * There's plenty it does not (yet) do which needs to be done, although the
 * first step of understanding the problem has hopefully been contributed to
 * with the documentation here.
 *
 * <p> Certainly this class ought be built on something like java.nio.ByteBuffer
 * or a byte[] (which it is not currently).
 *
 * <p> Information about the "current" width of "ordinarily wide" and "possibly
 * wide" and even "in the current font, unexpectedly wide" graphemes,
 * determination of grapheme boundaries and more is yet to be implemented.
 *
 * <p> For starters some API is needed for textual display environments to
 * access the width of each glyph in the current environment, notification of
 * environment changes (such as a new font being applied to the environment) so
 * that glyph width or other calculations can be redone if needed, and to what
 * extent the current environment supports combining, potentially wide and other
 * interesting characters.
 *
 * <p> Glyph widths might for example in a text environment be in some suitable
 * environment units, with 'normal' characters being width 1 and 'wide'
 * characters being width 2.  This is presently an exercise for the excessively
 * curious and diligent.
 *
 * <p> Note: There's no point being upset with Java's UTF-16 String class and
 * char primitive, since at the time these were chosen for Java, it was
 * erroneously believed by those making that choice that 16 bits would be enough
 * to encode all the world's characters and therefore "should be enough for
 * anyone, even a programming language."<br>
 * BUT, there IS a point in complaining that once Java 1.0 was released, and
 * prior to Java 1.1 being released, that Java should have had a serious String
 * enhancement plan of action, due to the fundamental change in Unicode, which
 * Java has forever been fundamentally incompatible with (practically, as in for
 * regular day to day programming, or anything other than herculean efforts on
 * behalf of the programmer anyway). Swift gets it right whilst Java's 20 years
 * late to the party.
 *
 * <p> <b>WARNING</b>: Java's <a
 * href="https://docs.oracle.com/javase/6/docs/api/java/io/DataInput.html">current
 * usage</a> of <a
 * href="http://stackoverflow.com/questions/7921016/what-does-it-mean-to-say-java-modified-utf-8-encoding">bastadardized
 * UTF-8</a> ("<a
 * href="http://en.wikipedia.org/wiki/UTF-8#Modified_UTF-8">modified UTF-8</a>")
 * in some cases such as JNI <a
 * href="http://banachowski.com/deprogramming/2012/02/working-around-jni-utf-8-strings/">ought
 * be eliminated</a>, <a
 * href="https://wikigurus.com/Article/Show/199390/Java-modified-UTF-8-strings-in-Python">removed</a>,
 * <a
 * href="https://www.securecoding.cert.org/confluence/display/java/JNI04-J.+Do+not+assume+that+Java+strings+are+null-terminated">stopped</a>,
 * <a
 * href="http://bitbrothers.org/blog/2013/08/jni-strings-modified-utf8-oh-my/">stamped
 * out and completely extricated</a> from a future version of Java.  Bring on
 * the deprecations.
 *
 * <p> Note: It is probably in the interests of the Java language to have a class
 * possibly similar to this one as a "new string" type perhaps named {@code
 * java.lang.string} (note the lower case s in string).  Such strings should be
 * stored in UTF-8 byte arrays as canonical storage format and may require some
 * language (read, compiler) support for auto casting or more.
 *
 * <p> Or it may be that the best approach is to simply replace Java's String
 * class (and any and all dependent library APIs) so that it is built on a
 * "sane" UTF-8 encoded byte[] and deprecate all API (in all the Java libraries)
 * that violate this assumption for "performance" or any other grounds.
 *
 * <p>It is now self evident that Java's char type is actually not relevant and
 * not particularly useful outside of Java String's deformed birth due to the
 * false assumptions made when a Java class named String was impregnated with
 * chars.
 *
 * <p> In hindsight if char is for programming convenience, what might be more
 * useful is a grapheme type say a UTF-8 {@code byte[]} or a
 * code point {@code int[]}, perhaps with language support to cast to and from a
 * "new string".  Until this is tidied up in Java we unfortunately have a bit of
 * a mess on the string front and nothing even approaching a sexy Grapheme
 * String; I can't help wondering how such a verbose name as
 * java.lang.GraphemeString might be shortened.  This class started out named
 * "Graphemes" until it became obvious it's just a "sane string".
 *
 * <p> There are plenty api thoughts to think and some questions are in the code
 * comments below in this class.  Definitive answers would be nice.
 * <b>Swift appears to get it basically right.</b>
 *
 * <p> In case it was unclear note also that string literals as they appear in
 * <b>source files</b> are in the charset/encoding of the source file itself,
 * whatever that happens to be (of course we do <a
 * href="http://utf8everywhere.org/">insist on UTF-8</a>).
 *
 *
 * <p> <a name="further"></a><b>5. <a href="#contents">Further reading</a></b>
 *
 * <p> For further information see the following:
 *
 * <p> <ul>
 *    <li> <a href="http://utf8everywhere.org/">http://utf8everywhere.org/</a> - UTF-8. And why.
 *    <li> <a href="http://unicode.org/glossary/">http://unicode.org/glossary/</a> - Unicode Glossary
 *    <li> <a href="http://unicode.org/reports/tr15/">http://unicode.org/reports/tr15/</a> - Unicode normalization forms.
 *    <li> <a href="http://unicode.org/faq/utf_bom.html#utf16-2">http://unicode.org/faq/utf_bom.html#utf16-2</a> - Unicode, What are surrogates?
 *    <li> <a href="http://unicode.org/faq/utf_bom.html#utf8-5">http://unicode.org/faq/utf_bom.html#utf8-5</a> - Unicode, How do I convert an unpaired UTF-16 surrogate to UTF-8?
 *    <li> <a href="http://www.oracle.com/us/technologies/java/supplementary-142654.html">http://www.oracle.com/us/technologies/java/supplementary-142654.html</a> - Supplementary Characters in the Java Platform
 *    <li> <a href="http://userguide.icu-project.org/">http://userguide.icu-project.org/</a> - Introduction to ICU
 *    <li> <a href="http://en.wikipedia.org/wiki/Unicode">http://en.wikipedia.org/wiki/Unicode</a>
 *    <li> <a href="http://en.wikipedia.org/wiki/Combining_character">http://en.wikipedia.org/wiki/Combining_character</a>
 *    <li> <a href="http://en.wikipedia.org/wiki/Diacritics">http://en.wikipedia.org/wiki/Diacritics</a>
 *    <li> <a href="http://en.wikipedia.org/wiki/Specials_(Unicode_block)">http://en.wikipedia.org/wiki/Specials_(Unicode_block)</a>
 *    <li> <a href="http://en.wikipedia.org/wiki/Unicode_control_characters">http://en.wikipedia.org/wiki/Unicode_control_characters</a>
 *    <li> <a href="http://en.wikipedia.org/wiki/Byte_order_mark">http://en.wikipedia.org/wiki/Byte_order_mark</a>
 *    <li> <a href="http://unicode.org/faq/char_combmark.html">http://unicode.org/faq/char_combmark.html</a> - Characters and Combining Marks
 *    <li> <a href="http://unicode.org/cldr/utility/character.jsp?a=0301">http://unicode.org/cldr/utility/character.jsp?a=0301</a> - Unicode character property viewer
 *    <li> <a href="http://www.regular-expressions.info/unicode.html">http://www.regular-expressions.info/unicode.html</a> - Unicode Regular Expressions
 *    <li> <a href="http://stackoverflow.com/questions/3956734/why-does-the-java-char-primitive-take-up-2-bytes-of-memory">http://stackoverflow.com/questions/3956734/why-does-the-java-char-primitive-take-up-2-bytes-of-memory</a>
 *    <li> <a href="http://www.cl.cam.ac.uk/~mgk25/unicode.html">http://www.cl.cam.ac.uk/~mgk25/unicode.html</a> - UTF-8 and Unicode FAQ for Unix/Linux
 *    <li> <a href="http://unix.stackexchange.com/questions/139493/how-can-i-make-unicode-symbols-and-truetype-fonts-work-in-xterm-uxterm">http://unix.stackexchange.com/questions/139493/how-can-i-make-unicode-symbols-and-truetype-fonts-work-in-xterm-uxterm</a>
 *    <li> <a href="https://developer.apple.com/library/ios/documentation/Swift/Conceptual/Swift_Programming_Language/StringsAndCharacters.html">https://developer.apple.com/library/ios/documentation/Swift/Conceptual/Swift_Programming_Language/StringsAndCharacters.html</a> - Swift
 *    <li> <a href="http://www.cattlegrid.info/blog/2014/12/graphemes-code-points-characters-and-bytes.html">http://www.cattlegrid.info/blog/2014/12/graphemes-code-points-characters-and-bytes.html</a> - Perl 6
 * </ul>
 *
 *
 * <p> <a name="clubrules"></a><b>6. <a href="#contents">UTF-8 Club Rules</a></b>
 *
 * <p> <b>Rule 1.</b> UTF-8 is the only charset encoding.
 * <br><b>Rule 2.</b> See rule 1.
 *
 * @see @code java.nio.charset.StandardCharsets
 * @see @code java.text.BreakIterator
 * @see @code java.text.Normalizer
 * @see @code java.util.Locale
 */

public class string {


/* Here's a reminder of where we begin:
 * - A string in its simplest form is just a "cookie".
 * - A deficient string is any string which is not amenable to counting its
 *   "characters as the user perceives them" (i.e. graphemes).
 * - java.lang.String is a deficient string.


Questions needing research, discussion, thought, testing or all of the above:

 * - should the java.lang.String class itself be somehow 'enhanced' (with an
 *   even larger api), rather than add a new proper string, or replace
 *   altogether?
 *   - Backwards compatibility has a been a hallmark of Java for many years.
 *   - This has had some cost though - the build up of messy backwards
 *     compatibility APIs, language and syntax restrictions, the confusion upon
 *     newcomers to the language and its libraries, presumably at least some
 *     areas where performance is sacrificed, and most likely more.
 *   - At what point is it better to cut the apron strings and actually make a
 *     better world?
 *   - Of course, with all due duty of care: deprecations, notices of api break
 *     major version releases etc etc.
 * (An API break/ backwards compatibility break version of Java might also
 *  provide the opportunity to fix up a few other things while we're at it - it
 *  would effectively be a new but quite similar language (and JRE); sort of
 *  like the python-2 to python-3 transition, which Java has not had yet;
 *  - for example, bring on continuations, stack based variables, sse/matrix
 *    primitives, performant auto boxing, a performant native interface, and I'm
 *    sure most of us can think of plenty of api cleanups needed;
 *  - Java could finally, truly scale the heights attained by lisp so many years
 *    ago, not to mention put C# back in its place.
 * )

 * - should this class be a subclass of java.nio.ByteBuffer?
 * - should it have a similar looking api to ByteBuffer or same but extended?
 * - or should it contain a hidden or visible ByteBuffer, or byte[] ?

 * - which methods/ api should really be in string?
 * - and which should be in other classes such as encoding and iterator classes?
 * - is a minimal or maximal api better?

 * - perhaps the "ultimate" or "proper" string ought be just an interface?
 * - if an interface, then smaller api so it can be used more readily?
 * - or at least an associated abstract class to simplify "string like" class
 *   implementation
 * - an interface 'string' could provide fertile experimentation ground
 *    - high performance UTF-8 text file backed, memory mapped "string"
 *    - tiny minimal immutable string (sort of like java.lang.String),
 *      highest performance for general API use - small care free strings in a
 *      "direct" ByteBuffer - that'd be funny
 *    - extended 'heavy api' string with all sorts of unicode goodness?
 *      May not be needed, but if someone does need it, they ought be able to
 *      implement it and generally use it as a normal string.
 *    NOTWITHSTANDING, proper string.length MUST return the number of graphemes,
 *    somewhere, somehow, a string's length, AS SEEN BY USERS, must be easily
 *    determinable in the api for any "string" class worthy of the name!

 * - in any case this class needs to be "fixed" to be built on some type of byte (array)
 * - proper fixing probably requires jvm/jre updates


Regarding some of the draft implementation and api choices in this class:

 * - firstly, I created this to make my use of Unicode code points a bit easier,
 *   on my journey to eventually understanding that code points are not
 *   graphemes and that graphemes actually can't be counted yet in Java;
 *   and therefore most or possibly all of the api may need to change to be sane
 *   in general;
 *   I really would like Java to have a half decent string class.
 *
 * - get* methods are like java.lang.StringBuilder#getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin)
 *    - should these instead be named copy* ?
 *    - or something like ByteBuffer's complementary get and put methods?
 *      (symmetry is nice)
 *
 * - to* methods create and return something, like java.lang.String.toCharArray()
 *
 * - count* methods are named in this way:
 *    - to hint at possible execution time greater than O(1);
 *      although the execution time ought always be specified in the javadoc!
 *    - to avoid some of the (frankly rampant and) inappropriate overloading (in
 *      Java) of get* method naming
 *    - it's a bit shorter than "get...Count()" method naming
 *
 * - the methods taking StringBuilder args are almost certainly deprecated on
 *   arrival, in the sense that they are really just backwards compatibility
 *   machinery and we should probably be working exclusively (in general) with
 *   a corresponding ByteBuilder class (which doesn't exist yet either...)
 *
 * But please, any better ideas, bring em on - let's create the best string
 * class or interface worthy of the name, for the Java programming language!
 * It may well take a few years to get there, but the first step after
 * understanding Java's String nightmare is nailing the concept and an api.
 */


   private String text;       // java.lang.String version of this string
   private char[] chars;      // UTF-16 char[] version of this string
   private int[] cpcindices;  // Code point char indices (boundary indicies into a UTF-16 char[] - this.chars)
   private int cpcount = 0;   // Number of code points in this string

   // TODO:
   //private byte[] bytes;      // UTF-8 byte[] version of this string - ought be primary storage format
   //private int[] gbindices;   // Grapheme byte indices (boundary indicies into a UTF-8 byte sequence)
   //private int gcount = 0;    // Number of graphemes in this string


   /** Create an empty string. */
   public string () {}


   /**
    * Initialize a new string using {@code text} and default locale.
    * @see #setText(Object)
    */
   public string (Object text) {setText(text, Locale.getDefault());}


   /**
    * Initialize a new string using {@code text} and {@code locale}.
    * @see #setText(Object,Locale)
    */
   public string (Object text, Locale locale) {setText(text, locale);}


   /**
    * Count the number of Unicode UTF-8 code units required to UTF-8 encode this
    * string.
    *
    * <p>This implementation operates in O(n) time.
    */
   public int countUTF8Units () {return toBytes().length;}


   /**
    * Count the number of Unicode UTF-16 code units required to UTF-16 encode
    * this string.
    *
    * <p>This implementation operates in O(1) time.
    */
   public int countUTF16Units () {return chars.length;}


   /**
    * Returns the number of unicode code points in this string.
    *
    * <p>This is the same as the number of Unicode UTF-32 code units required to
    * UTF-32 encode this string.
    *
    * <p>This implementation operates in O(1) time.
    *
    * @return The code point count.
    */
   public int countCodePoints () {return cpcount;}


   /**
    * Returns the number of graphemes in this string;
    * Not implemented yet; throws UnsupportedOperationException.
    *
    * <p>This implementation operates in O(?) time.
    *
    * @return The grapheme count.
    * @param flags Grapheme types to include in the count.
    */
   public int countGraphemes (int flags) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Returns true if this string starts with the Unicode "Byte Order Mark"
    * character;
    * Not implemented yet; throws UnsupportedOperationException.
    *
    * <p>This implementation operates in O(1) time.
    */
   public boolean hasStartingBOM () {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Returns true if this string contains one or more Unicode "Byte Order Mark"
    * characters;
    * Not implemented yet; throws UnsupportedOperationException.
    *
    * @see #hasStartingBOM
    */
   public boolean hasBOM () {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Resets this grapheme string with {@code text} using default locale.
    */
   public void setText (Object text) {
      setText(text, Locale.getDefault());
   }


   /**
    * Resets this grapheme instance with {@code text} and specified {@code
    * Locale}.
    *
    * <p> Currently {@code text} may be a {@code java.lang.String} or {@code
    * char[]}.
    */
   public void setText (Object text, Locale locale) {
      // a lazy loading implementation might just save the text and process
      // later if/ when needed
      _applyText(text, locale);
   }

   private void _applyText (Object text, Locale locale) {
      //if (text == null) chars = "null";
      CharacterIterator ci;
      int textlen;
      if (text instanceof String) {
         String s = (String)text;
         this.text = s;
         textlen = s.length();
         s.getChars(0, textlen, chars = new char[textlen], 0);
         ci = new StringCharacterIterator(s);
      }
      else if (text instanceof char[]) {
         chars = (char[])text;
         textlen = chars.length;
         ci = new Segment(chars, 0, textlen);
      }
      //else if (text instanceof byte[]) ... locale ... TODO
      //else if (text instanceof CharacterIterator) ... TODO
      //else if (text instanceof CharSequence) ... TODO
      else throw new RuntimeException("Graphemes.setText: text=\"" + text + "\" not recognized.");
      // now index the chars:
      if (textlen == 0) {cpcount = 0; return;}
      BreakIterator cpbi = BreakIterator.getCharacterInstance(locale);
      cpbi.setText(ci);
      ArrayList newIndices = new ArrayList(textlen);
      int start = cpbi.first();
      for (int end = cpbi.next(); end != BreakIterator.DONE; start = end, end = cpbi.next())
         newIndices.add(start);
      newIndices.add(start); // after looping, add the final (enclosing) boundary index
      cpcount = newIndices.size() - 1;
      if (cpcount > 0) {
         //indices = newIndices.toArray();
         //indices = list.stream().mapToInt(i -> i).toArray();
         cpcindices = Ints.toArray(newIndices);
      } else cpcindices = null;
   }


   /**
    * Returns the grapheme boundary cpcindices into the utf-16 char[] used
    * internally; this array of indices is used internally.
    *
    * <p> Boundaries are calculated with BreakIterator.getCharacterInstance(Locale).
    * There are length() + 1 cpcindices, where each index is the location in the
    * utf-16 char[] of a grapheme boundary.
    */
   public int[] getCodePointUTF16Indices () {
      return cpcindices;
   }


   /**
    * Returns the code point at {@code cpindex}.
    */
   public int codePointToInt (int cpindex) {
      return toString().codePointAt(cpcindices[cpindex]);
   }


   /**
    * Returns the code points in the range {@code (cpistart, cpiend)}.
    * Not implemented yet; throws UnsupportedOperationException.
    * In current implementation, easy to implement.
    */
   public int[] codePointsToInts (int cpistart, int cpiend) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Returns this string as code points in an int[].
    */
   public int[] codePointsToInts () {
      int[] cpa = new int[cpcount];
      String text = toString();
      for (int i = cpcount - 1; i >= 0; --i) cpa[i] = text.codePointAt(cpcindices[i]);
      return cpa;
   }


   /**
    * Returns a UTF-16 encoded char[] of the code point at {@code cpindex}.
    *
    * <p>Code points are indexed from zero to {@code countCodePoints() - 1}.
    *
    * @param cpindex The code point index of the code point to return.
    *
    * @return A char[] of the UTF-16 encoded code point at {@code cpindex}.
    *
    * @throws IndexOutOfBoundsException if
    *    {@code index} is negative or greater than {@code this.length()} or
    *    {@code end} is less than -1 or greater than {@code this.length()}. 
    */
   public char[] codePointToChars (int cpindex) {
      // the corresponding method with a range rather than index handles
      // reversibility and so the following has slightly less overhead than
      // calling that method
      if (cpindex < 0 || cpindex >= cpcount)
         throw new IndexOutOfBoundsException("Graphemes.getGrapheme: cpindex=" + cpindex
            + ", countCodePoints=" + cpcount);
      int first = cpcindices[cpindex];
      int last = cpcindices[cpindex + 1];
      int nchar = last - first;
      char[] g = new char[nchar--];
      for (int i = --last; nchar >= 0; --i, --nchar) g[nchar] = chars[i];
      return g;
   }


   /**
    * Returns a UTF-16 encoded char[] range (reversible) of code points.
    *
    * <p>Code points are indexed from zero to {@code countCodePoints() - 1}.
    *
    * @param cpistart The code point index inclusive of the first code point to
    * return.
    * Valid range is {@code (0, this.countCodePoints() - 1)}.
    *
    * @param cpiend The code point index exclusive of the last code point to
    * return.
    * Valid range is {@code (-1, this.countCodePoints())}.
    *
    * @return A char[] of the UTF-16 encoded code points in the range requested.
    * If {@code cpiend < cpistart} then the code point sequence is reversed.
    * If {@code cpistart == cpiend} then an empty char[] is returned.
    *
    * @throws IndexOutOfBoundsException if
    *    {@code cpistart < 0} or {@code cpistart >= length()} or
    *    {@code cpiend < -1} or {@code cpiend > length()}. 
    */
   public char[] codePointsToChars (int cpistart, int cpiend) {
      // This impl might be made simpler, but no point since the 'reversible'
      // algorithm will be more important when we store in ByteBuffer; the
      // algorithm will essentially be identical, just based on bytes.
      if (cpistart < 0 || cpistart >= cpcount || cpiend < -1 || cpiend > cpcount)
         throw new IndexOutOfBoundsException("Graphemes.get: cpistart=" + cpistart
            + ", cpiend=" + cpiend + ", countCodePoints=" + cpcount);
      if (cpistart == cpiend) return new char[0];
      int delta;
      int cpclen; // code point char length
      if (cpistart < cpiend) {
         delta = 1;
         cpclen = cpcindices[cpiend+1] - cpcindices[cpistart];
      } else {
         delta = -1;
         cpclen = cpcindices[cpistart+1] - cpcindices[cpiend];
      }
      char[] g = new char[cpclen];
      for (int i = 0; cpistart != cpiend; cpistart += delta)
         for (int cpci = cpcindices[cpistart]; cpci < cpcindices[cpistart+1]; ++cpci)
            g[i++] = chars[cpci];
      return g;
   }


   /**
    * Returns a byte[] containing the chosen code point encoded as UTF-8.
    * Not implemented yet; throws UnsupportedOperationException.
    *
    * <p>Code points are indexed from zero to {@code countCodePoints() - 1}.
    *
    * @param cpindex The code point index of the code point to return.
    */
   public byte[] codePointsToBytes (int cpindex) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Returns a byte[] containing the chosen code points encoded as UTF-8.
    * Not implemented yet; throws UnsupportedOperationException.
    *
    * <p>Code points are indexed from zero to {@code countCodePoints() - 1}.
    *
    * @param cpistart The first code point index inclusive of the range of code
    * points to return.
    * @param cpiend The last code point index exclusive of the range of code
    * points to return.
    */
   public byte[] codePointsToBytes (int cpistart, int cpiend) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Returns a char[] containing the chosen grapheme encoded as UTF-16.
    * Not implemented yet; throws UnsupportedOperationException.
    *
    * <p>Graphemes are indexed from zero to {@code countGraphemes(flags) - 1}.
    *
    * @param gindex The grapheme index of the grapheme to return.
    */
   public char[] graphemesToChars (int flags, int gindex) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Not implemented yet; throws UnsupportedOperationException.
    */
   public char[] graphemesToChars (int flags, int gistart, int giend) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Not implemented yet; throws UnsupportedOperationException.
    */
   public byte[] graphemesToBytes (int flags, int gindex) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Not implemented yet; throws UnsupportedOperationException.
    */
   public byte[] graphemesToBytes (int flags, int gistart, int giend) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Returns all of this string encoded as a UTF-8 byte[].
    */
   public byte[] toBytes () {
      return toString().getBytes(StandardCharsets.UTF_8);
   }


   /**
    * Returns all of this string encoded as a UTF-16 char[].
    *
    * @return The char[] used internally, stored when setText was called.
    */
   public char[] toChars () {
      return chars;
   }


   /**
    * Returns this string converted into a {@code java.lang.String}.
    * For dignity this method ought be named toJavaString, but Object.toString
    * is a little engrained in the Java world...
    * Needs correctness tuning as this class is enhanced.
    */
   public String toString () {
      if (text == null) {
         text = new String(chars);
         //StringBuilder sb = new StringBuilder(chars.length);
         //for (int i = 0; i < cpcount; ++i) sb.append(chars[i], 0, chars.length);
         //text = sb.toString();
      }
      return text;
   }


   /**
    * Appends all of this string to a ByteBuilder (such class does not yet
    * exist).
    * Not implemented yet; throws UnsupportedOperationException.
    */
   public void appendTo (Object bb) {
      throw new UnsupportedOperationException("Not implemented");
   }

   /**
    * Inserts all of this string into a ByteBuilder (such class to be
    * implemented).
    * Not implemented yet; throws UnsupportedOperationException.
    */
   public void insertInto (int sbdest, Object bb) {
      throw new UnsupportedOperationException("Not implemented");
   }

   /**
    * Appends all of this string to a StringBuilder.
    */
   public void appendTo (StringBuilder sb) {
      sb.append(chars, 0, chars.length);
   }

   /**
    * Inserts all of this string into {@code sb}.
    */
   public void insertInto (int sbdest, StringBuilder sb) {
      sb.insert(sbdest, chars, 0, chars.length);
   }


   /**
    * Append all graphemes to {@code sb}.
    * Needs correctness tuning, currently ignores flags.
    *
    * <p>In general, minimize the number of overloaded/ similar methods.
    *
    * @param flags Working with graphemes needs flags, such as whether to
    * include various space types (non-breaking, zero-width, etc), tabs, various
    * newlines, sentence and paragraph ending characters and more.
    */
   public void appendGraphemesTo (int flags, StringBuilder sb) {
      sb.append(chars, 0, chars.length);
   }


   /**
    * Insert graphemes into sb.
    * Needs correctness tuning, currently ignores flags.
    *
    * @param flags Working with graphemes needs flags, such as whether to
    * include various space types (non-breaking, zero-width, etc), tabs, various
    * newlines, sentence and paragraph ending characters and more.
    */
   public void insertGraphemesInto (int flags, int sbdest, StringBuilder sb) {
      sb.insert(sbdest, chars, 0, chars.length);
   }


   /**
    * Not implemented yet; throws UnsupportedOperationException.
    */
   public void appendGraphemesTo (int flags, StringBuilder sb, int gistart, int giend) {
      throw new UnsupportedOperationException("Not implemented");
   }


   /**
    * Not implemented yet; throws UnsupportedOperationException.
    */
   public void insertGraphemesInto (int flags, int sbdest, StringBuilder sb, int gistart, int giend) {
      throw new UnsupportedOperationException("Not implemented");
   }



   /**
    * Append code points in the range {@code (cpistart, cpiend)} into {@code
    * sb};  {@code cpistart} may be less than {@code cpiend} in which case the
    * code points are appended in reverse order.
    *
    * @param cpistart The first code point (index inclusive) to append to sb.
    * Valid range is from 0 to (this.countCodePoints() - 1).
    * @param cpiend The last code point (index, exclusive) to append to sb.
    * Valid range is from -1 to this.countCodePoints().
    * If cpend is less than cpstart, code points are appended in reverse order.
    * @throws IndexOutOfBoundsException if cpstart or cpend are out of valid
    * range.
    */
   public void appendCodePointsTo (StringBuilder sb, int cpistart, int cpiend) {
      // we could just call insertCodePointsInto, but the following is higher
      // performance, which might be considered important for java.lang.string
      if (cpistart < 0 || cpistart >= cpcount || cpiend < -1 || cpiend > cpcount)
         throw new IndexOutOfBoundsException("Graphemes.appendTo: cpistart=" + cpistart
            + ", cpiend=" + cpiend + ", countCodePoints=" + cpcount);
      if (cpistart == cpiend) return;
      int delta = (cpiend < cpistart) ? -1 : 1;
      for (int i = cpistart; i != cpiend; i += delta) sb.append(chars, cpcindices[i], cpcindices[i+1]);
   }


   /**
    * Inserts a range (reversible) of code points into a StringBuilder.
    *
    * <p>Code points are indexed from zero to {@code countCodePoints() - 1}.
    *
    * <p>If {@code cpiend < cpistart} then the code point sequence is reversed.
    *
    * <p>This method might be one or two too many - perhaps better to simplify
    * (reduce) the API rather than eek out only a little extra performance and
    * only for the measley StringBuilder class - what we need to start chugging
    * with is ByteBuffer!
    *
    * @param sbdest Destination in {@code sb} in which to insert the chosen code
    * points (as UTF-16 chars).
    *
    * @param cpistart The code point index inclusive of the first code point to
    * insert into sb.
    * Valid range is {@code (0, this.countCodePoints() - 1)}.
    *
    * @param cpiend The code point index exclusive of the last code point to
    * insert into sb.
    * Valid range is {@code (-1, this.countCodePoints())}.
    *
    * @throws IndexOutOfBoundsException if
    *    {@code cpistart < 0} or {@code cpistart >= length()} or
    *    {@code cpiend < -1} or {@code cpiend > length()} or
    *    {@code sbdest} is not a valid location in {@code sb}.
    */
   public void insertCodePointsInto (int sbdest, StringBuilder sb, int cpistart, int cpiend) {
      if (cpistart < 0 || cpistart >= cpcount || cpiend < -1 || cpiend > cpcount)
         throw new IndexOutOfBoundsException("Graphemes.appendTo: cpistart=" + cpistart
            + ", cpiend=" + cpiend + ", countCodePoints=" + cpcount);
      if (cpistart == cpiend) return;
      int delta = (cpiend < cpistart) ? -1 : 1;
      tmplen = (cpistart-cpiend) * 2 * delta;
      StringBuilder tmp = new StringBuilder(tmplen);
      for (int i = cpistart; i != cpiend; i += delta) tmp.append(chars, cpcindices[i], cpcindices[i+1]);
      sb.insert(sbdest, tmp);
   }


   /** Returns a verbosely descriptive java.lang.String of this string. */
   public String toDebugString () {
      return "zen.lang.string: countCodePoints=" + cpcount + " cpcindices=" + Arrays.toString(cpcindices);
   }


   /** tests */
   public static void main (String[] args) {
      string s = new string("");
      System.out.println(s);
   }

}
