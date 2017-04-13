/*
 * AnnotationHandler.java December 2009
 *
 * Copyright (C) 2009, Niall Gallagher <niallg@users.sf.net>
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * The <code>AnnotationHandler</code> object is used to handle all
 * invocation made on a synthetic annotation. This is required so
 * that annotations can be created without an implementation. The
 * <code>java.lang.reflect.Proxy</code> object is used to wrap this
 * invocation handler with the annotation interface. 
 * 
 * @author Niall Gallagher
 */
class AnnotationHandler implements InvocationHandler {

   /**
    * This is the method used to acquire the associated type.
    */
   private static final String CLASS = "annotationType";
   
   /**
    * This is used to acquire a string value for the annotation.
    */
   private static final String STRING = "toString";
   
   /**
    * This is used to determine if annotations are optional.
    */
   private static final String REQUIRED = "required";
   
   /**
    * This is used to determine if a key should be an attribute.
    */
   private static final String ATTRIBUTE = "attribute";
   
   /**
    * This is used to perform a comparison of the annotations.
    */
   private static final String EQUAL = "equals";

   /**
    * This is used to perform a comparison of the annotations.
    */
   private final Comparer comparer;
   
   /**
    * This is annotation type associated with this handler. 
    */
   private final Class type;
   
   /**
    * This determines if a map should have a key attribute.
    */
   private final boolean attribute;
   
   /**
    * This is used to determine if the annotation is required.
    */
   private final boolean required;
   
   /**
    * Constructor for the <code>AnnotationHandler</code> object. This 
    * is used to create a handler for invocations on a synthetic 
    * annotation. The annotation type wrapped must be provided. By
    * default the requirement of the annotations is true.
    * 
    * @param type this is the annotation type that this is wrapping
    */
   public AnnotationHandler(Class type) {
      this(type, true);
   }

   /**
    * Constructor for the <code>AnnotationHandler</code> object. This 
    * is used to create a handler for invocations on a synthetic 
    * annotation. The annotation type wrapped must be provided.
    * 
    * @param type this is the annotation type that this is wrapping
    * @param required this is used to determine if its required
    */
   public AnnotationHandler(Class type, boolean required) {
      this(type, required, false);
   }
   
   /**
    * Constructor for the <code>AnnotationHandler</code> object. This 
    * is used to create a handler for invocations on a synthetic 
    * annotation. The annotation type wrapped must be provided.
    * 
    * @param type this is the annotation type that this is wrapping
    * @param required this is used to determine if its required
    * @param attribute determines if map keys are attributes
    */
   public AnnotationHandler(Class type, boolean required, boolean attribute) {
      this.comparer = new Comparer();
      this.attribute = attribute;
      this.required = required;
      this.type = type;
   }

   /**
    * This is used to handle all invocations on the wrapped annotation.
    * Typically the response to an invocation will result in the
    * default value of the annotation attribute being returned. If the
    * method is an <code>equals</code> or <code>toString</code> then
    * this will be handled by an internal implementation.
    * 
    * @param proxy this is the proxy object the invocation was made on
    * @param method this is the method that was invoked on the proxy
    * @param list this is the list of parameters to be used
    * 
    * @return this is used to return the result of the invocation
    */
   public Object invoke(Object proxy, Method method, Object[] list) throws Throwable {
      String name = method.getName();

      if(name.equals(STRING)) {
         return toString();
      }
      if(name.equals(EQUAL)) {
         return equals(proxy, list);
      }
      if(name.equals(CLASS)) {
         return type;
      }
      if(name.equals(REQUIRED)) {
         return required;
      }
      if(name.equals(ATTRIBUTE)) {
         return attribute;
      }
      return method.getDefaultValue();
   }

   /**
    * This is used to determine if two annotations are equals based
    * on the attributes of the annotation. The comparison done can
    * ignore specific attributes, for instance the name attribute.
    * 
    * @param proxy this is the annotation the invocation was made on
    * @param list this is the parameters provided to the invocation
    * 
    * @return this returns true if the annotations are equals
    */
   private boolean equals(Object proxy, Object[] list) throws Throwable {
      Annotation left = (Annotation) proxy;
      Annotation right = (Annotation) list[0];

      if(left.annotationType() != right.annotationType()) {
         throw new PersistenceException("Annotation %s is not the same as %s", left, right);
      }
      return comparer.equals(left, right);
   }

   /**
    * This is used to build a string from the annotation. The string
    * produces adheres to the typical string representation of a
    * normal annotation. This ensures that an exceptions that are
    * thrown with a string representation of the annotation are
    * identical to those thrown with a normal annotation.
    *
    * @return returns a string representation of the annotation 
    */
   public String toString() {
      StringBuilder builder = new StringBuilder();
 
      if(type != null) {
         name(builder);
         attributes(builder);
      }
      return builder.toString();
   }
   
   /**
    * This is used to build a string from the annotation. The string
    * produces adheres to the typical string representation of a
    * normal annotation. This ensures that an exceptions that are
    * thrown with a string representation of the annotation are
    * identical to those thrown with a normal annotation.
    * 
    * @param builder this is the builder used to compose the text
    */
   private void name(StringBuilder builder) {
      String name = type.getName();
      
      if(name != null) {
         builder.append('@');
         builder.append(name);
         builder.append('(');
      }  
   }
   
   /**
    * This is used to build a string from the annotation. The string
    * produces adheres to the typical string representation of a
    * normal annotation. This ensures that an exceptions that are
    * thrown with a string representation of the annotation are
    * identical to those thrown with a normal annotation.
    * 
    * @param builder this is the builder used to compose the text
    */
   private void attributes(StringBuilder builder) {
      Method[] list = type.getDeclaredMethods();

      for(int i = 0; i < list.length; i++) {
         String attribute = list[i].getName();
         Object value = value(list[i]);
         
         if(i > 0) {
            builder.append(',');
            builder.append(' ');
         }
         builder.append(attribute);
         builder.append('=');
         builder.append(value);
      }
      builder.append(')');
   }
   
   /**
    * This is used to extract the default value used for the provided
    * annotation attribute. This will return the default value for 
    * all attributes except that it makes the requirement optional.
    * Making the requirement optional provides better functionality.
    * 
    * @param method this is the annotation representing the attribute
    * 
    * @return this returns the default value for the attribute
    */
   private Object value(Method method) {
      String name = method.getName();
              
      if(name.equals(REQUIRED)) {
         return required;
      }
      if(name.equals(ATTRIBUTE)) {
         return attribute;
      }
      return method.getDefaultValue();
   }
}
