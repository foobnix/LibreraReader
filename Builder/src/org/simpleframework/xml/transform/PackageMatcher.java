/*
 * PackageMatcher.java May 2007
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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The <code>PackageMatcher</code> object is used to match the stock
 * transforms to Java packages. This is used to match useful types 
 * from the <code>java.lang</code> and <code>java.util</code> packages
 * as well as other Java packages. This matcher groups types by their
 * package names and attempts to search the stock transforms for a
 * suitable match. If no match can be found this throws an exception.
 *  
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.transform.DefaultMatcher
 */
class PackageMatcher implements Matcher {
   
   /**
    * Constructor for the <code>PackageMatcher</code> object. The
    * package matcher is used to resolve a transform instance to
    * convert object types to an from strings. If a match cannot
    * be found with this matcher then an exception is thrown.
    */
   public PackageMatcher() {
      super();
   }
   
   /**
    * This method attempts to perform a resolution of the transform
    * based on its package prefix. This allows this matcher to create
    * a logical group of transforms within a single method based on
    * the types package prefix. If no transform can be found then
    * this will throw an exception.
    * 
    * @param type this is the type to resolve a transform for
    * 
    * @return the transform that is used to transform that type
    */
   public Transform match(Class type) throws Exception {  
      String name = type.getName();
      
      if(name.startsWith("java.lang")) {
         return matchLanguage(type);
      }
      if(name.startsWith("java.util")) {
         return matchUtility(type);         
      }
      if(name.startsWith("java.net")) {
         return matchURL(type);         
      }
      if(name.startsWith("java.io")) {
         return matchFile(type);         
      }
      if(name.startsWith("java.sql")) {
         return matchSQL(type);   
      }     
      if(name.startsWith("java.math")) {
         return matchMath(type);
      }
      return matchEnum(type);
   }
   
   /**
    * This is used to resolve <code>Transform</code> implementations
    * that are <code>Enum</code> implementations. If the type is not
    * an enumeration then this will return null.
    * 
    * @param type this is the type to resolve a stock transform for
    * 
    * @return this will return a transform for the specified type
    */
   private Transform matchEnum(Class type) {
      Class parent = type.getSuperclass();
      
      if(parent != null) {
         if(parent.isEnum()) {
            return new EnumTransform(type);
         }
         if(type.isEnum()) {
            return new EnumTransform(type);
         }
      }
      return null;
   }
   
   /**
    * This is used to resolve <code>Transform</code> implementations
    * that relate to the <code>java.lang</code> package. If the type
    * does not resolve to a valid transform then this method will 
    * throw an exception to indicate that no stock transform exists
    * for the specified type.
    * 
    * @param type this is the type to resolve a stock transform for
    * 
    * @return this will return a transform for the specified type
    */
   private Transform matchLanguage(Class type) throws Exception {      
      if(type == Boolean.class) {
         return new BooleanTransform();
      }
      if(type == Integer.class) {
         return new IntegerTransform();
      }
      if(type == Long.class) {
         return new LongTransform();
      }
      if(type == Double.class) {
         return new DoubleTransform();
      }
      if(type == Float.class) {
         return new FloatTransform();
      }
      if(type == Short.class) {
         return new ShortTransform();
      }
      if(type == Byte.class) {
         return new ByteTransform();
      }
      if(type == Character.class) {
         return new CharacterTransform();
      }
      if(type == String.class) {
         return new StringTransform();
      }      
      if(type == Class.class) {
         return new ClassTransform();
      }
      return null;
   }
   
   /**
    * This is used to resolve <code>Transform</code> implementations
    * that relate to the <code>java.math</code> package. If the type
    * does not resolve to a valid transform then this method will 
    * throw an exception to indicate that no stock transform exists
    * for the specified type.
    * 
    * @param type this is the type to resolve a stock transform for
    * 
    * @return this will return a transform for the specified type
    */ 
   private Transform matchMath(Class type) throws Exception {
      if(type == BigDecimal.class) {
         return new BigDecimalTransform();
      }
      if(type == BigInteger.class) {
         return new BigIntegerTransform();
      }      
      return null;
   }
   
   /**
    * This is used to resolve <code>Transform</code> implementations
    * that relate to the <code>java.util</code> package. If the type
    * does not resolve to a valid transform then this method will 
    * throw an exception to indicate that no stock transform exists
    * for the specified type.
    * 
    * @param type this is the type to resolve a stock transform for
    * 
    * @return this will return a transform for the specified type
    */
   private Transform matchUtility(Class type) throws Exception {
      if(type == Date.class) {
         return new DateTransform(type);
      }
      if(type == Locale.class) {
         return new LocaleTransform();
      }
      if(type == Currency.class) {
         return new CurrencyTransform();
      }
      if(type == GregorianCalendar.class) {
         return new GregorianCalendarTransform();
      }
      if(type == TimeZone.class) {
         return new TimeZoneTransform();
      }   
      if(type == AtomicInteger.class) {
         return new AtomicIntegerTransform();
      }
      if(type == AtomicLong.class) {
         return new AtomicLongTransform();
      } 
      return null;
   }  
   
   /**
    * This is used to resolve <code>Transform</code> implementations
    * that relate to the <code>java.sql</code> package. If the type
    * does not resolve to a valid transform then this method will 
    * throw an exception to indicate that no stock transform exists
    * for the specified type.
    * 
    * @param type this is the type to resolve a stock transform for
    * 
    * @return this will return a transform for the specified type
    */
   private Transform matchSQL(Class type) throws Exception {
      if(type == Time.class) {
         return new DateTransform(type);
      }
      if(type == java.sql.Date.class) {
         return new DateTransform(type);
      }
      if(type == Timestamp.class) {
         return new DateTransform(type);
      }      
      return null;
   }   
   
   /**
    * This is used to resolve <code>Transform</code> implementations
    * that relate to the <code>java.io</code> package. If the type
    * does not resolve to a valid transform then this method will 
    * throw an exception to indicate that no stock transform exists
    * for the specified type.
    * 
    * @param type this is the type to resolve a stock transform for
    * 
    * @return this will return a transform for the specified type
    */
   private Transform matchFile(Class type) throws Exception {
      if(type == File.class) {
         return new FileTransform();
      }       
      return null;
   }   
   
   /**
    * This is used to resolve <code>Transform</code> implementations
    * that relate to the <code>java.net</code> package. If the type
    * does not resolve to a valid transform then this method will 
    * throw an exception to indicate that no stock transform exists
    * for the specified type.
    * 
    * @param type this is the type to resolve a stock transform for
    * 
    * @return this will return a transform for the specified type
    */
   private Transform matchURL(Class type) throws Exception {
      if(type == URL.class) {
         return new URLTransform();
      }      
      return null;
   }
}
