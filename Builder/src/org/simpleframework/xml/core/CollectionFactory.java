/*
 * CollectionFactory.java July 2006
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Value;
import org.simpleframework.xml.stream.InputNode;

/**
 * The <code>CollectionFactory</code> is used to create collection
 * instances that are compatible with the field type. This performs
 * resolution of the collection class by firstly consulting the
 * specified <code>Strategy</code> implementation. If the strategy
 * cannot resolve the collection class then this will select a type
 * from the Java Collections framework, if a compatible one exists.
 * 
 * @author Niall Gallagher
 */ 
class CollectionFactory extends Factory {

   /**
    * Constructor for the <code>CollectionFactory</code> object. This
    * is given the field type as taken from the owning object. The
    * given type is used to determine the collection instance created.
    * 
    * @param context this is the context associated with this factory
    * @param type this is the class for the owning object
    */
   public CollectionFactory(Context context, Type type) {
      super(context, type);           
   }
   
   /**
    * Creates a collection that is determined from the field type. 
    * This is used for the <code>ElementList</code> to get a
    * collection that does not have any overrides. This must be
    * done as the inline list does not contain an outer element.
    * 
    * @return a type which is used to instantiate the collection     
    */
   @Override
   public Object getInstance() throws Exception {
      Class expect = getType();
      Class real = expect;

      if(!isInstantiable(real)) {
         real = getConversion(expect);   
      }
      if(!isCollection(real)) {
         throw new InstantiationException("Invalid collection %s for %s", expect, type);
      }
      return real.newInstance();
   }
   
   /**
    * Creates the collection to use. The <code>Strategy</code> object
    * is consulted for the collection class, if one is not resolved
    * by the strategy implementation or if the collection resolved is
    * abstract then the Java Collections framework is consulted.
    * 
    * @param node this is the input node representing the list
    * 
    * @return this is the collection instantiated for the field
    */         
   public Instance getInstance(InputNode node) throws Exception {
      Value value = getOverride(node);
      Class expect = getType();
     
      if(value != null) {              
         return getInstance(value);
      }
      if(!isInstantiable(expect)) {
         expect = getConversion(expect);
      }
      if(!isCollection(expect)) {
         throw new InstantiationException("Invalid collection %s for %s", expect, type);
      }
      return context.getInstance(expect);         
   }     

   /**
    * This creates a <code>Collection</code> instance from the type
    * provided. If the type provided is abstract or an interface then
    * this can promote the type to a collection type that can be 
    * instantiated. This is done by asking the type to convert itself.
    * 
    * @param value the type used to instantiate the collection
    * 
    * @return this returns a compatible collection instance 
    */
   public Instance getInstance(Value value) throws Exception {
      Class expect = value.getType();

      if(!isInstantiable(expect)) {
         expect = getConversion(expect);
      }
      if(!isCollection(expect)) {
         throw new InstantiationException("Invalid collection %s for %s", expect, type);              
      }
      return new ConversionInstance(context, value, expect);         
   }  

   /**
    * This is used to convert the provided type to a collection type
    * from the Java Collections framework. This will check to see if
    * the type is a <code>List</code> or <code>Set</code> and return
    * an <code>ArrayList</code> or <code>HashSet</code> type. If no
    * suitable match can be found this throws an exception.
    * 
    * @param require this is the type that is to be converted
    * 
    * @return a collection that is assignable to the provided type
    */   
   public Class getConversion(Class require) throws Exception {
      if(require.isAssignableFrom(ArrayList.class)) {
         return ArrayList.class;
      }
      if(require.isAssignableFrom(HashSet.class)) {
         return HashSet.class;                 
      }
      if(require.isAssignableFrom(TreeSet.class)) {
         return TreeSet.class;              
      }       
      throw new InstantiationException("Cannot instantiate %s for %s", require, type);
   }

   /**
    * This determines whether the type provided is a collection type.
    * If the type is assignable to a <code>Collection</code> then 
    * this returns true, otherwise this returns false.
    * 
    * @param type given to determine whether it is a collection  
    * 
    * @return true if the provided type is a collection type
    */
   private boolean isCollection(Class type) {
      return Collection.class.isAssignableFrom(type);           
   }
}
