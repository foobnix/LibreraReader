/*
 * ElementException.java July 2006
 *
 * Copyright (C) 2006, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>ElementException</code> is used to represent conditions 
 * when an XML element is in an invalid state. Typically this is used
 * when and element cannot be serialized or deserialized. Such a
 * condition can arise when an element exists within the source XML 
 * that does not match the schema class. For serialization this 
 * exception is thrown if a required field is null.
 * 
 * @author Niall Gallagher
 */
public class ElementException extends PersistenceException {

   /**
    * Constructor for the <code>ElementException</code> object. This
    * constructor takes a format string an a variable number of object
    * arguments, which can be inserted into the format string. 
    * 
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string
    */
   public ElementException(String text, Object... list) {
      super(text, list);           
   }        

   /**
    * Constructor for the <code>ElementException</code> object. This
    * constructor takes a format string an a variable number of object
    * arguments, which can be inserted into the format string. 
    * 
    * @param cause the source exception this is used to represent
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string
    */
   public ElementException(Throwable cause, String text, Object... list) {
      super(cause, text, list);           
   }
}
