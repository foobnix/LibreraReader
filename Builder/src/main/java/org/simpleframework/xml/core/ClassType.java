/*
 * ClassType.java January 2010
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
 * The <code>ClassType</code> object is used to represent a type that
 * is neither a field or method. Such a type is used when an object
 * is to be used to populate a collection. In such a scenario there
 * is no method or field annotations associated with the object.
 * 
 * @author Niall Gallagher
 */
class ClassType implements Type {
   
   /**
    * This is the type that is represented by this instance.
    */
   private final Class type;
   
   /**
    * Constructor for the <code>ClassType</code> object. This will
    * create a type used to represent a stand alone object, such
    * as an object being inserted in to a Java collection.
    * 
    * @param type this is the class that this type represents
    */
   public ClassType(Class type) {
      this.type = type;
   }

   /**
    * This is the class associated with this type. This is used by
    * the serialization framework to determine how the XML is to
    * be converted in to an object and vice versa.
    * 
    * @return this returns the class associated with this type
    */
   public Class getType() {
      return type;
   }
   
   /**
    * This is used to acquire an annotation of the specified type.
    * If no such annotation exists for the type then this will
    * return null. Currently for classes this will always be null.
    * 
    * @param type this is the annotation type be be acquired
    * 
    * @return currently this method will always return null
    */
   public <T extends Annotation> T getAnnotation(Class<T> type) {
      return null;
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