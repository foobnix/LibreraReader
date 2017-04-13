/*
 * SetPart.java April 2007
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>SetPart</code> object represents the setter method for
 * a Java Bean property. This composes the set part of the method
 * contact for an object. The set part contains the method that is
 * used to set the value on an object and the annotation that tells
 * the deserialization process how to deserialize the value.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.MethodContact
 */
class SetPart implements MethodPart {
   
   /**
    * This cache contains the annotations present on the method.
    */
   private final Cache<Annotation> cache; 
   
   /**
    * This is the list of annotations associated with the method.
    */
   private final Annotation[] list;
   
   /**
    * This is the annotation for the set method provided.
    */
   private final Annotation label;
   
   /**
    * This represents the method type for the set part method.
    */
   private final MethodType type;
   
   /**
    * This method is used to set the value during deserialization. 
    */
   private final Method method;
   
   /**
    * This represents the name of this set part instance.
    */
   private final String name;
   
   /**
    * Constructor for the <code>SetPart</code> object. This is
    * used to create a method part that will provide a means for 
    * the deserialization process to set a value to a object.
    * 
    * @param method the method that is used to set the value
    * @param label this describes how to deserialize the value
    * @param list this is the list of annotations on the method
    */
   public SetPart(MethodName method, Annotation label, Annotation[] list) {
      this.cache = new ConcurrentCache<Annotation>();
      this.method = method.getMethod();
      this.name = method.getName();
      this.type = method.getType();
      this.label = label;
      this.list = list;
   }
   
   /**
    * This provides the name of the method part as acquired from the
    * method name. The name represents the Java Bean property name
    * of the method and is used to pair getter and setter methods.
    * 
    * @return this returns the Java Bean name of the method part
    */
   public String getName() {
      return name;
   }
   
   /**
    * This is used to acquire the type for this method part. This
    * is used by the serializer to determine the schema class that
    * is used to match the XML elements to the object details.
    * 
    * @return this returns the schema class for this method
    */
   public Class getType() {
      return method.getParameterTypes()[0];
   }
   
   /**
    * This is used to acquire the dependent class for the method 
    * part. The dependent type is the type that represents the 
    * generic type of the type. This is used when collections are
    * annotated as it allows a default entry class to be taken
    * from the generic information provided.
    * 
    * @return this returns the generic dependent for the type
    */  
   public Class getDependent() {
      return Reflector.getParameterDependent(method, 0);
   }
   
   /**
    * This is used to acquire the dependent classes for the method 
    * part. The dependent types are the types that represents the 
    * generic types of the type. This is used when collections are
    * annotated as it allows a default entry classes to be taken
    * from the generic information provided.
    * 
    * @return this returns the generic dependents for the type
    */  
   public Class[] getDependents() {
      return Reflector.getParameterDependents(method, 0);
   }
   
   /**
    * This is the class that declares the contact. The declaring
    * class is where the method represented has been defined. This
    * will typically be a class rather than an interface.
    * 
    * @return this returns the class the part is declared within
    */
   public Class getDeclaringClass() {
      return method.getDeclaringClass();
   }
   
   /**
    * This is used to acquire the annotation that was used to label
    * the method this represents. This acts as a means to match the
    * set method with the get method using an annotation comparison.
    * 
    * @return this returns the annotation used to mark the method
    */
   public Annotation getAnnotation() {
      return label;
   }
   
   /**
    * This is the annotation associated with the point of contact.
    * This will be an XML annotation that describes how the contact
    * should be serialized and deserialized from the object.
    * 
    * @param type this is the type of the annotation to acquire
    *
    * @return this provides the annotation associated with this
    */
   public <T extends Annotation> T getAnnotation(Class<T> type) {
      if(cache.isEmpty()) {
         for(Annotation entry : list) {
            Class key = entry.annotationType();
            cache.cache(key, entry);
         }
      }
      return (T)cache.fetch(type);
   }
  
   /**
    * This is the method type for the method part. This is used in
    * the scanning process to determine which type of method a
    * instance represents, this allows set and get methods to be
    * paired.
    * 
    * @return the method type that this part represents
    */
   public MethodType getMethodType() {
      return type;
   }
   
   /**
    * This is used to acquire the method that can be used to invoke
    * the Java Bean method on the object. If the method represented
    * by this is inaccessible then this will set it as accessible.
    * 
    * @return returns the method used to interface with the object
    */
   public Method getMethod() {
      if(!method.isAccessible()) {
         method.setAccessible(true);              
      }           
      return method;
   }
   
   /**
    * This is used to describe the method as it exists within the
    * owning class. This is used to provide error messages that can
    * be used to debug issues that occur when processing a method.
    * This returns the method as a generic string representation.  
    * 
    * @return this returns a string representation of the method
    */
   public String toString() {
      return method.toGenericString();
   }
}
