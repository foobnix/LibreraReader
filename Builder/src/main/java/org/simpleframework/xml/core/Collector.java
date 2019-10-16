/*
 * Collector.java December 2007
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

package org.simpleframework.xml.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * The <code>Collector</code> object is used to store variables for
 * a deserialized object. Each variable contains the label and value
 * for a field or method. The <code>Composite</code> object uses
 * this to store deserialized values before committing them to the
 * objects methods and fields. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Composite
 */
class Collector implements Criteria {
   
   /**
    * This is the registry containing all the variables collected.
    */
   private final Registry registry;
   
   /**
    * This is the registry that contains variables mapped to paths.
    */
   private final Registry alias;
   
   /**
    * Constructor for the <code>Collector</code> object. This is 
    * used to store variables for an objects fields and methods.
    * Each variable is stored using the name of the label.
    */
   public Collector() {
      this.registry = new Registry();
      this.alias = new Registry();
   }

   /**
    * This is used to get the <code>Variable</code> that represents
    * a deserialized object. The variable contains all the meta
    * data for the field or method and the value that is to be set
    * on the method or field.
    * 
    * @param key this is the key of the variable to be acquired
    * 
    * @return this returns the keyed variable if it exists
    */
   public Variable get(Object key) {
      return registry.get(key);
   } 
   
   /**
    * This is used to get the <code>Variable</code> that represents
    * a deserialized object. The variable contains all the meta
    * data for the field or method and the value that is to be set
    * on the method or field.
    * 
    * @param label this is the label to resolve the variable with
    * 
    * @return this returns the variable associated with the label
    */
   public Variable get(Label label) throws Exception {
      if(label != null) {
         Object key = label.getKey();
         
         return registry.get(key);
      }
      return null;
   } 
   
   /**
    * This is used to resolve the <code>Variable</code> by using 
    * the union names of a label. This will also acquire variables
    * based on the actual name of the variable.
    * 
    * @param path this is the path of the variable to be acquired
    * 
    * @return this returns the variable mapped to the path
    */
   public Variable resolve(String path) {
      return alias.get(path);
   }
   
   /**
    * This is used to remove the <code>Variable</code> from this
    * criteria object. When removed, the variable will no longer be
    * used to set the method or field when the <code>commit</code>
    * method is invoked.
    * 
    * @param key this is the key associated with the variable
    * 
    * @return this returns the keyed variable if it exists
    */
   public Variable remove(Object key) throws Exception{
      return registry.remove(key);
   }
   
   /**
    * This is used to acquire an iterator over the named variables.
    * Providing an <code>Iterator</code> allows the criteria to be
    * used in a for each loop. This is primarily for convenience.
    * 
    * @return this returns an iterator of all the variable names
    */
   public Iterator<Object> iterator() {
      return registry.iterator();
   }
   
   /**
    * This is used to create a <code>Variable</code> and set it for
    * this criteria. The variable can be retrieved at a later stage
    * using the name of the label. This allows for repeat reads as
    * the variable can be used to acquire the labels converter.
    * 
    * @param label this is the label used to create the variable
    * @param value this is the value of the object to be read
    */
   public void set(Label label, Object value) throws Exception {
      Variable variable = new Variable(label, value);

      if(label != null) {
         String[] paths = label.getPaths();
         Object key = label.getKey();
         
         for(String path : paths) {
            alias.put(path,  variable);
         }
         registry.put(key, variable);
      }
   }
   
   /**
    * This is used to set the values for the methods and fields of
    * the specified object. Invoking this performs the population
    * of an object being deserialized. It ensures that each value 
    * is set after the XML element has been fully read.
    * 
    * @param source this is the object that is to be populated
    */
   public void commit(Object source) throws Exception {
      Collection<Variable> set = registry.values();
      
      for(Variable entry : set) { 
         Contact contact = entry.getContact();
         Object value = entry.getValue();

         contact.set(source, value);
      }
   }  
   
   /**
    * The <code>Registry</code> object is used to store variables 
    * for the collector. All variables are stored under its name so
    * that they can be later retrieved and used to populate the
    * object when deserialization of all variables has finished.
    * 
    * @author Niall Gallagher
    */
   private static class Registry extends LinkedHashMap<Object, Variable> {
      
      /**
       * This is used to iterate over the names of the variables
       * in the registry. This is primarily used for convenience
       * so that the variables can be acquired in a for each loop.
       * 
       * @return an iterator containing the names of the variables
       */
      public Iterator<Object> iterator() {
         return keySet().iterator();
      }
   }
}