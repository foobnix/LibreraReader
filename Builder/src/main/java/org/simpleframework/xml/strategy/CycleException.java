/*
 * CycleException.java April 2007
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

package org.simpleframework.xml.strategy;

import org.simpleframework.xml.core.PersistenceException;

/**
 * The <code>CycleException</code> is thrown when an invalid cycle 
 * is found when deserializing an object from an XML document. This
 * usually indicates the either the XML has been edited incorrectly
 * or has been corrupted. Conditions that this exception is thrown
 * are when there is an invalid reference or a duplicate identifier. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.strategy.CycleStrategy
 */
public class CycleException extends PersistenceException {
   
   /**
    * Constructor for the <code>CycleException</code> object. This  
    * constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string
    */
   public CycleException(String text, Object... list) {
      super(text, list);           
   }        
   
   /**
    * Constructor for the <code>CycleException</code> object. This 
    * constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param cause the source exception this is used to represent
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string 
    */
   public CycleException(Throwable cause, String text, Object... list) {
      super(cause, text, list);           
   }
}
