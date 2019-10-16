/*
 * AnnotationFactory.java January 2010
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

import static org.simpleframework.xml.stream.Verbosity.LOW;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.Verbosity;

/**
 * The <code>AnnotationFactory</code> is used to create annotations
 * using a given class. This will classify the provided type as
 * either a list, map, array, or a default object. Depending on the
 * type provided a suitable annotation will be created. Annotations
 * produced by this will have default attribute values.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.AnnotationHandler
 */
class AnnotationFactory {  
   
   /**
    * This represents the format used for the serialization process.
    */
   private final Format format;
   
   /**
    * This is used to determine if the defaults are required.
    */
   private final boolean required;
   
   /**
    * Constructor for the <code>AnnotationFactory</code> object. This
    * is used to create a factory for annotations used to provide
    * the default annotations for generated labels.
    * 
    * @param detail this contains details for the annotated class
    * @param support this contains various support functions
    */
   public AnnotationFactory(Detail detail, Support support) {
      this.required = detail.isRequired();
      this.format = support.getFormat();
   }
  
   /**
    * This is used to create an annotation for the provided type.
    * Annotations created are used to match the type provided. So
    * a <code>List</code> will have an <code>ElementList</code>
    * annotation for example. Matching the annotation to the
    * type ensures the best serialization for that type. 
    * 
    * @param type the type to create the annotation for
    * @param dependents these are the dependents for the type
    * 
    * @return this returns the synthetic annotation to be used
    */
   public Annotation getInstance(Class type, Class[] dependents) throws Exception { 
      ClassLoader loader = getClassLoader();
      
      if(Map.class.isAssignableFrom(type)) {
         if(isPrimitiveKey(dependents) && isAttribute()) { 
            return getInstance(loader, ElementMap.class, true);
         }
         return getInstance(loader, ElementMap.class);
      }
      if(Collection.class.isAssignableFrom(type)) {
         return getInstance(loader, ElementList.class);
      }
      return getInstance(type);
   }
   
   /**
    * This is used to create an annotation for the provided type.
    * Annotations created are used to match the type provided. So
    * an array of objects will have an <code>ElementArray</code>
    * annotation for example. Matching the annotation to the
    * type ensures the best serialization for that type. 
    * 
    * @param type the type to create the annotation for
    * 
    * @return this returns the synthetic annotation to be used
    */
   private Annotation getInstance(Class type) throws Exception {
      ClassLoader loader = getClassLoader();
      Class entry = type.getComponentType();
      
      if(type.isArray()) {
         if(isPrimitive(entry)) {
            return getInstance(loader, Element.class);
         }
         return getInstance(loader, ElementArray.class);
      }
      if(isPrimitive(type) && isAttribute()) {
         return getInstance(loader, Attribute.class);
      }
      return getInstance(loader, Element.class);
   }
   
   /**
    * This will create a synthetic annotation using the provided 
    * interface. All attributes for the provided annotation will
    * have their default values. 
    * 
    * @param loader this is the class loader to load the annotation 
    * @param label this is the annotation interface to be used
    * 
    * @return this returns the synthetic annotation to be used
    */
   private Annotation getInstance(ClassLoader loader, Class label) throws Exception {
      return getInstance(loader, label, false);
   }
   
   /**
    * This will create a synthetic annotation using the provided 
    * interface. All attributes for the provided annotation will
    * have their default values. 
    * 
    * @param loader this is the class loader to load the annotation 
    * @param label this is the annotation interface to be used
    * @param attribute determines if a map has an attribute key
    * 
    * @return this returns the synthetic annotation to be used
    */
   private Annotation getInstance(ClassLoader loader, Class label, boolean attribute) throws Exception {
      AnnotationHandler handler = new AnnotationHandler(label, required, attribute);
      Class[] list = new Class[] {label};
      
      return (Annotation) Proxy.newProxyInstance(loader, list, handler);
   }
   
   /**
    * This is used to create a suitable class loader to be used to
    * load the synthetic annotation classes. The class loader
    * provided will be the same as the class loader that was used
    * to load this class.
    * 
    * @return this returns the class loader that is to be used
    */
   private ClassLoader getClassLoader() throws Exception {
      return AnnotationFactory.class.getClassLoader();
   }
   
   /**
    * This is used to determine if a map contains a primitive key.
    * A primitive key is a key for a <code>Map</code> that is of
    * a primitive type and thus can be used as an attribute. Here
    * we accept all primitive types and also enumerations.
    * 
    * @param dependents these are the dependents of the map
    * 
    * @return this returns true if the key is a primitive type
    */
   private boolean isPrimitiveKey(Class[] dependents) {
      if(dependents != null && dependents.length > 0) {
         Class parent = dependents[0].getSuperclass();
         Class type = dependents[0];
         
         if(parent != null) {
            if(parent.isEnum()) {
               return true;
            }
            if(type.isEnum()) {
               return true;
            }
         }
         return isPrimitive(type);
      }
      return false;
   }
   
   /**
    * This is used to determine if the type specified is primitive.
    * A primitive is any type that can be reliably transformed in
    * to an XML attribute without breaking the XML.
    * 
    * @param type this is the type that is to be evaluated 
    * 
    * @return true if the type provided is a primitive type
    */
   private boolean isPrimitive(Class type) {
      if(Number.class.isAssignableFrom(type)) {
         return true;
      }
      if(type == Boolean.class) {
         return true;
      }
      if(type == Character.class) {
         return true;
      }
      return type.isPrimitive();
   }
   
   /**
    * This is used to determine whether the format for the current
    * serialization is verbose or not. The verbosity dictates the
    * type of default annotations that are generated for an object.
    * 
    * @return this is used to determine the verbosity to use
    */
   private boolean isAttribute() {
      Verbosity verbosity = format.getVerbosity();
      
      if(verbosity != null) {
         return verbosity == LOW;
      }
      return false;
   }
}
