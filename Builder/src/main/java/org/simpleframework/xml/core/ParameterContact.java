/*
 * ParameterContact.java April 2007
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
import java.lang.reflect.Constructor;

/**
 * The <code>ParameterContact</code> object is used to represent
 * a contact that is provided so that a <code>Label</code> can be
 * used to determine a consistent name for the parameter. Unlike
 * field and method contacts this is essentially an adapter that
 * is used so that the parameter name can be determined in a 
 * similar way to a method or field.
 * 
 * @author Niall Gallagher
 */
abstract class ParameterContact<T extends Annotation> implements Contact {

   /**
    * This is used to hold the annotations for this parameter.
    */
   protected final Annotation[] labels;

   /**
    * This is the constructor the parameter was declared within. 
    */
   protected final Constructor factory;
   
   /**
    * This represents the class that this parameter was declared in.
    */
   protected final Class owner;
   
   /**
    * This is the index of the parameter within the constructor.
    */
   protected final int index;
   
   /**
    * This is the annotation used to label the parameter.
    */
   protected final T label;

   /**
    * Constructor for the <code>ParameterContact</code> object. This
    * is used to create a contact that can be used to determine a
    * consistent name for the parameter. It requires the annotation,
    * the constructor, and the parameter declaration index.
    * 
    * @param label this is the annotation used for the parameter
    * @param factory this is the constructor that is represented
    * @param index this is the index for the parameter
    */
   public ParameterContact(T label, Constructor factory, int index) {
      this.labels = factory.getParameterAnnotations()[index];
      this.owner = factory.getDeclaringClass();
      this.factory = factory;
      this.index = index;
      this.label = label;
   }
   
   /**
    * This is the annotation associated with the point of contact.
    * This will be an XML annotation that describes how the contact
    * should be serialized and deserialized from the object.
    *
    * @return this provides the annotation associated with this
    */
   public Annotation getAnnotation() {
      return label;
   }
   
   /**
    * This will provide the contact type. The contact type is the
    * class that is to be set and get on the object. Typically the
    * type will be a serializable object or a primitive type.
    *
    * @return this returns the type that this contact represents
    */ 
   public Class getType() {
      return factory.getParameterTypes()[index];
   }

   /**
    * This provides the dependent class for the contact. This will
    * typically represent a generic type for the actual type. For
    * contacts that use a <code>Collection</code> type this will
    * be the generic type parameter for that collection.
    * 
    * @return this returns the dependent type for the contact
    */
   public Class getDependent() {
      return Reflector.getParameterDependent(factory, index);
   }

   /**
    * This provides the dependent classes for the contact. This will
    * typically represent a generic types for the actual type. For
    * contacts that use a <code>Map</code> type this will be the 
    * generic type parameter for that map type declaration.
    * 
    * @return this returns the dependent types for the contact
    */
   public Class[] getDependents() {
      return Reflector.getParameterDependents(factory, index);
   }
   
   /**
    * This is the class that declares the contact. The declaring
    * class is where the parameter has been defined. Typically
    * this will not be a class rather than an interface.
    * 
    * @return the class this parameter is declared within
    */
   public Class getDeclaringClass() {
      return owner;
   }
   
   /**
    * This is used to get the value from the specified object using
    * the point of contact. Typically the value is retrieved from
    * the specified object by invoking a get method of by acquiring
    * the value from a field within the specified object.
    *
    * @param source this is the object to acquire the value from
    *
    * @return this is the value acquired from the point of contact
    */ 
   public Object get(Object source) {
      return null;
   }  

   /**
    * This is used to set the value on the specified object through
    * this contact. Depending on the type of contact this will set
    * the value given, typically this will be done by invoking a
    * method or setting the value on the object field.
    *
    * @param source this is the object to set the value on
    * @param value this is the value to be set through the contact
    */ 
   public void set(Object source, Object value) {  
      return;
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
   public <A extends Annotation> A getAnnotation(Class<A> type) {
      for(Annotation label : labels) {
         Class expect = label.annotationType();
         
         if(expect.equals(type)) {
            return (A)label;
         }
      }
      return null;
   }
   
   /**
    * This is used to determine if the annotated contact is for a
    * read only variable. A read only variable is a field that
    * can be set from within the constructor such as a blank final
    * variable. It can also be a method with no set counterpart.
    * 
    * @return this returns true if the contact is a constant one
    */
   public boolean isReadOnly() {
      return false;
   }
   
   /**
    * This is used to provide a textual representation of the 
    * parameter. Providing a string describing the parameter is
    * useful for debugging and for exception messages.
    * 
    * @return this returns the string representation for this
    */
   public String toString() {
      return String.format("parameter %s of constructor %s", index, factory);
   }

   /**
    * This represents the name of the parameter. Because the name
    * of the parameter does not exist at runtime the name must
    * be taken directly from the annotation and the parameter type.
    * Each XML annotation must provide their own unique way of
    * providing a name for the parameter contact.
    * 
    * @return this returns the name of the contact represented
    */
   public abstract String getName();
}
