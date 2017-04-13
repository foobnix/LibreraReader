/*
 * Introspector.java February 2005
 *
 * Copyright (C) 2005, Niall Gallagher <niallg@users.sf.net>
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

import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.Format;

/**
 * The <code>Introspector</code> object is used to determine the details
 * to use for an annotated field or method using both the field an
 * annotation details. This allows defaults to be picked up from the
 * method or field type if that have not been explicitly overridden
 * in the annotation. 
 * 
 * @author Niall Gallagher
 */
class Introspector {
   
   /**
    * This is the actual annotation from the specified contact.
    */
   private final Annotation marker;
   
   /**
    * This is the field or method contact that has been annotated.
    */
   private final Contact contact; 
   
   /**
    * This is the format used to style the paths created.
    */
   private final Format format;
   
   /**
    * This is the label used to expose the annotation details.
    */
   private final Label label;
   
   /**
    * Constructor for the <code>Introspector</code> object. This is 
    * used to create an object that will use information available
    * within the field and annotation to determine exactly what 
    * the name of the XML element is to be and the type to use.
    * 
    * @param contact this is the method or field contact used
    * @param label this is the annotation on the contact object
    * @param format this is used to style the paths created
    */
   public Introspector(Contact contact, Label label, Format format) {
      this.marker = contact.getAnnotation();
      this.contact = contact;
      this.format = format;
      this.label = label;
   }
   
   /**
    * This is used to acquire the <code>Contact</code> for this. The
    * contact is the actual method or field that has been annotated
    * and is used to set or get information from the object instance.
    * 
    * @return the method or field that this signature represents
    */
   public Contact getContact() {
      return contact;
   }
   
   /**
    * This returns the dependent type for the annotation. This type
    * is the type other than the annotated field or method type that
    * the label depends on. For the <code>ElementList</code> this 
    * can be the generic parameter to an annotated collection type.
    * 
    * @return this is the type that the annotation depends on
    */
   public Type getDependent() throws Exception {
      return label.getDependent();
   }

   /**
    * This method is used to get the entry name of a label using 
    * the type of the label. This ensures that if there is no
    * entry XML element name declared by the annotation that a
    * suitable name can be calculated from the annotated type.
    * 
    * @return this returns a suitable XML entry element name
    */
   public String getEntry() throws Exception {
      Type depend = getDependent();   
      Class type = depend.getType();
      
      if(type.isArray()) {
         type = type.getComponentType();
      }
      return getName(type);
   }
   
   /**
    * This is used to acquire the name of the specified type using
    * the <code>Root</code> annotation for the class. This will 
    * use either the name explicitly provided by the annotation or
    * it will use the name of the class that the annotation was
    * placed on if there is no explicit name for the root.
    * 
    * @param type this is the type to acquire the root name for
    * 
    * @return this returns the name of the type from the root
    * 
    * @throws Exception if the class contains an illegal schema
    */
   private String getName(Class type) throws Exception {
      String name = getRoot(type);
      
      if(name != null) {
         return name;
      } else {
         name = type.getSimpleName();
      }
      return Reflector.getName(name);
   }
   
   /**
    * This will acquire the name of the <code>Root</code> annotation
    * for the specified class. This will traverse the inheritance
    * hierarchy looking for the root annotation, when it is found it
    * is used to acquire a name for the XML element it represents.
    *  
    * @param type this is the type to acquire the root name with
    * 
    * @return the root name for the specified type if it exists
    */
   private String getRoot(Class type) { 
      Class real = type;
         
      while(type != null) {
         String name = getRoot(real, type);
         
         if(name != null) {
           return name;
         }
         type = type.getSuperclass();
      }
      return null;     
   }
   
   /**
    * This will acquire the name of the <code>Root</code> annotation
    * for the specified class. This will traverse the inheritance
    * hierarchy looking for the root annotation, when it is found it
    * is used to acquire a name for the XML element it represents.
    *  
    * @param real the actual type of the object being searched
    * @param type this is the type to acquire the root name with    
    * 
    * @return the root name for the specified type if it exists
    */
   private String getRoot(Class<?> real, Class<?> type) {
      String name = type.getSimpleName();
      Root root = type.getAnnotation(Root.class);
      
      if(root != null) {
         String text = root.name();
          
         if(!isEmpty(text)) {
            return text;
         }
         return Reflector.getName(name);
      }
      return null;
   }
   
   /**
    * This is used to determine the name of the XML element that the
    * annotated field or method represents. This will determine based
    * on the annotation attributes and the dependent type required
    * what the name of the XML element this represents is. 
    * 
    * @return this returns the name of the XML element expected
    */
   public String getName() throws Exception {
      String entry = label.getEntry(); 
         
      if(!label.isInline()) {
         entry = getDefault();
      }
      return entry;
   }
   
   /**
    * This is used to acquire the name for an element by firstly
    * checking for an override in the annotation. If one exists
    * then this is returned if not then the name of the field
    * or method contact is returned. 
    * 
    * @return this returns the XML element name to be used
    */
   private String getDefault() throws Exception {
      String name = label.getOverride();

      if(!isEmpty(name)) {
         return name;
      }
      return contact.getName();
   }  
   
   /**
    * This method is used to return an XPath expression that is 
    * used to represent the position of a label. If there is no
    * XPath expression associated with this then an empty path is
    * returned. This will never return a null expression.
    * 
    * @return the XPath expression identifying the location
    */
   public Expression getExpression() throws Exception {
      String path = getPath();

      if(path != null) {
         return new PathParser(path, contact, format);
      }
      return new EmptyExpression(format);
   }
   
   /**
    * This is used to acquire the path of the element or attribute
    * that is used by the class schema. The path is determined by
    * acquiring the XPath expression and appending the name of the
    * label to form a fully qualified path.
    * 
    * @return returns the path that is used for the XML property
    */
   public String getPath() throws Exception {
      Path path = contact.getAnnotation(Path.class);
      
      if(path == null) {
         return null;
      }
      return path.value();
   }
   
   /**
    * This method is used to determine if a root annotation value is
    * an empty value. Rather than determining if a string is empty
    * be comparing it to an empty string this method allows for the
    * value an empty string represents to be changed in future.
    * 
    * @param value this is the value to determine if it is empty
    * 
    * @return true if the string value specified is an empty value
    */
   public boolean isEmpty(String value) {
      if(value != null) {
         return value.length() == 0;
      }
      return true;      
   }
   
   /**
    * This method is used to construct a string that describes the
    * signature of an XML annotated field or method. This will use
    * the <code>Contact</code> object and the annotation used for
    * that contact to construct a string that has sufficient
    * information such that it can be used in error reporting.
    * 
    * @return returns a string used to represent this signature 
    */
   public String toString() {
      return String.format("%s on %s", marker, contact);
   }
}
