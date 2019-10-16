/*
 * Value.java January 2007
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

package org.simpleframework.xml.strategy;

/**
 * The <code>Value</code> object describes a type that is represented 
 * by an XML element. This enables a <code>Strategy</code> to define
 * not only the type an element represents, but also defines if that
 * type needs to be created. This allows arrays as well as standard
 * object types to be described. When instantiated the instance should
 * be set on the value object for use by the strategy to detect cycles.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.strategy.Strategy
 */
public interface Value {

   /**
    * This method is used to acquire an instance of the type that
    * is defined by this object. If the value has not been set
    * then this method will return null if this is not a reference.
    * 
    * @return an instance of the type this object represents
    */
   Object getValue();
   
   /**
    * This method is used set the value within this object. Once
    * this is set then the <code>getValue</code> method will return
    * the object that has been provided for consistency. 
    * 
    * @param value this is the value to insert as the type
    */
   void setValue(Object value);
   
   /**
    * This is the type of the object instance this represents. The
    * type returned by this is used to instantiate an object which
    * will be set on this value and the internal graph maintained.
    * 
    * @return the type of the object that must be instantiated
    */
   Class getType();
   
   /**
    * This returns the length of the array that is to be allocated.
    * If this value does not represent an array then this should
    * return zero to indicate that it is not an array object.
    * 
    * @return this returns the number of elements for the array
    */
   int getLength();
   
   /**
    * This will return true if the object represents a reference.
    * A reference will provide a valid instance when this objects 
    * getter is invoked. A valid instance can be a null.
    * 
    * @return this returns true if this represents a reference
    */
   boolean isReference();
}
