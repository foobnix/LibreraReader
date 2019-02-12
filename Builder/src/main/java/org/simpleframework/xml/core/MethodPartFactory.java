/*
 * MethodPartFactory.java April 2007
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
 * The <code>MethodPartFactory</code> is used to create method parts
 * based on the method signature and the XML annotation. This is 
 * effectively where a method is classified as either a getter or a
 * setter method within an object. In order to determine the type of
 * method the method name is checked to see if it is prefixed with
 * either the "get", "is", or "set" tokens.
 * <p>
 * Once the method is determined to be a Java Bean method according 
 * to conventions the method signature is validated. If the method
 * signature does not follow a return type with no arguments for the
 * get method, and a single argument for the set method then this
 * will throw an exception.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.MethodScanner
 */
class MethodPartFactory {
   
   /**
    * This is used to create the synthetic annotations for methods.
    */
   private final AnnotationFactory factory;
   
   /**
    * Constructor for the <code>MethodPartFactory</code> object. This
    * is used to create method parts based on the method signature 
    * and the XML annotation is uses. The created part can be used to
    * either set or get values depending on its type.
    * 
    * @param detail this contains details for the annotated class
    * @param support this contains various support functions
    */
   public MethodPartFactory(Detail detail, Support support) {
      this.factory = new AnnotationFactory(detail, support);
   }
   
   /**
    * This is used to acquire a <code>MethodPart</code> for the method
    * provided. This will synthesize an XML annotation to be used for
    * the method. If the method provided is not a setter or a getter
    * then this will return null, otherwise it will return a part with
    * a synthetic XML annotation. In order to be considered a valid
    * method the Java Bean conventions must be followed by the method.
    * 
    * @param method this is the method to acquire the part for
    * @param list this is the list of annotations on the method
    * 
    * @return this is the method part object for the method
    */
   public MethodPart getInstance(Method method, Annotation[] list) throws Exception {
      Annotation label = getAnnotation(method);
      
      if(label != null) {
         return getInstance(method, label, list);
      }
      return null;
   }
   
   /**
    * This is used to acquire a <code>MethodPart</code> for the name
    * and annotation of the provided method. This will determine the
    * method type by examining its signature. If the method follows
    * Java Bean conventions then either a setter method part or a
    * getter method part is returned. If the method does not comply
    * with the conventions an exception is thrown.
    * 
    * @param method this is the method to acquire the part for
    * @param label this is the annotation associated with the method
    * @param list this is the list of annotations on the method
    * 
    * @return this is the method part object for the method
    */
   public MethodPart getInstance(Method method, Annotation label, Annotation[] list) throws Exception {
      MethodName name = getName(method, label);
      MethodType type = name.getType();
      
      if(type == MethodType.SET) {
         return new SetPart(name, label, list);
      }
      return new GetPart(name, label, list);
   }
   
   /**
    * This is used to acquire a <code>MethodName</code> for the name
    * and annotation of the provided method. This will determine the
    * method type by examining its signature. If the method follows
    * Java Bean conventions then either a setter method name or a
    * getter method name is returned. If the method does not comply
    * with the conventions an exception is thrown.
    * 
    * @param method this is the method to acquire the name for
    * @param label this is the annotation associated with the method
    * 
    * @return this is the method name object for the method
    */
   private MethodName getName(Method method, Annotation label) throws Exception {
      MethodType type = getMethodType(method);
      
      if(type == MethodType.GET) {
         return getRead(method, type);
      }
      if(type == MethodType.IS) {
         return getRead(method, type);         
      }
      if(type == MethodType.SET) {
         return getWrite(method, type);
      }
      throw new MethodException("Annotation %s must mark a set or get method", label);      
   }    

   /**
    * This is used to acquire a <code>MethodType</code> for the name
    * of the method provided. This will determine the method type by 
    * examining its prefix. If the name follows Java Bean conventions 
    * then either a setter method type is returned. If the name does
    * not comply with the naming conventions then null is returned.
    * 
    * @param method this is the method to acquire the type for
    * 
    * @return this is the method name object for the method    
    */
   private MethodType getMethodType(Method method) {
      String name = method.getName();      
      
      if(name.startsWith("get")) {
         return MethodType.GET;
      }
      if(name.startsWith("is")) {
         return MethodType.IS;         
      }
      if(name.startsWith("set")) {
         return MethodType.SET;
      }
      return MethodType.NONE;
   }
   
   /**
    * This is used to synthesize an XML annotation given a method. The
    * provided method must follow the Java Bean conventions and either
    * be a getter or a setter. If this criteria is satisfied then a
    * suitable XML annotation is created to be used. Typically a match
    * is performed on whether the method type is a Java collection or
    * an array, if neither criteria are true a normal XML element is
    * used. Synthesizing in this way ensures the best results.
    * 
    * @param method this is the method to extract the annotation for
    * 
    * @return an XML annotation or null if the method is not suitable
    */
   private Annotation getAnnotation(Method method) throws Exception {
      Class[] dependents = getDependents(method);
      Class type = getType(method);
      
      if(type != null) {
         return factory.getInstance(type, dependents);
      }
      return null;
   }
   
