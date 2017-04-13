// Copyright (C) 2017, Zenaan Harkness, see files LICENSE and contact.txt for details.

package zen.util;


/**
 * String static convenience methods.
 */
public class UString {


   /** Convert a code point to a string. */
   public static String cp2s (int codepoint) {return new String(Character.toChars(codepoint));}


   /**
    * Strip whitespace from left of string.
    * NOTE this is different to String.trim!
    *
    * See http://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java
    */
   public static StringBuilder lstrip(StringBuilder s) {
      int len = s.length();
      int i = 0;
      while (i < len && Character.isWhitespace(s.codePointAt(i))) i = s.offsetByCodePoints(i,1);
      return s.delete(0,i);
   }


   /**
    * Strip whitespace from right of string.
    * NOTE this is different to String.trim!
    *
    * See http://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java
    */
   public static StringBuilder rstrip(StringBuilder s) {
      int len = s.length();
      int i = len;
      while (i > 0 && Character.isWhitespace(s.codePointBefore(i))) i = s.offsetByCodePoints(i,-1);
      return s.delete(i,len);
   }


   /**
    * Strip chars in set, from left of string.
    *
    * @param chars The char filter set, of characters (in any order) to strip from left of s.
    * If set is null, then whitespace is stripped.
    */
   public static StringBuilder lstrip(StringBuilder s, CharSequence chars) {
      if (chars == null) return lstrip(s);
      if (chars.length() == 0) return s;
      String set = chars.toString();
      int len = s.length();
      int i = 0;
      while (i < len && set.indexOf(s.codePointAt(i)) != -1) i = s.offsetByCodePoints(i,1);
      return s.delete(0,i);
   }


   /**
    * Strip chars in set, from right of string.
    * NOTE this is different to String.trim!
    *
    * @param chars The char filter set, of characters (in any order) to strip from right of s.
    * If set is null, then whitespace is stripped.
    */
   public static StringBuilder rstrip(StringBuilder s, CharSequence chars) {
      if (chars == null) return rstrip(s);
      if (chars.length() == 0) return s;
      String set = chars.toString();
      int len = s.length();
      int i = len;
      while (i > 0 && set.indexOf(s.codePointBefore(i)) != -1) i = s.offsetByCodePoints(i,-1);
      return s.delete(i,len);
   }


   /**
    * Null-safe method to replace all line terminators in a string.
    * If s is null, null is returned.
    */
   public static String replaceAllLineTerminators (String s, String replacement) {
      if (s == null) return s;
      final String LINE_TERMINATORS_CLASS = "\\n|\\r|\\r\\n|\\u0085|\\u2028|\\u2029";
      return s.replaceAll(LINE_TERMINATORS_CLASS, replacement);
   }


   /**
    * Repeats s, count times.
    */
   public static String repeat (String s, int count) {
      final int len = s.length();
      final StringBuilder sb = new StringBuilder(count * len);
      for (int i = count; i != 0; --i) sb.append(s);
      return sb.toString();
   }


}

