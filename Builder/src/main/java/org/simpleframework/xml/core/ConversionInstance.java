/*
 * CoversionInstance.java April 2007
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

import org.simpleframework.xml.strategy.Value;

/**
 * The <code>ConversionInstance</code> object is used to promote the
 * type to some more specialized type. For example if a field or 
 * method that represents a <code>List</code> is annotated then this
 * might create a specialized type such as a <code>Vector</code>. It
 * typically used to promote a type either because it is abstract
 * or because another type is required. 
 * <p>
 * This is used by the <code>CollectionFactory</code> to convert the
 * type of a collection field from an abstract type to a instantiable
 * type. This is used to simplify strategy implementations. 
 *
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.CollectionFactory
 */
class ConversionInstance implements Instance {
   
   /**
    * This is the context that is used to create the instance.
    */
   private final Context context;
   
   /**
    * This is the new class that is used for the type conversion. 
    */
   private final Class convert;
   
   /**
    * This is the value object that will be wrapped by this.
    */
   private final Value value;
   
   /**
    * This is used to specify the creation of a conversion type that
    * can be used for creating an instance with a class other than 
    * the default class specified by the <code>Value</code> object.
    * 
    * @param context this is the context used for instantiation
    * @param value this is the type used to create the instance
    * @param convert this is the class the type is converted to
    */
   public ConversionInstance(Context context, Value value, Class convert) throws Exception {
      this.context = context;
      this.convert = convert;      
      this.value = value;
   }
   
   /**
    * This method is used to acquire an instance of the type that
    * is defined by this object. If for some reason the type can
    * not be instantiated an exception is thrown from this.
    * 
    * @return an instance of the type this object represents
    */
   public Object getInstance() throws Exception {
      if(value.isReference()) {
         return value.getValue();
      }
      Object created = getInstance(convert);
      
      if(created != null) {
         setInstance(created);
      }
      return created;
   }

   /**
    * This method is used to acquire an instance of the type that
    * is defined by this object. If for some reason the type can
    * not be instantiated an exception is thrown from this.
    * 
    * @param type this is the type of the instance to create
    * 
    * @return an instance of the type this object represents
    */
   public Object getInstance(Class type) throws Exception {
      Instance value = context.getInstance(type);
      Object object = value.getInstance();
      
      return object;
   }
   
   /**
    * This method is used acquire the value from the type and if
    * possible replace the value for the type. If the value can
    * not be replaced then an exception should be thrown. This 
    * is used to allow primitives to be inserted into a graph.
    * 
    * @param object this is the object to insert as the value
    * 
    * @return an instance of the type this object represents
    */
   public Object setInstance(Object object) throws Exception {
      if(value != null) {
         value.setValue(object);
      }
      return object;
   }
   
   /**
    * This is the type of the object instance that will be created
    * by the <code>getInstance</code> method. This allows the 
    * deserialization process to perform checks against the field.
    * 
    * @return the type of the object that will be instantiated
    */
   public Class getType() {
      return convert;
   }
   
   /**
    * This will return true if the <code>Value</code> object provided
    * is a reference type. Typically a reference type refers to a 
    * type that is substituted during the deserialization process 
    * and so constitutes an object that does not need initialization.
    * 
    * @return this returns true if the type is a reference type
    */
   public boolean isReference() {
      return value.isReference();
   }   
}