   /**
    * This is used extract the dependents of the method. Extracting
    * the dependents in this way ensures that they can be used when
    * creating a default annotation. Any default annotation can then
    * create the optimal attributes for the method it represents.
    * 
    * @param method this is the method to acquire the dependents for
    * 
    * @return this returns the dependents for the method
    */
   private Class[] getDependents(Method method) throws Exception {
      MethodType type = getMethodType(method);
      
      if(type == MethodType.SET) {
         return Reflector.getParameterDependents(method, 0);
      }
      if(type == MethodType.GET) {
         return Reflector.getReturnDependents(method);
      }
      if(type == MethodType.IS) {
         return Reflector.getReturnDependents(method);
      }
      return null;
   }
   
   /**
    * This is used to extract the type from a method. Type type of a
    * method is the return type for a getter and a parameter type for
    * a setter. Such a parameter will only be returned if the method
    * observes the Java Bean conventions for a property method.
    * 
    * @param method this is the method to acquire the type for
    * 
    * @return this returns the type associated with the method
    */
   public Class getType(Method method) throws Exception {
      MethodType type = getMethodType(method);
      
      if(type == MethodType.SET) {
         return getParameterType(method);
      }
      if(type == MethodType.GET) {
         return getReturnType(method);
      }
      if(type == MethodType.IS) {
         return getReturnType(method);
      }
      return null;
   }
   
   /**
    * This is the parameter type associated with the provided method.
    * The first parameter is returned if the provided method is a
    * setter. If the method takes more than one parameter or if it
    * takes no parameters then null is returned from this.
    * 
    * @param method this is the method to get the parameter type for
    * 
    * @return this returns the parameter type associated with it
    */
   private Class getParameterType(Method method) throws Exception {
      Class[] list = method.getParameterTypes();
      
      if(list.length == 1) {
         return method.getParameterTypes()[0];
      }
      return null;
   }
   
   /**
    * This is the return type associated with the provided method.
    * The return type of the method is provided only if the method
    * adheres to the Java Bean conventions regarding getter methods.
    * If the method takes a parameter then this will return null.
    * 
    * @param method this is the method to get the return type for
    * 
    * @return this returns the return type associated with it
    */
   private Class getReturnType(Method method) throws Exception {
      Class[] list = method.getParameterTypes();
      
      if(list.length == 0) {
         return method.getReturnType();
      }
      return null;
   }
   
   /**
    * This is used to acquire a <code>MethodName</code> for the name
    * and annotation of the provided method. This must be a getter
    * method, and so must have a return type that is not void and 
    * have not arguments. If the method has arguments an exception 
    * is thrown, if not the Java Bean method name is provided.
    *
    * @param method this is the method to acquire the name for
    * @param type this is the method type to acquire the name for    
    * 
    * @return this is the method name object for the method
    */
   private MethodName getRead(Method method, MethodType type) throws Exception {
      Class[] list = method.getParameterTypes();
      String real = method.getName();
         
      if(list.length != 0) {
         throw new MethodException("Get method %s is not a valid property", method);
      }
      String name = getTypeName(real, type);
      
      if(name == null) {
         throw new MethodException("Could not get name for %s", method);
      }
      return new MethodName(method, type, name);
   }

   /**
    * This is used to acquire a <code>MethodName</code> for the name
    * and annotation of the provided method. This must be a setter
    * method, and so must accept a single argument, if it contains 
    * more or less than one argument an exception is thrown.
    * return type that is not void and
    *
    * @param method this is the method to acquire the name for
    * @param type this is the method type to acquire the name for    
    * 
    * @return this is the method name object for the method
    */
   private MethodName getWrite(Method method, MethodType type) throws Exception {
      Class[] list = method.getParameterTypes();
      String real = method.getName();
      
      if(list.length != 1) {
         throw new MethodException("Set method %s is not a valid property", method);         
      }
      String name = getTypeName(real, type);
      
      if(name == null) {
         throw new MethodException("Could not get name for %s", method);
      }
      return new MethodName(method, type, name);
   }
   
   /**
    * This is used to acquire the name of the method in a Java Bean
    * property style. Thus any "get", "is", or "set" prefix is 
    * removed from the name and the following character is changed
    * to lower case if it does not represent an acronym.
    * 
    * @param name this is the name of the method to be converted
    * @param type this is the type of method the name represents
    * 
    * @return this returns the Java Bean name for the method
    */
   private String getTypeName(String name, MethodType type) {
      int prefix = type.getPrefix();
      int size = name.length();
      
      if(size > prefix) {
         name = name.substring(prefix, size);
      }
      return Reflector.getName(name);          
   }
}