// Copyright (C) 2017, Zenaan Harkness, see files LICENSE and contact.txt for details.

package zen.lang;

import zen.IDEBUG;
import zen.util.UString;

import java.lang.IndexOutOfBoundsException;

/**
 * Some simple parsing using CodePointCursor, supporting an optional literals escape char
 * (currently bounded to sane ASCII chars).
 *
 * Very limited functionality (only {@link parseString} and {@link parseULong}),
 * but that which is here is well tested.
 *
 * Uses traditional parsing idioms and not standard Java idioms - feel free to
 * write a friendlier class :)
 *
 * @author Zenaan Harkness
 */
public class CodePointParser implements IDEBUG {

   private CodePointParser () {}
   public  CodePointParser (CodePointCursor i) {this.i = i;}

   private CodePointCursor i;

   public class CPParserException extends RuntimeException {
      public CPParserException (String s) {super(s);}
   }

   /**
    * Set a literals escape char - any char appearing after this "escape char" is included as a
    * literal when parsing a string, or a digit when parsing a number (a non-digit when parsing
    * a number would end the parsing of a number).
    *
    * <p> Valid literals escape characters are basically the 8 bit ASCII visible characters from
    * space to tilde (' ' to '~' with integer values 32 to 126).
    *
    * <p> Use zero to unset the literals escape char.
    *
    * @param escapecp The character code point to use as a literals escape char.
    */
   public void setEscape (int escapecp) {
      if (escapecp != 0 && escapecp < ' ' || escapecp > '~')
         throw new CPParserException("Invalid escape code point: (" + escapecp + ") '"
            + UString.cp2s(escapecp) + '\'');
      lescape = escapecp;
   }

   private int lescape; // literal escape

   /** Return true if a literals escape has been set. */
   public boolean hasEscape () {return lescape != 0;}

   public String toString () {
      return "CPParser( " + i
         + ", e" + (hasEscape() ? new String(Character.toChars(lescape)) : "")
         + " )";
   }

   /** Calls {@link parseString(StringBuilder,end_char,end_char)}. */
   public int parseString (StringBuilder result, int end_char) {
      return parseString(result, end_char, end_char, messages);
   }

   public int parseString (StringBuilder result, int end_char1, int end_char2) {
      return parseString(result, end_char1, end_char2, messages);
   }

   public static final int CURSOR_END = -1;
   public static final int ESCAPE_AT_END = -2;

   /**
    * Parse a sequence of code points starting at {@code CodePointCursor i.next()}
    * (see parameter {@code i} in the constructor),
    * honouring literal escapes (if {@link setEscape(int)} has been called)
    * and advancing the cursor accordingly.
    *
    * <p> Honours literal escapes if {@code setEscape(int)} has been called setting a valid
    * escape char.
    * If the literal_escape char is encountered, it is not consumed if no char follows it (i.e.
    * end of string is reached), and in this case parsing stops
    * and the cursor is left pointing at the final escape char
    * (fwiw, CodePointCursor is algorithmically "exclusive").
    *
    * <p> Nice debugging messages are supported, which may optionally be compiled in
    * or out of this class depending on performance needs.
    *
    * @return The end char (code point) found ({@link end_char1} or {@link end_char2}),
    * or {@link CURSOR_END} if cursor end reached before either end char is found,
    * or {@link ESCAPE_AT_END} if the last char of the string is the escape char (and no char
    * follows it, see {@link setEscape}) and in this case the escape char is not consumed and
    * not appended to {@code result}.
    *
    * @see setEscape
    */
   public int parseString (StringBuilder result, int end_char1, int end_char2, Messages m) {
      if (result == null) result = new StringBuilder();
      StringBuilder dsb = INFO && i._debug ? new StringBuilder() : null;
      try {
         while (i.hasNext()) {
            int next = i.next();
            if (INFO && i._debug) dsb.append(debugChar(next));
            if (hasEscape() && next == lescape) {
               if (INFO && i._debug) dsb.append(m.escape);
               if (!i.hasNext()) {
                  if (INFO && i._debug) dsb.append(m.atEnd).append(m.pushBack);
                  i.advance(-1);
                  return ESCAPE_AT_END;
               }
               // next char treated as a literal:
               next = i.next();
               if (INFO && i._debug) dsb.append(debugChar(next)).append(m.literal);
            }
            else if (next == end_char1 || next == end_char2) {
               if (INFO && i._debug) dsb.append(m.endChar);
               return next;
            }
            result.appendCodePoint(next);
         }
         if (INFO && i._debug) dsb.append(m.atEnd);
         return CURSOR_END;
      } finally {
         if (INFO && i._debug) System.err.print(dsb);
      }
   }

