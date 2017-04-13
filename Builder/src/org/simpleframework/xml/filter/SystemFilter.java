/*
 * SystemFilter.java May 2006
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

package org.simpleframework.xml.filter;

/**
 * The <code>SystemFilter</code> object is used to provide a filter
 * that will replace the specified values with system properties.
 * This can be given a delegate filter which can be used to resolve
 * replacements should the value requested not match a property. 
 * 
 * @author Niall Gallagher
 */
public class SystemFilter implements Filter {
   
   /**
    * Filter delegated to if no system property can be resolved.
    */
   private Filter filter;        
        
   /**
    * Constructor for the <code>SystemFilter</code> object. This 
    * creates a filter that will resolve replacements using system
    * properties. Should the system properties not contain the
    * requested mapping this will return a null value.
    */
   public SystemFilter() {
      this(null);           
   }
        
   /**
    * Constructor for the <code>SystemFilter</code> object. This 
    * creates a filter that will resolve replacements using system
    * properties. Should the system properties not contain the
    * requested mapping this delegates to the specified filter.
    * 
    * @param filter the filter delegated to if resolution fails
    */   
   public SystemFilter(Filter filter) {
      this.filter = filter;           
   }
   
   /**
    * Replaces the text provided with the value resolved from the
    * system properties. If the system properties fails this will
    * delegate to the specified <code>Filter</code> if it is not
    * a null object. If no match is found a null is returned.
    * 
    * @param text this is the text value to be replaced
    * 
    * @return this will return the replacement text resolved
    */
   public String replace(String text) {
      String value = System.getProperty(text);           

      if(value != null) {
         return value;                       
      }
      if(filter != null) {
         return filter.replace(text);              
      }      
      return null;
   }   
}
