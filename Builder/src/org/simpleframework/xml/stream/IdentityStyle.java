/*
 * IdentityStyle.java July 2008
 *
 * Copyright (C) 2008, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.xml.stream;

/**
 * The <code>IdentityStyle</code> object is used to represent a style
 * that does not modify the tokens passed in to it. This is used if
 * there is no style specified or if there is no need to convert the
 * XML elements an attributes to a particular style. This is also 
 * the most performant style as it does not require cache lookups.
 * 
 * @author Niall Gallagher
 */
class IdentityStyle implements Style {

   /**
    * This is basically a pass through method. It will return the
    * same string as is passed in to it. This ensures the best
    * performant when no styling is required for the XML document.
    * 
    * @param name this is the token that is to be styled by this
    * 
    * @return this returns the same string that is passed in
    */
   public String getAttribute(String name) {
      return name;
   }

   /**
    * This is basically a pass through method. It will return the
    * same string as is passed in to it. This ensures the best
    * performant when no styling is required for the XML document.
    * 
    * @param name this is the token that is to be styled by this
    * 
    * @return this returns the same string that is passed in
    */
   public String getElement(String name) {
      return name;
   }
}
