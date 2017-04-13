/*
 * MethodType.java May 2007
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

package org.simpleframework.xml.core;

/**
 * The <code>MethodType</code> enumeration is used to specify a 
 * set of types that can be used to classify Java Beans methods.
 * This creates three types for the get, is, and set methods. The
 * method types allow the <code>MethodScanner</code> to determine
 * what function the method has in creating a contact point for
 * the object. This also enables methods to be parsed correctly.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.MethodScanner
 * @see org.simpleframework.xml.core.MethodPart
 */
enum MethodType {
   
   /**
    * This is used to represent a method that acts as a getter.
    */
   GET(3),
   
   /**
    * This is used to represent a method that acts as a getter.
    */
   IS(2),
   
   /**
    * This is used to represent a method that acts as a setter.
    */
   SET(3),  
   
   /**
    * This is used to represent a a normal method to be ignored.
    */
   NONE(0);
   
   /**
    * This is the length of the prefix the method type uses.
    */
   private int prefix;
   
   /**
    * Constructor for the <code>MethodType</code> object. This is
    * used to create a method type specifying the length of the
    * prefix. This allows the method name to be parsed easily.
    * 
    * @param prefix this is the length of the method name prefix
    */
   private MethodType(int prefix) {
      this.prefix = prefix;
   }
   
   /**
    * This is used to acquire the prefix for the method type. The
    * prefix allows the method name to be extracted easily as it
    * is used to determine the character range that forms the name.
    * 
    * @return this returns the method name prefix for the type
    */
   public int getPrefix() {
      return prefix;
   }
}