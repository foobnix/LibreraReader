/*
 * ConverterScanner.java January 2010
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

package org.simpleframework.xml.convert;

import java.lang.annotation.Annotation;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Value;

/**
 * The <code>ConverterScanner</code> is used to create a converter 
 * given a method or field representation. Creation of the converter
 * is done using the <code>Convert</code> annotation, which may
 * be used to annotate a field, method or class. This describes the
 * implementation to use for object serialization. To account for
 * polymorphism the type scanned for annotations can be overridden
 * from type provided in the <code>Type</code> object. This ensures
 * that if a collection of objects are serialized the correct
 * implementation will be used for each type or subtype.
 * 
 * @author Niall Gallagher
 */
class ConverterScanner {
   
   /**
    * This is used to instantiate converters given the type.
    */
   private final ConverterFactory factory;
   
   /**
    * This is used to build a scanner to scan for annotations.
    */
   private final ScannerBuilder builder;
   
   /**
    * Constructor for the <code>ConverterScanner</code> object. This
    * uses an internal factory to instantiate and cache all of the
    * converters created. This will ensure that there is reduced
    * overhead for a serialization process using converters.
    */
   public ConverterScanner() {
      this.factory = new ConverterFactory();
      this.builder = new ScannerBuilder();
   }
   
   /**
    * This method will lookup and instantiate a converter found from
    * scanning the field or method type provided. If the type has
    * been overridden then the <code>Value</code> object will provide
    * the type to scan. If no annotation is found on the class, field
    * or method then this will return null.
    * 
    * @param type this is the type to search for the annotation
    * @param value this contains the type if it was overridden
    * 
    * @return a converter scanned from the provided field or method
    */
   public Converter getConverter(Type type, Value value) throws Exception {
      Class real = getType(type, value);
      Convert convert = getConvert(type, real);
      
      if(convert != null) {
         return factory.getInstance(convert);
      }
      return null;
   }
   
   /**
    * This method will lookup and instantiate a converter found from
    * scanning the field or method type provided. If the type has
    * been overridden then the object instance will provide the type 
    * to scan. If no annotation is found on the class, field or 
    * method then this will return null.
    * 
    * @param type this is the type to search for the annotation
    * @param value this contains the type if it was overridden
    * 
    * @return a converter scanned from the provided field or method
    */
   public Converter getConverter(Type type, Object value) throws Exception {
      Class real = getType(type, value);
      Convert convert = getConvert(type, real);
      
      if(convert != null) {
         return factory.getInstance(convert);
      }
      return null;
   }
   
   /**
    * This method is used to scan the provided <code>Type</code> for
    * an annotation. If the <code>Type</code> represents a field or
    * method then the annotation can be taken directly from that
    * field or method. If however the type represents a class then
    * the class itself must contain the annotation. 
    * 
    * @param type the field or method containing the annotation
    * @param real the type that represents the field or method
    * 
    * @return this returns the annotation on the field or method
    */
   private Convert getConvert(Type type, Class real) throws Exception {
      Convert convert = getConvert(type);
      
      if(convert == null) {
         return getConvert(real);
      }
      return convert;
   }
   
   /**
    * This method is used to scan the provided <code>Type</code> for
    * an annotation. If the <code>Type</code> represents a field or
    * method then the annotation can be taken directly from that
    * field or method. If however the type represents a class then
    * the class itself must contain the annotation. 
    * 
    * @param type the field or method containing the annotation
    * 
    * @return this returns the annotation on the field or method
    */
   private Convert getConvert(Type type) throws Exception {
      Convert convert = type.getAnnotation(Convert.class);
      
      if(convert != null) {
         Element element = type.getAnnotation(Element.class);
      
         if(element == null) {
            throw new ConvertException("Element annotation required for %s", type);
         }
      }
      return convert;
   }
   
   /**
    * This method is used to scan the provided <code>Type</code> for
    * an annotation. If the <code>Type</code> represents a field or
    * method then the annotation can be taken directly from that
    * field or method. If however the type represents a class then
    * the class itself must contain the annotation. 
    * 
    * @param real the type that represents the field or method
    * 
    * @return this returns the annotation on the field or method
    */
   private Convert getConvert(Class real) throws Exception {
      Convert convert = getAnnotation(real, Convert.class);
      
      if(convert != null) {
         Root root = getAnnotation(real, Root.class);
         
         if(root == null) {
            throw new ConvertException("Root annotation required for %s", real);
         }
      }
      return convert;
   }
   
   /**
    * This is used to acquire the <code>Convert</code> annotation from
    * the class provided. If the type does not contain the annotation
    * then this scans all supertypes until either an annotation is
    * found or there are no further supertypes. 
    * 
    * @param type this is the type to scan for annotations
    * @param label this is the annotation type that is to be found
    * 
    * @return this returns the annotation if found otherwise null
    */
   private <T extends Annotation> T getAnnotation(Class<?> type, Class<T> label) {
      return builder.build(type).scan(label);
   }
   
   /**
    * This is used to acquire the class that should be scanned. The
    * type is found either on the method or field, or should there
    * be a subtype then the class is taken from the provided value.
    * 
    * @param type this is the type representing the field or method
    * @param value this contains the type if it was overridden
    * 
    * @return this returns the class that has been scanned
    */
   private Class getType(Type type, Value value) {
      Class real = type.getType();
      
      if(value != null) {
         return value.getType();
      }
      return real;
   }
   
   /**
    * This is used to acquire the class that should be scanned. The
    * type is found either on the method or field, or should there
    * be a subtype then the class is taken from the provided value.
    * 
    * @param type this is the type representing the field or method
    * @param value this contains the type if it was overridden
    * 
    * @return this returns the class that has been scanned
    */
   private Class getType(Type type, Object value) {
      Class real = type.getType();
      
      if(value != null) {
         return value.getClass();
      }
      return real;
   }
}
