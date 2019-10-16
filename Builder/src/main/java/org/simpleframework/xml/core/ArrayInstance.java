/*
 * ArrayInstance.java January 2007
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

import java.lang.reflect.Array;

import org.simpleframework.xml.strategy.Value;

/**
 * The <code>ArrayInstance</code> object is used for creating arrays
 * from a specified <code>Value</code> object. This allows primitive
 * and composite arrays to be acquired either by reference or by value 
 * from the given value object. This must be  given the length of the 
 * array so that it can be allocated correctly.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Instance
 */
class ArrayInstance implements Instance {
 
   /**
    * This is the value object that contains the criteria.
    */
   private final Value value;
   
   /**
    * This is the array component type for the created array.
    */
   private final Class type;
   
   /**
    * This is the length of the array to be instantiated.
    */
   private final int length;
   
   /**
    * Constructor for the <code>ArrayInstance</code> object. This
    * is used to create an object that can create an array of the
    * given length and specified component type.
    * 
    * @param value this is the value object describing the instance
    */
   public ArrayInstance(Value value) {
      this.length = value.getLength();
      this.type = value.getType();
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
      Object array = Array.newInstance(type, length);
      
      if(value != null) {
         value.setValue(array);
      }
      return array;
   }
   
   /**
    * This method is used acquire the value from the type and if
    * possible replace the value for the type. If the value can
    * not be replaced then an exception should be thrown. This 
    * is used to allow primitives to be inserted into a graph.
    * 
    * @param array this is the array to insert as the value
    * 
    * @return an instance of the type this object represents
    */
   public Object setInstance(Object array) {
      if(value != null) {
         value.setValue(array);
      }
      return array;
   }
   
   /**
    * This is the type of the object instance that will be created
    * by the <code>getInstance</code> method. This allows the 
    * deserialization process to perform checks against the field.
    * 
    * @return the type of the object that will be instantiated
    */
   public Class getType() {
      return type;
   }

   /**
    * This is used to determine if the type is a reference type.
    * A reference type is a type that does not require any XML
    * deserialization based on its annotations. Values that are
    * references could be substitutes objects of existing ones. 
    * 
    * @return this returns true if the object is a reference
    */
   public boolean isReference() {
      return value.isReference();
   }
}
