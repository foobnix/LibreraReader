/*
 * FieldContact.java April 2007
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>FieldContact</code> object is used to act as a contact
 * for a field within an object. This allows a value to be set on an
 * object field during deserialization and acquired from the same
 * field during serialization.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.core.FieldScanner
 */ 
class FieldContact implements Contact {
   
   /**
    * This cache contains the annotations present on the field.
    */
   private final Cache<Annotation> cache; 
   
   /**
    * This is the list of annotations associated with the field.
    */
   private final Annotation[] list;
   
   /**
    * This is the label that marks the field within the object.
    */           
   private final Annotation label;
   
   /**
    * This represents the field within the schema class object.
    */ 
   private final Field field;
   
   /**
    * This is the name for this contact as taken from the field.
    */
   private final String name;
   
   /**
    * This is the modifiers for the field that this represents.
    */
   private final int modifier;
   
   /**
    * Constructor for the <code>FieldContact</code> object. This is 
    * used as a point of contact for a field within a schema class.
    * Values can be read and written directly to the field with this.
    *
    * @param field this is the field that is the point of contact
    * @param label this is the annotation that is used by the field
    * @param list this is the list of annotations on the field
    */ 
   public FieldContact(Field field, Annotation label, Annotation[] list) {
      this.cache = new ConcurrentCache<Annotation>();
      this.modifier = field.getModifiers();
      this.name = field.getName();
      this.label = label;
      this.field = field;
      this.list = list;
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
      return !isStatic() && isFinal();
   }
   
   /**
    * This is used to determine if the annotated contact is for a
    * static field or method. A static field or method is one that
    * contains the "static" keyword. Any static final fields will
    * be read only and does not require any matching annotation.
    * 
    * @return this returns true if the contact is a static one
    */
   public boolean isStatic() {
      return Modifier.isStatic(modifier);
   }
   
   /**
    * This is used to identify annotated methods are fields that
    * can not be modified. Such field will require that there is 
    * a constructor that can have the value injected in to it.
    * 
    * @return this returns true if the field or method is final
    */
   public boolean isFinal() {
      return Modifier.isFinal(modifier); 
   }

   /**
    * This will provide the contact type. The contact type is the
    * class that is to be set and get on the object. This represents
    * the return type for the get and the parameter for the set.
    *
    * @return this returns the type that this contact represents
    */
   public Class getType() {
      return field.getType();
   }
   
   /**
    * This provides the dependent class for the contact. This will
    * actually represent a generic type for the actual type. For
    * contacts that use a <code>Collection</code> type this will
    * be the generic type parameter for that collection.
    * 
    * @return this returns the dependent type for the contact
    */
   public Class getDependent() {
      return Reflector.getDependent(field);
   }
   
   /**
    * This provides the dependent classes for the contact. This will
    * typically represent a generic types for the actual type. For
    * contacts that use a <code>Map</code> type this will be the 
    * generic type parameter for that map type declaration.
    * 
    * @return this returns the dependent type for the contact
    */
   public Class[] getDependents() {
      return Reflector.getDependents(field);
   }
   
   /**
    * This is the class that declares the contact. The declaring
    * class is where the field represented been defined. This will
    * typically be a class rather than an interface.
    * 
    * @return this returns the class the contact is declared within
    */
   public Class getDeclaringClass() {
      return field.getDeclaringClass();
   }
   
   /**
    * This is used to acquire the name of the field. This will return
    * the name of the field which can then be used to determine the 
    * XML attribute or element the contact represents. This ensures
    * that the name provided string is internalized for performance.  
    * 
    *  @return this returns the name of the field represented
    */
   public String getName() {
      return name;
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
    * This is the annotation associated with the point of contact.
    * This will be an XML annotation that describes how the contact
    * should be serialized and deserialized from the object.
    * 
    * @param type this is the type of the annotation to acquire
    *
    * @return this provides the annotation associated with this
    */
   public <T extends Annotation> T getAnnotation(Class<T> type) {
      if(type == label.annotationType()) {
         return (T) label;
      }
      return getCache(type);
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
   private <T extends Annotation> T getCache(Class<T> type) {
      if(cache.isEmpty()) {
         for(Annotation entry : list) {
            Class key = entry.annotationType();
            cache.cache(key, entry);
         }
      }
      return (T)cache.fetch(type);
   }

   /**
    * This is used to set the specified value on the provided object.
    * The value provided must be an instance of the contact class so
    * that it can be set without a runtime class compatibility error.
    *
    * @param source this is the object to set the value on
    * @param value this is the value that is to be set on the object
    */ 
   public void set(Object source, Object value) throws Exception {
      if(!isFinal()) {
         field.set(source, value);
      }
   }

   /**
    * This is used to get the specified value on the provided object.
    * The value returned from this method will be an instance of the
    * contact class type. If the returned object is of a different
    * type then the serialization process will fail.
    *
    * @param source this is the object to acquire the value from
    *
    * @return this is the value that is acquired from the object
    */
   public Object get(Object source) throws Exception {
      return field.get(source);
   }
   
   /**
    * This is used to describe the contact as it exists within the
    * owning class. It is used to provide error messages that can
    * be used to debug issues that occur when processing a contact.
    * The string provided is the generic field string.
    * 
    * @return this returns a string representation of the contact
    */
   public String toString() {
      return String.format("field '%s' %s", getName(), field.toString());
   }
}
