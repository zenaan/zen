// Copyright (C) 2017, Zenaan Harkness, see files LICENSE and contact.txt for details.

package zen.lang;

import zen.IDEBUG;

import java.lang.IndexOutOfBoundsException;

/**
 * A bidirectional code point cursor to iterate the code points of a {@link java.lang.String}.
 *
 * Being bidirectional, also supports push_back functionality, by calling {@code advance(int)}
 * with a negative distance.
 *
 * Note that this class is not Java-idiom perfect, erring on the side of extra
 * functionality, optional performance methods (to avoid Exception throwing overhead),
 * and not only bidirectional operation, but moving forwards and or backwards in steps
 * of greater than one; and the cursor can be reused, with the direction changed as well,
 * with a call to {@link reset(boolean)}. One could certainly fashion a simpler class
 * based on this class to more accurately model "normal" Java idioms, if one so desired.
 *
 * @author Zenaan Harkness
 */
public class CodePointCursor implements IDEBUG {

   /**
    * Creates a "null" CodePointCursor - {@link hasNext()} will always return false.
    */
   public CodePointCursor () {}

   /**
    * Creates a forwards CodePointCursor based on {@code s}.
    */
   public CodePointCursor (String s) {this(s, false);}

   /**
    * Creates a CodePointCursor based on {@code s}, calls {@link reset(reverse)}.
    */
   public CodePointCursor (String s, boolean reverse) {
      this.s = s;
      sculen = s == null ? 0 : s.length();
      if (sculen == 0) {
         scplen = 0;
         ilast = -1;
      } else {
         scplen = s.codePointCount(0,sculen);
         ilast = s.offsetByCodePoints(sculen,-1);
      }
      reset(reverse);
   }

   /**
    * This cursor can be reset at any time.
    *
    * <p> The heart of simplifying: ensure that start and end (for i) are algorithmically
    * consistent, whether going forwards or reversing; in this case:
    *
    * <br> - start is starting code unit index for code point extraction, -exclusive-
    * <br> - end is ending code unit index for code point extraction, -inclusive-
    *
    * <p> (First tried start inclusive/ end exclusive, but that makes curr() and remove()
    * methods (if included) relatively more awkward; so the way it is is the way for now.)
    *
    * @param reverse If true, this cursor will start at the last code point in the underlying
    * string and go backwards, otherwise, start at the beginning of the underlying string.
    */
   public CodePointCursor reset (boolean reverse) {
      if (reverse) {
         d = -1;
         if (sculen == 0) {
            istart   = -1;
            iend     = -1;
            cpistart = -1;
            cpiend   = -1;
         } else {
            istart   = sculen;
            iend     = 0;
            cpistart = scplen;
            cpiend   = 0;
         }
      } else {
         d = 1;
         istart      = -1;
         cpistart    = -1;
         if (sculen == 0) {
            iend     = -1;
            cpiend   = -1;
         } else {
            iend     = ilast;
            cpiend   = scplen - 1;
         }
      }
      i = istart;
      cpi = cpistart;
      return this;
   }

   /**
    * If true, and {@link zen.IDEBUG.INFO} is true/enabled, then more verbose
    * error messages based on {@code this.toString()} will be produced.
    */
   public void setDebug (boolean debug) {this._debug = debug;}

   boolean _debug = false;

   private String s;
   private int sculen, scplen; // code units, code points

   private int d; // direction

   private int istart;
   private int iend;
   private int ilast;
   private int i; // string index

   private int cpistart;
   private int cpiend;
   private int cpi; // code point index


   /** Return the number of code points this cursor traverses. */
   public int getCPLen () {return scplen;}


   /**
    * Returns the current code point index for this cursor,
    * where starting index is inclusive
    * (0 for a forwards iterator, or string.codePointCount() - 1 for a reverse iterator),
    * and ending index is exclusive
    * (string.codePointCount() for a forwards iterator, and -1 for a reverse iterator).
    */
   public int getCPIdxIn () {return cpi + d;}

   /**
    * Returns the current code point index for this cursor,
    * where starting index is exclusive
    * (-1 for a forwards iterator, or string.codePointCount() for a reverse iterator),
    * and ending index is inclusive
    * (string.codePointCount() - 1 for a forwards iterator, and 0 for a reverse iterator).
    */
   public int getCPIdxEx () {return cpi;}

   /* This method does not look very useful, even if it were made to work.
    * Returns the current code unit index (in the underlying string) for this cursor,
    * where starting index is inclusive
    * (0 for a forwards iterator, or string.length() - 1 for a reverse iterator),
    * and ending index is exclusive
    * (string.length() for a forwards iterator, and -1 for a reverse iterator).
    *
    * <p> Code units correspond to Java char instances - this index corresponds to a location in
    * a Java char array or String.
    */
   //public int getCUIdxIn () {return i + d;} << implementation is incorrect

   /**
    * Returns the current code unit index (in the underlying string) for this cursor,
    * where starting index is exclusive
    * (-1 for a forwards iterator, or string.length() for a reverse iterator),
    * and ending index is inclusive
    * (string.length() - 1 for a forwards iterator, and 0 for a reverse iterator).
    *
    * <p> After {@link next()} has been called at least once with no error, this method will
    * return the current string index, in the underlying string, of the current code point that
    * this cursor points at.
    *
    * <p> Code units correspond to Java char instances - this index corresponds to a location in
    * a Java char array or String.
    */
   public int getCUIdxEx () {return i;}


   /**
    * Return true if there is at least one more code point to be traversed by this cursor.
    * This method returns the same as calling {@link hasNext(int)} with a parameter of 1,
    * but is more performant.
    */
   public boolean hasNext () {return i != iend;} // | hasNext(1)

