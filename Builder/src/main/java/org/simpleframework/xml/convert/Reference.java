/*
 * Reference.java January 2010
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

package org.simpleframework.xml.convert;

import org.simpleframework.xml.strategy.Value;

/**
 * The <code>Reference</code> object represents a value that holds
 * an object instance. If an object instance is to be provided from
 * a <code>Strategy</code> implementation it must be wrapped in a 
 * value object. The value object can then provide the details of
 * the instance and the actual object instance to the serializer.
 * 
 * @author Niall Gallagher
 */
class Reference implements Value {
   
   /**
    * This represents the original value returned from a strategy.
    */
   private Value value;
   
   /**
    * This represents the object instance that this represents.
    */
   private Object data;
   
   /**
    * This is the actual type of the reference that is represented.
    */
   private Class actual;
   
   /**
    * Constructor for a <code>Reference</code> object. To create
    * this a value and an object instance is required. The value
    * provided may be null, but the instance should be a valid
    * object instance to be used by the serializer.
    * 
    * @param value this is the original value from a strategy
    * @param data this is the object instance that is wrapped
    * @param actual this is the overriding type of the reference
    */
   public Reference(Value value, Object data, Class actual){
      this.actual = actual;
      this.value = value;
      this.data = data;
   }
   
   /**
    * This will return the length of an array reference. Because
    * the value will represent the value itself the length is 
    * never used, as no instance needs to be created.
    * 
    * @return this will always return zero for a reference
    */
   public int getLength() {
      return 0;
   }
   
   /**
    * This is the type of the object instance this represents. The
    * type returned by this is used to instantiate an object which
    * will be set on this value and the internal graph maintained.
    * 
    * @return the type of the object that must be instantiated
    */
   public Class getType() {
      if(data != null) {
         return data.getClass();
      }
      return actual;
   }
   
   /**
    * This returns the actual object instance that is held by this
    * reference object.
    */
   public Object getValue() {
      return data;
   }
   
   /**
    * This will always return true as this <code>Value</code> object
    * will always contain an object instance. Returning true from 
    * this method tells the serializer that there is no need to
    * actually perform any further deserialization.
    * 
    * @return this always returns true as this will be a reference
    */
   public boolean isReference() {
      return true;
   }
   
   /**
    * This is used to set the value of the object. If the internal
    * <code>Value</code> is not null then the internal value will 
    * have the instance set also. 
    * 
    * @param data this is the object instance that is to be set
    */
   public void setValue(Object data) {
      if(value != null) {
         value.setValue(data);
      }
      this.data = data;
   }
}