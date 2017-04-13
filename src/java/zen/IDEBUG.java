// Copyright (C) 2017, Zenaan Harkness, see files LICENSE and contact.txt for details.

package zen;


/**
 * Debugging/ logging &amp; category compile-out constants.
 *
 * These provide compile time guards/ switches for logging/ debugging
 * (switch off even compilation of logging statements if desired).
 *
 * Changes to this class should not be pushed upstream - use a local
 * instance of this file in your compile CLASSPATH to override these
 * values/ override this class and avoid upstreaming changes made merely
 * for debugging.
 */
public interface IDEBUG {

   /**
    * Assertions override;
    * WARNING: important/ "essential" assertions shall not be excluded by this
    * override, thus it is safe to set this to false, for a "release" build.
    */
   boolean ASSERT    = true;

   /**
    * Application "verbose" override.
    * When false, no verbose output will be compiled in.
    * For "release" builds, this should always be true!
    */
   boolean VERBOSE   = true;

   /**
    * Loggers creation &amp; initialization override;
    * disables all logging.
    */
   boolean LOGGERS   = true;

   /** Turns off logging/ fancy appending operations. */
   boolean LOG       = true;

   /** LOG4J compile time switch to turn on ALL logging levels. */
   boolean ALL       = false;

   boolean FATAL     = LOGGERS && ALL || true;
   boolean ERROR     = LOGGERS && ALL || (FATAL && true);
   boolean WARN      = LOGGERS && ALL || (ERROR && true);
   boolean INFO      = LOGGERS && ALL || (WARN  && true);
   boolean DEBUG     = LOGGERS && ALL || (INFO  && true);

   /** Generic/lower logging levels for finer-grained logging control. */
   boolean DEBUG0    = LOGGERS && ALL || (DEBUG  && true);
   boolean DEBUG1    = LOGGERS && ALL || (DEBUG0 && true);
   boolean DEBUG2    = LOGGERS && ALL || (DEBUG1 && true);
   boolean DEBUG3    = LOGGERS && ALL || (DEBUG2 && true);


   /** Test harness/ test generation code. */
   boolean TEST      = ALL || true;

   /** Server side code. */
   boolean SERVER    = ALL || true;

   /** Switch for debug of fireEvent type calls. */
   boolean DEBUG_FIRE_EVENT = ALL || true;

   /**
    * @see bten.util.UDebug.debug(Object)
    * @see bten.util.UDebug.debug(Object,Throwable)
    */
   boolean DEBUG_RAW = ALL || true;


   boolean DEBUG_EXCLUDE_STATE_EVENTS = ALL || true;

 //boolean DEBUG_ = ALL || true;


   char CHAR_QUOTE_START = '`';
   char CHAR_QUOTE_END = '\'';

   default StringBuilder debugChar (int cp) {
      return debugQuoteChar(new StringBuilder(4), cp).append(' ');
   }

   default StringBuilder debugQuoteChar (int cp) {return debugQuoteChar(new StringBuilder(4), cp);}

   default StringBuilder debugQuoteChar (StringBuilder sb, int cp) {
      return sb
         .append(CHAR_QUOTE_START)
         .appendCodePoint(cp)
         .append(CHAR_QUOTE_END)
         ;
   }

}

