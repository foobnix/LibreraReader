/*
 * ModelMap.java November 2010
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

package org.simpleframework.xml.core;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * The <code>ModelMap</code> object represents a map that contains 
 * string model mappings. This is used for convenience as a typedef
 * like construct to avoid having declare the generic type whenever
 * it is referenced. Also this allows <code>ModelList</code> values 
 * from the map to be iterated within for each loops.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.core.Model
 * @see org.simpleframework.xml.core.ModelList
 */
class ModelMap extends LinkedHashMap<String, ModelList> implements Iterable<ModelList>{
   
   /**
    * This is the detail associated with this model map instance.
    */
   private final Detail detail;
   
   /**
    * Constructor for the <code>ModelMap</code> object is used to 
    * create an empty map. This is used for convenience as a typedef
    * like construct which avoids having to use the generic type.
    *
    * @param detail this is the detail associated with the map
    */ 
   public ModelMap(Detail detail) {
      this.detail = detail;
   }
   
   /**
    * This method is used to clone the model map such that mappings
    * can be maintained in the original even if they are modified
    * in the clone. This is used to that the <code>Schema</code> can
    * remove mappings from the model map as they are visited. 
    *
    * @return this returns a cloned representation of this map
    */
   public ModelMap getModels() throws Exception {
      ModelMap map = new ModelMap(detail);
      
      for(String name : keySet()) {
         ModelList list = get(name);
         
         if(list != null) {
            list = list.build();
         }
         if(map.containsKey(name)) {
            throw new PathException("Path with name '%s' is a duplicate in %s ", name, detail);
         }
         map.put(name, list);
      }         
      return map;    
   }
   
   /**
    * This method is used to look for a <code>Model</code> that
    * matches the specified element name. If no such model exists
    * then this will return null. This is as a convenient way
    * to find a model within the tree of models being built.
    * 
    * @param name this is the name of the model to be acquired
    * @param index this is the index used to order the model
    * 
    * @return this returns the model located by the expression
    */
   public Model lookup(String name, int index) {
      ModelList list = get(name);
      
      if(list != null) {
         return list.lookup(index);
      }
      return null;
   }
   
   /**
    * This is used to register a <code>Model</code> within this
    * map. Registration of a model creates a tree of models that
    * can be used to represent an XML structure. Each model can
    * contain elements and attributes associated with a type.
    * 
    * @param name this is the name of the model to be registered
    * @param model this is the model that is to be registered
    */
   public void register(String name, Model model) {
      ModelList list = get(name);
      
      if(list == null) {
         list = new ModelList();
         put(name, list);
      }
      list.register(model);      
   }
   
   /**
    * This allows the <code>ModelList</code> objects within the model 
    * map to be iterated within for each loops. This will provide all
    * remaining model objects within the map. The iteration order is
    * not maintained so model objects may be given in any sequence.
    *
    * @return this returns an iterator for existing model objects
    */ 
   public Iterator<ModelList> iterator() {
      return values().iterator(); 
   }
}