   private static final Messages messages = new Messages();

   /**
    * Messages to facilitate nice step-parsing debugging output.
    */
   public static class Messages {
      public String noDigit   = "<no digit> ";
      public String endChar   = "<end_char> ";
      public String atEnd     = "<at end> ";
      public String escape    = "<escape> ";
      public String literal   = "<literal> ";
      public String pushBack  = "<PUSH BACK> ";
   }

   /** Calls {@link parseULong(defaultVal,10,messages)}. */
   public long parseULong (long defaultVal) {return parseULong(defaultVal, 10, messages);}

   /**
    * Parse a sequence of zero or more optionally {@link setEscape escaped} digits, into
    * an (unsigned) long, starting at {@code this.next()} and advancing the cursor accordingly.
    *
    * <p> No leading + or - sign is allowed.
    * Digits are parsed until either a non digit or the end of the underlying string is reached.
    *
    * <p> Honours literal escapes if {@code setEscape(int)} has been called setting a valid
    * escape char.
    * If the literal_escape char is encountered, it is not consumed if no digit follows it,
    * and in this case parsing stops.
    *
    * <p> No numeric overflow detection is done.
    * I.e., overflows to negative, and then back past zero, I think - definitely do boundary
    * testing if you need to know the exact details, and flick us a link to your results
    * please.
    *
    * @param defaultVal The value to return if no digits are found.
    * @param base The numeric base (e.g. base 10) of the number to be parsed.
    *
    * @return The result, or {@link defaultVal} if no digits are found; this parser consumes all
    * digits found, but only consumes an escape char if it escapes a digit.
    *
    * @see setEscape
    */
   public long parseULong (long defaultVal, int base, Messages m) {
      StringBuilder dsb = INFO && i._debug ? new StringBuilder() : null;
      try {
         if (!i.hasNext()) {
            if (INFO && i._debug) dsb.append(m.atEnd);
            return defaultVal;
         }
         int next = i.next();
         if (INFO && i._debug) dsb.append(debugChar(next));
         if (hasEscape() && next == lescape) {
            if (INFO && i._debug) dsb.append(m.escape);
            if (!i.hasNext()) {
               if (INFO && i._debug) dsb.append(m.atEnd).append(m.pushBack);
               i.advance(-1);
               return defaultVal;
            }
            next = i.next();
            if (INFO && i._debug) dsb.append(debugChar(next));
            if (!Character.isDigit(next)) {
               if (INFO && i._debug) dsb.append(m.noDigit).append(m.pushBack).append(m.pushBack);
               i.advance(-2);
               return defaultVal;
            }
         } else {
            if (!Character.isDigit(next)) {
               if (INFO && i._debug) dsb.append(m.noDigit).append(m.pushBack);
               i.advance(-1);
               return defaultVal;
            }
         }
         long result = Character.digit(next, base);
         while (true) {
            if (!i.hasNext()) {
               if (INFO && i._debug) dsb.append(m.atEnd);
               return result;
            }
            next = i.next();
            if (INFO && i._debug) dsb.append(debugChar(next));
            if (hasEscape() && next == lescape) {
               if (INFO && i._debug) dsb.append(m.escape);
               if (!i.hasNext()) {
                  if (INFO && i._debug) dsb.append(m.atEnd).append(m.pushBack);
                  i.advance(-1);
                  return result;
               }
               next = i.next();
               if (INFO && i._debug) dsb.append(debugChar(next));
               if (!Character.isDigit(next)) {
                  if (INFO && i._debug) dsb.append(m.noDigit).append(m.pushBack).append(m.pushBack);
                  i.advance(-2);
                  return result;
               }
            } else {
               if (!Character.isDigit(next)) {
                  if (INFO && i._debug) dsb.append(m.noDigit).append(m.pushBack);
                  i.advance(-1);
                  return result;
               }
            }
            result = result * base + Character.digit(next, base);
         }
         //return result;
      } finally {
         if (INFO && i._debug) System.err.print(dsb);
      }
   }

}

