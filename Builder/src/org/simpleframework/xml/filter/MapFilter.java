/*
 * MapFilter.java May 2006
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

import java.util.Map;

/**
 * The <code>MapFilter</code> object is a filter that can make use
 * of user specified mappings for replacement. This filter can be
 * given a <code>Map</code> of name value pairs which will be used
 * to resolve a value using the specified mappings. If there is
 * no match found the filter will delegate to the provided filter. 
 * 
 * @author Niall Gallagher
 */
public class MapFilter implements Filter {
   
   /**
    * This will resolve the replacement if no mapping is found.
    */
   private Filter filter;        

   /**
    * This contains a collection of user specified mappings.
    */
   private Map map;
        
   /**
    * Constructor for the <code>MapFilter</code> object. This will
    * use the specified mappings to resolve replacements. If this
    * map does not contain a requested mapping null is resolved.
    * 
    * @param map this contains the user specified mappings
    */
   public MapFilter(Map map) {
      this(map, null);           
   }
     
   /**
    * Constructor for the <code>MapFilter</code> object. This will
    * use the specified mappings to resolve replacements. If this
    * map does not contain a requested mapping the provided filter
    * is used to resolve the replacement text.
    * 
    * @param map this contains the user specified mappings
    * @param filter this is delegated to if the map fails
    */   
   public MapFilter(Map map, Filter filter) {
      this.filter = filter;  
      this.map = map;      
   }

   /**
    * Replaces the text provided with the value resolved from the
    * specified <code>Map</code>. If the map fails this will
    * delegate to the specified <code>Filter</code> if it is not
    * a null object. If no match is found a null is returned.
    * 
    * @param text this is the text value to be replaced
    * 
    * @return this will return the replacement text resolved
    */
   public String replace(String text) {
      Object value = null;
      
      if(map != null) {
         value = map.get(text);
      }
      if(value != null) {
         return value.toString();                       
      }
      if(filter != null) {
         return filter.replace(text);              
      }      
      return null;
   }   
}
