/*
 * OverrideType.java January 2010
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

import java.lang.annotation.Annotation;

import org.simpleframework.xml.strategy.Type;

/**
 * The <code>OverrideType</code> is used to represent a type as class
 * other than that defined in a containing type. This can be used to
 * ensure that a union type does not have to have an attribute
 * defining its type serialized in to the resulting XML.
 * 
 * @author Niall Gallagher
 */
class OverrideType implements Type {
   
   /**
    * This is the override that is used to represent the type.
    */
   private final Class override;
   
   /**
    * This is the type associated with this override type.
    */
   private final Type type;
   
   /**
    * Constructor for the <code>OverrideType</code> object. This is
    * used to create a type object that has an override type which
    * is can be used to ensure serialization does not require any
    * extra data containing the class name of the type instance.
    * 
    * @param type this is the type used internally for this
    * @param override this is the override type to use
    */
   public OverrideType(Type type, Class override) {
      this.override = override;
      this.type = type;
   }
   
   /**
    * This is the annotation associated with the method or field
    * that has been annotated. If this represents an entry to a 
    * Java collection such as a <code>java.util.List</code> then
    * this will return null for any annotation requested.
    * 
    * @param label this is the type of the annotation to acquire
    *
    * @return this provides the annotation associated with this
    */
   public <T extends Annotation> T getAnnotation(Class<T> label) {
      return type.getAnnotation(label);
   }

   /**
    * This will provide the method or field type. The type is the
    * class that is to be read and written on the object. Typically 
    * the type will be a serializable object or a primitive type.
    *
    * @return this returns the type for this method o field
    */
   public Class getType() {
      return override;
   }
   
   /**
    * This is used to describe the type as it exists within the
    * owning class. This is used to provide error messages that can
    * be used to debug issues that occur when processing.  
    * 
    * @return this returns a string representation of the type
    */
   public String toString() {
      return type.toString();
   }
}