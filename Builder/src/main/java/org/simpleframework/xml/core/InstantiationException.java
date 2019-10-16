/*
 * InstantiationException.java July 2006
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
 * The <code>InstantiationException</code> is thrown when an object
 * cannot be instantiated either because it is an abstract class or an
 * interface. Such a situation can arise if a serializable field is an 
 * abstract type and a suitable concrete class cannot be found. Also,
 * if an override type is not assignable to the field type this is
 * thrown, for example if an XML element list is not a collection.
 *  
 * @author Niall Gallagher
 */         
public class InstantiationException extends PersistenceException {

   /**
    * Constructor for the <code>InstantiationException</code> object. 
    * This constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string
    */
   public InstantiationException(String text, Object... list) {
      super(text, list);           
   }        
   
   /**
    * Constructor for the <code>InstantiationException</code> object. 
    * This constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param cause the source exception this is used to represent
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string 
    */
   public InstantiationException(Throwable cause, String text, Object... list) {
      super(cause, text, list);           
   }
}
