/*
 * OverrideValue.java January 2010
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

import org.simpleframework.xml.strategy.Value;

/**
 * The <code>OverrideValue</code> is used to represent a value that
 * contains an override type. Providing a value in this way ensures
 * that should an XML element not contain any data representing
 * the type of object then the type data can be provided.
 * 
 * @author Niall Gallagher
 */
class OverrideValue implements Value {
   
   /**
    * This is the value that is used internally for this value.
    */
   private final Value value;
   
   /**
    * This is the type that is used to represent the value.
    */
   private final Class type;
   
   /**
    * Constructor for the <code>OverrideValue</code> object. This
    * will delegate to an internal value instance but will provide
    * the declared type when requested. 
    * 
    * @param value this is the value that this will delegate to
    * @param type this is the override type for this value
    */
   public OverrideValue(Value value, Class type){
      this.value = value;
      this.type = type;
   }

   /**
    * This method is used to acquire an instance of the type that
    * is defined by this object. If the value has not been set
    * then this method will return null if this is not a reference.
    * 
    * @return an instance of the type this object represents
    */
   public Object getValue() {
      return value.getValue();
   }

   /**
    * This method is used set the value within this object. Once
    * this is set then the <code>getValue</code> method will return
    * the object that has been provided for consistency. 
    * 
    * @param instance this is the value to insert as the type
    */
   public void setValue(Object instance) {
      value.setValue(instance);
   }

   /**
    * This is the type of the object instance this represents. The
    * type returned by this is used to instantiate an object which
    * will be set on this value and the internal graph maintained.
    * 
    * @return the type of the object that must be instantiated
    */
   public Class getType() {
      return type;
   }

   /**
    * This returns the length of the array that is to be allocated.
    * If this value does not represent an array then this should
    * return zero to indicate that it is not an array object.
    * 
    * @return this returns the number of elements for the array
    */
   public int getLength() {
      return value.getLength();
   }

   /**
    * This will return true if the object represents a reference.
    * A reference will provide a valid instance when this objects 
    * getter is invoked. A valid instance can be a null.
    * 
    * @return this returns true if this represents a reference
    */
   public boolean isReference() {
      return value.isReference();
   }
}
