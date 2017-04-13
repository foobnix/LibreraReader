/*
 * MethodContact.java April 2007
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
 * The <code>MethodContact</code> object is acts as a contact that
 * can set and get data to and from an object using methods. This 
 * requires a get method and a set method that share the same class
 * type for the return and parameter respectively.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.core.MethodScanner
 */ 
class MethodContact implements Contact {
   
   /**
    * This is the label that marks both the set and get methods.
    */         
   private Annotation label;
   
   /**
    * This is the set method which is used to set the value.
    */ 
   private MethodPart set;
   
   /**
    * This is the get method which is used to get the value.
    */
   private MethodPart get;

   /**
    * This is the dependent types as taken from the get method.
    */
   private Class[] items;
   
   /**
    * This represents the declaring class for this method.
    */
   private Class owner;
   
   /**
    * This is the dependent type as taken from the get method.
    */
   private Class item;
   
   /**
    * This is the type associated with this point of contact.
    */ 
   private Class type; 
   
   /**
    * This represents the name of the method for this contact.
    */
   private String name;
   
   /**
    * Constructor for the <code>MethodContact</code> object. This is
    * used to compose a point of contact that makes use of a get and
    * set method on a class. The specified methods will be invoked
    * during the serialization process to get and set values.
    *
    * @param get this forms the get method for the object
    */ 
   public MethodContact(MethodPart get) {
      this(get, null);
   }
   
   /**
    * Constructor for the <code>MethodContact</code> object. This is
    * used to compose a point of contact that makes use of a get and
    * set method on a class. The specified methods will be invoked
    * during the serialization process to get and set values.
    *
    * @param get this forms the get method for the object
    * @param set this forms the get method for the object 
    */ 
   public MethodContact(MethodPart get, MethodPart set) {
      this.owner = get.getDeclaringClass();
      this.label = get.getAnnotation();   
      this.items = get.getDependents();
      this.item = get.getDependent();
      this.type = get.getType();   
      this.name = get.getName();
      this.set = set;
      this.get = get;
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
      return set == null;
   }
   
   /**
    * This returns the get part of the method. Acquiring separate
    * parts of the method ensures that method parts can be inherited
    * easily between types as overriding either part of a property.
    * 
    * @return this returns the get part of the method contact
    */
   public MethodPart getRead() {
      return get;
   }
   
   /**
    * This returns the set part of the method. Acquiring separate
    * parts of the method ensures that method parts can be inherited
    * easily between types as overriding either part of a property.
    * 
    * @return this returns the set part of the method contact
    */
   public MethodPart getWrite() {
      return set;
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
      T result = get.getAnnotation(type);
      
      if(type == label.annotationType()) {
         return (T) label;
      }
      if(result == null && set != null) {        
         return set.getAnnotation(type);
      }
      return result;
   }

   /**
    * This will provide the contact type. The contact type is the
    * class that is to be set and get on the object. This represents
    * the return type for the get and the parameter for the set.
    *
    * @return this returns the type that this contact represents
    */
   public Class getType() {
      return type;
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
      return item;
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
      return items;
   } 
   
   /**
    * This is the class that declares the contact. The declaring
    * class is where the method represented has been defined. This 
    * will typically be a class rather than an interface.
    * 
    * @return this returns the class the contact is declared within
    */
   public Class getDeclaringClass() {
      return owner;
   }
   
   /**
    * This is used to acquire the name of the method. This returns
    * the name of the method without the get, set or is prefix that
    * represents the Java Bean method type. Also this decapitalizes
    * the resulting name. The result is used to represent the XML
    * attribute of element within the class schema represented.
    * 
    *  @return this returns the name of the method represented
    */
   public String getName() {
      return name;
   }   

   /**
    * This is used to set the specified value on the provided object.
    * The value provided must be an instance of the contact class so
    * that it can be set without a runtime class compatibility error.
    *
    * @param source this is the object to set the value on
    * @param value this is the value that is to be set on the object
    */    
   public void set(Object source, Object value) throws Exception{
      Method method = get.getMethod();
      Class type = method.getDeclaringClass();
      
      if(set == null) {
         throw new MethodException("Property '%s' is read only in %s", name, type);
      }
      set.getMethod().invoke(source, value);
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
      return get.getMethod().invoke(source);
   }
   
   /**
    * This is used to describe the contact as it exists within the
    * owning class. It is used to provide error messages that can
    * be used to debug issues that occur when processing a contact.
    * The string provided contains both the set and get methods.
    * 
    * @return this returns a string representation of the contact
    */
   public String toString() {
      return String.format("method '%s'", name);
   }
}
