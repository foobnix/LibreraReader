/*
 * ParameterMap.java May 2012
 *
 * Copyright (C) 2012, Niall Gallagher <niallg@users.sf.net>
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

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The <code>ParameterMap</code> object represents of parameters
 * that are present within an objects constructors. This is used
 * for convenience to iterate over parameters and also to acquire
 * parameters by index, that is, the position they appear in the
 * constructor signature.
 * 
 * @author Niall Gallagher
 */
class ParameterMap extends LinkedHashMap<Object, Parameter> implements Iterable<Parameter>{

   /**
    * Constructor for the <code>ParameterMap</code> object. This 
    * is used to create a linked hash map of parameters where each
    * parameter can be acquired by its index within a constructor.
    */
   public ParameterMap() {
      super();
   }
   
   /**
    * This is used to iterate over <code>Parameter</code> objects.
    * Parameters are iterated in the order that they are added to
    * the map. This is primarily used for convenience iteration. 
    * 
    * @return this returns an iterator for the parameters
    */
   public Iterator<Parameter> iterator() {
      return values().iterator();
   }
   
   /**
    * This is used to acquire a <code>Parameter</code> using the
    * position of that parameter within the constructors. This 
    * allows a builder to determine which parameters to use.
    * 
    * @param ordinal this is the position of the parameter
    * 
    * @return this returns the parameter for the position
    */
   public Parameter get(int ordinal) {
      return getAll().get(ordinal);
   }
   
   /**
    * This is used to acquire an list of <code>Parameter</code>
    * objects in declaration order. This list will help with the
    * resolution of the correct constructor for deserialization
    * of the XML. It also provides a faster method of iteration.
    * 
    * @return this returns the parameters in declaration order
    */
   public List<Parameter> getAll() {
      Collection<Parameter> list = values();
      
      if(!list.isEmpty()) {
         return new ArrayList(list);
      }
      return emptyList();
   }
}