   /**
    * Return true if there are at least {@code n} more code points to be traversed by this
    * cursor.
    * @param n May of course be positive, but may also be negative (have n code points been
    * traversed) or zero (is current code point valid);
    * n == 1 will give the same result as {@link hasNext()}, but this method is less performant.
    */
   public boolean hasNext (int n) {
      int cptarget = cpi + n * d;
      return 0 <= cptarget && cptarget < scplen;
   }


   /**
    * Return the code unit index in the underlying string for the next code point.
    */
   public int peekIdx () throws IndexOutOfBoundsException  {
      if (!hasNext()) throw new IndexOutOfBoundsException("No hasNext(): " + this);
      return peekIdxNoCheck();
   }

   /*
    * Return the code unit index in the underlying string for the next code point,
    * without checking hasNext() and throwing a tidy IndexOutOfBoundsException when needed;
    * be warned that if you get something wrong, using this method can make it hard to debug.
    */
   private int peekIdxNoCheck () {return i == -1 ? 0 : s.offsetByCodePoints(i,d);}

   /**
    * Return the code unit index in the underlying string for the {@code n}'th next code point.
    * @param n May be of course be positive, but may also be negative (return earlier code point
    * index) or zero (return current code point index); n == 1 is the same as
    * {@link peekIdx()}.
    */
   public int peekIdx (int n) throws IndexOutOfBoundsException  {
      if (!hasNext(n)) throw new IndexOutOfBoundsException("No hasNext(" + n + "): " + this);
      return peekIdxNoCheck(n);
   }

   /*
    * Return the code unit index in the underlying string for the {@code n}'th next code point,
    * without checking {@link hasNext(int)} and throwing a tidy IndexOutOfBoundsException when
    * needed;
    * be warned that if you get something wrong, using this method can make it hard to debug.
    *
    * @param n May be of course be positive, but may also be negative (return earlier code point
    * index) or zero (return current code point index); n == 1 is the same as
    * {@link peekIdxNoCheck()}.
    */
   private int peekIdxNoCheck (int n) {
      if (n == 0) return i;
      int i = peekIdxNoCheck(); // initial offset is sometimes unique condition
      return n == 1 ? i : s.offsetByCodePoints(i, d * (n-1));
   }


   /**
    * Advance the cursor if a valid index exists, throw exception otherwise.
    * @return The next code point index in the underlying string.
    */
   public int advance () throws IndexOutOfBoundsException {
      // Peek first, so errors are thrown without updating anything.
      // This allows the use of the try {i.next();} catch (Exception) pattern without causing
      // this cursor to become self-inconsistent as a result:
      i = peekIdx();
      cpi += d;
      return i;
   }

   /**
    * Advance the cursor to the {@code n}'th next code point index if such an index exists,
    * throw an error otherwise.
    *
    * @param n May be of course be positive, but may also be negative (advance backwards)
    * or zero (return current code point index, but more efficient to call {@link peekIdx} in
    * this case);
    * {@code n == 1} is the same as {@link peekIdx()}.
    *
    * @return The next code point index in the underlying string.
    */
   public int advance (int n) throws IndexOutOfBoundsException  {
      // Peek first, so errors are thrown without updating anything.
      // This allows the use of the try {i.next();} catch (Exception) pattern without causing
      // this cursor to become self-inconsistent as a result:
      i = peekIdx(n);
      cpi += d * n;
      return i;
   }


   /**
    * Return the next code point in this sequence, from the current cursor position, without
    * advancing the cursor;
    * if {@link hasNext()} currently returns false, this method will throw IndexOutOfBoundsException.
    */
   public int peek () {return s.codePointAt(peekIdx());} // | peek(1)

   /**
    * Return the {@code n}'th next code point in this sequence, from the current cursor
    * position, without advancing the cursor;
    * if {@link hasNext(n)} currently returns false, this method will throw IndexOutOfBoundsException.
    *
    * @param n may be 0, positive or negative.
    */
   public int peek (int n) {return s.codePointAt(peekIdx(n));}

   /**
    * Return the next code point in this sequence, and advance the cursor,
    * if {@link hasNext()} currently returns false, this method will throw IndexOutOfBoundsException.
    */
   public int next () {return s.codePointAt(advance());} // | s.codePointAt(advance(1))

   /**
    * Return the next code point in this sequence, and advance the cursor by {@code n},
    * if {@link hasNext(n)} currently returns false, this method will throw IndexOutOfBoundsException.
    *
    * @param n may be 0, positive or negative.
    */
   public int next (int n) {return s.codePointAt(advance(n));}

   /**
    * Return the current code point this cursor points at;
    * if {@link hasNext(0)} currently returns false, this method will throw IndexOutOfBoundsException.
    */
   public int curr () {return s.codePointAt(i);} // | next(0) | peek(0) | s.codePointAt(advance(0))

   /**
    * Return the previous code point in this sequence, and advance the cursor backwards,
    * if {@link hasNext(-1)} currently returns false, this method will throw IndexOutOfBoundsException.
    */
   public int prev () {return s.codePointAt(advance(-1));} // | next(-1)


   private String debug () {
      return "CPCursor(\""+s+"\")"
         + ", istartEx " + istart
         + ", iendIn " + iend
         + ", direction " + d
         + ", iEx " + i
         + ", cpLen " + scplen
         + ", cpiEx " + cpi
         ;
   }

   public String toString () {
      return INFO && _debug ? debug() : "(" + istart + ',' + getCUIdxEx() + ',' + iend + ')';
   }

}

