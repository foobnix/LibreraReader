/*
 * MethodPart.java April 2007
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

/**
 * The <code>MethodPart</code> interface is used to provide a point 
 * of contact with an object. Typically this will be used to get a
 * method from an object which is contains an XML annotation. This
 * provides the type the method is associated with, this type is
 * either the method return type or the single value parameter.
 * 
 * @author Niall Gallagher
 */ 
interface MethodPart {

   /**
    * This provides the name of the method part as acquired from the
    * method name. The name represents the Java Bean property name
    * of the method and is used to pair getter and setter methods.
    * 
    * @return this returns the Java Bean name of the method part
    */
   String getName(); 
   
   /**
    * This is the annotation associated with the point of contact.
    * This will be an XML annotation that describes how the contact
    * should be serializaed and deserialized from the object.
    *
    * @return this provides the annotation associated with this
    */
   Annotation getAnnotation();
   
   /**
    * This is the annotation associated with the point of contact.
    * This will be an XML annotation that describes how the contact
    * should be serialized and deserialized from the object.
    * 
    * @param type this is the type of the annotation to acquire
    *
    * @return this provides the annotation associated with this
    */
   <T extends Annotation> T getAnnotation(Class<T> type);
   
   /**
    * This will provide the contact type. The contact type is the
    * class that is either the method return type or the single
    * value parameter type associated with the method.
    *
    * @return this returns the type that this contact represents
    */ 
   Class getType();
   
   /**
    * This is used to acquire the dependent class for the method 
    * part. The dependent type is the type that represents the 
    * generic type of the type. This is used when collections are
    * annotated as it allows a default entry class to be taken
    * from the generic information provided.
    * 
    * @return this returns the generic dependent for the type
    */
   Class getDependent();
   
   /**
    * This is used to acquire the dependent classes for the method 
    * part. The dependent types are the types that represent the 
    * generic types of the type. This is used when collections are 
    * annotated as it allows a default entry class to be taken
    * from the generic information provided.
    * 
    * @return this returns the generic dependent for the type
    */
   Class[] getDependents();
   
   /**
    * This is the class that declares the contact. The declaring
    * class is where the method represented has been defined. This
    * will typically be a class rather than an interface.
    * 
    * @return this returns the class the part is declared within
    */
   Class getDeclaringClass();
   
   /**
    * This is the method for this point of contact. This is what
    * will be invoked by the serialization or deserialization 
    * process when an XML element or attribute is to be used.
    * 
    * @return this returns the method associated with this
    */
   Method getMethod();
   
   /**
    * This is the method type for the method part. This is used in
    * the scanning process to determine which type of method a
    * instance represents, this allows set and get methods to be
    * paired.
    * 
    * @return the method type that this part represents
    */
   MethodType getMethodType();
   
   /**
    * This is used to describe the method as it exists within the
    * owning class. This is used to provide error messages that can
    * be used to debug issues that occur when processing a method.
    * This should return the method as a generic representation.  
    * 
    * @return this returns a string representation of the method
    */
   String toString();
}