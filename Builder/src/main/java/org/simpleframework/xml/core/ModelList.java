/*
 * ModelList.java November 2010
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

import java.util.ArrayList;

/**
 * The <code>ModelList</code> object is used to maintain an ordered
 * list of models. Models are maintained within the list in an 
 * sequenced manner, ordered by the index of the model. During the
 * building process models can be registered in any order, however
 * once building has finished the list must contain a complete
 * sequence of models, ordered by index.
 * 
 * @author Niall Gallagher
 */
class ModelList extends ArrayList<Model> {
   
   /**
    * Constructor for the <code>ModelList</code> object. This is
    * used to construct a linked list that can take registrations
    * of <code>Model</code> objects out of sequence. Once complete
    * the list should contain a full ordered set of models.
    */
   public ModelList() {
      super();
   }
   
   /**
    * This is used when building a copy of the model. A copy is
    * required when serializing or deserializing so that the list
    * remains intact for the next time it is used  
    * 
    * @return this returns an exact copy of the model list
    */
   public ModelList build() {
      ModelList list = new ModelList();
      
      for(Model model : this) {
         list.register(model);
      }
      return list;
   }
   
   /**
    * This is used to determine if the model list is empty. The
    * model list is considered empty if it does not contain any
    * models with element or attribute registrations. This does
    * not mean that the list itself does not contain models.
    * 
    * @return this returns true if there are no registrations
    */
   public boolean isEmpty() {
      for(Model model : this) {
         if(model != null) {
            if(!model.isEmpty()) {
               return false;
            }
         }
      }
      return true;
   }
   
   /**
    * This is used to find a model based on its index. If there
    * are no models at the specified index this will return 
    * null. Unlike the get method this does not throw exceptions.
    * 
    * @param index this is the index to acquire the model at
    * 
    * @return this returns the model if one exists at the index
    */
   public Model lookup(int index) {
      int size = size();
      
      if(index <= size) {
         return get(index-1);
      }
      return null;
   }
   
   /**
    * This is used to register the model within the list. The model
    * is registered at the index provided. If the registration is
    * out of sequence all indexes that do not have models are
    * populated with null values to ensure each model resides in
    * its index position within the list.
    * 
    * @param model the model to be registered within the list
    */
   public void register(Model model) {
      int index = model.getIndex();
      int size = size(); 

      for(int i = 0; i < index; i++) {
         if(i >= size) {
            add(null); 
         }
         if(i == index -1) {           
            set(index-1, model);
         } 
      }      
   }
   
   /**
    * This is used to take the models from the model list at the
    * first index. This is used when iterating over the models
    * to ensure there is only ever one visit of a specific model
    * 
    * @return this returns the next model in the sequence
    */
   public Model take() {
      while(!isEmpty()) {
         Model model = remove(0);         
         
         if(!model.isEmpty()) {
            return model;
         }
      }
      return null;
   }
}