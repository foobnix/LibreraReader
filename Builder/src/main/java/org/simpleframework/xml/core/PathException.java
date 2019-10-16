/*
 * PathException.java July 2010
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
 * The <code>PathException</code> is thrown when there is a problem
 * with the syntax of an XPath expression. Parsing problems include
 * specifying a path that is either illegal or contains features 
 * that are not supported by the internal parser.
 * 
 * @author Niall Gallagher
 */
public class PathException extends PersistenceException {

   /**
    * Constructor for the <code>PathException</code> object. This
    * constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string
    */
   public PathException(String text, Object... list) {
      super(text, list);
   }
}
