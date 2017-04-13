/*
 * TransformException.java May 2007
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

import org.simpleframework.xml.core.PersistenceException;

/**
 * The <code>TransformException</code> is thrown if a problem occurs
 * during the transformation of an object. This can be thrown either
 * because a transform could not be found for a specific type or
 * because the format of the text value had an invalid structure.
 * 
 * @author Niall Gallagher
 */
public class TransformException extends PersistenceException {
   
   /**
    * Constructor for the <code>TransformException</code> object. 
    * This constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the string
    */     
   public TransformException(String text, Object... list) {
      super(String.format(text, list));               
   }       

   /**
    * Constructor for the <code>TransformException</code> object. 
    * This constructor takes a format string an a variable number of 
    * object arguments, which can be inserted into the format string. 
    * 
    * @param cause the source exception this is used to represent
    * @param text a format string used to present the error message
    * @param list a list of arguments to insert into the stri 
    */
   public TransformException(Throwable cause, String text, Object... list) {
      super(String.format(text, list), cause);           
   }  
}
