/*
 * ConvertException.java January 2010
 *
 * Copyright (C) 2010, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.xml.convert;

/**
 * The <code>ConvertException</code> is thrown when there is a
 * problem converting an object. Such an exception can occur if an
 * annotation is use incorrectly, or if a <code>Converter</code>
 * can not be instantiated. Messages provided to this exception are
 * formatted similar to the <code>PrintStream.printf</code> method.
 * 
 * @author Niall Gallagher
 */
public class ConvertException extends Exception {

   /**
    * Constructor for the <code>ConvertException</code> object. 
    * This constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string
    */
   public ConvertException(String text, Object... list) {
      super(String.format(text, list));               
   }   
}
