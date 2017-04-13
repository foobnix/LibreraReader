/*
 * DateType.java May 2007
 *
 * Copyright (C) 2007, Niall Gallagher <niallg@users.sf.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 */

package org.simpleframework.xml.transform;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The <code>DateType</code> enumeration provides a set of known date
 * formats supported by the date transformer. This allows the XML
 * representation of a date to come in several formats, from most 
 * accurate to least. Enumerating the dates ensures that resolution
 * of the format is fast by enabling inspection of the date string. 
 * 
 * @author Niall Gallagher
 */
enum DateType {
   
   /**
    * This is the default date format used by the date transform.
    */
   FULL("yyyy-MM-dd HH:mm:ss.S z"),
   
   /**
    * This is the date type without millisecond resolution.
    */
   LONG("yyyy-MM-dd HH:mm:ss z"),
   
   /**
    * This date type enables only the specific date to be used.
    */
   NORMAL("yyyy-MM-dd z"),
   
   /**
    * This is the shortest format that relies on the date locale.
    */
   SHORT("yyyy-MM-dd");

   /**
    * This is the date formatter that is used to parse the date.
    */
   private DateFormat format;

   /**
    * Constructor for the <code>DateType</code> enumeration. This
    * will accept a simple date format pattern, which is used to
    * parse an input string and convert it to a usable date.
    * 
    * @param format this is the format to use to parse the date
    */
   private DateType(String format) {
      this.format = new DateFormat(format);         
   }

   /**
    * Acquires the date format from the date type. This is then 
    * used to parse the date string and convert it to a usable
    * date. The format returned is synchronized for safety.
    * 
    * @return this returns the date format to be used
    */
   private DateFormat getFormat() {
      return format;         
   }
   
   /**
    * This is used to convert the date to a string value. The 
    * string value can then be embedded in to the generated XML in
    * such a way that it can be recovered as a <code>Date</code>
    * when the value is transformed by the date transform.
    * 
    * @param date this is the date that is converted to a string
    * 
    * @return this returns the string to represent the date
    */
   public static String getText(Date date) throws Exception {
      DateFormat format = FULL.getFormat();
      
      return format.getText(date);
   }
   
   /**
    * This is used to convert the string to a date value. The 
    * date value can then be recovered from the generated XML by
    * parsing the text with one of the known date formats. This
    * allows bidirectional transformation of dates to strings.
    * 
    * @param text this is the date that is converted to a date
    * 
    * @return this returns the date parsed from the string value
    */
   public static Date getDate(String text) throws Exception {
      DateType type = getType(text);
      DateFormat format = type.getFormat();
      
      return format.getDate(text);
   }

   /**
    * This is used to acquire a date type using the specified text
    * as input. This will perform some checks on the raw string to
    * match it to the appropriate date type. Resolving the date type
    * in this way ensures that only one date type needs to be used.
    * 
    * @param text this is the text to be matched with a date type
    * 
    * @return the most appropriate date type for the given string
    */
   public static DateType getType(String text) {
      int length = text.length();

      if(length > 23) {
         return FULL;
      }
      if(length > 20) {
         return LONG;
      }
      if(length > 11) {
         return NORMAL;
      }
      return SHORT;
   }
   
   /**
    * The <code>DateFormat</code> provides a synchronized means for
    * using the simple date format object. It ensures that should 
    * there be many threads trying to gain access to the formatter 
    * that they will not collide causing a race condition.
    * 
    * @author Niall Gallagher
    */
   private static class DateFormat {
      
      /**
       * This is the simple date format used to parse the string.
       */
      private SimpleDateFormat format;
      
      /**
       * Constructor for the <code>DateFormat</code> object. This will
       * wrap a simple date format, providing access to the conversion
       * functions which allow date to string and string to date.
       * 
       * @param format this is the pattern to use for the date type
       */
      public DateFormat(String format) {         
         this.format = new SimpleDateFormat(format);
      }
      
      /**
       * This is used to provide a transformation from a date to a string.
       * It ensures that there is a bidirectional transformation process
       * which allows dates to be serialized and deserialized with XML.
       * 
       * @param date this is the date to be converted to a string value
       * 
       * @return returns the string that has be converted from a date
       */
      public synchronized String getText(Date date) throws Exception {
         return format.format(date);
      }
      
      /**
       * This is used to provide a transformation from a string to a date.
       * It ensures that there is a bidirectional transformation process
       * which allows dates to be serialized and deserialized with XML.
       * 
       * @param text this is the string to be converted to a date value
       * 
       * @return returns the date that has be converted from a string
       */
      public synchronized Date getDate(String text) throws Exception {
         return format.parse(text);
      }
   }
}  