/*
 * MethodName.java April 2007
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

import java.lang.reflect.Method;

/**
 * The <code>MethodName</code> object is used to represent the name
 * of a Java Bean method. This contains the Java Bean name the type
 * and the actual method it represents. This allows the scanner to
 * create <code>MethodPart</code> objects based on the method type.
 * 
 * @author Niall Gallagher
 */
class MethodName {
   
   /**
    * This is the type of method this method name represents.
    */
   private MethodType type;
   
   /**
    * This is the actual method that this method name represents.
    */
   private Method method;
   
   /**
    * This is the Java Bean method name that is represented.
    */
   private String name;
   
   /**
    * Constructor for the <code>MethodName</code> objects. This is
    * used to create a method name representation of a method based
    * on the method type and the Java Bean name of that method.
    * 
    * @param method this is the actual method this is representing
    * @param type type used to determine if it is a set or get
    * @param name this is the Java Bean property name of the method
    */
   public MethodName(Method method, MethodType type, String name) {
      this.method = method;
      this.type = type;      
      this.name = name;
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
    * This is the method type for the method part. This is used in
    * the scanning process to determine which type of method a
    * instance represents, this allows set and get methods to be
    * paired.
    * 
    * @return the method type that this part represents
    */ 
   public MethodType getType() {
      return type;
   }
   
   /**
    * This is the method for this point of contact. This is what
    * will be invoked by the serialization or deserialization 
    * process when an XML element or attribute is to be used.
    * 
    * @return this returns the method associated with this
    */
   public Method getMethod() {
      return method;      
   }  
}