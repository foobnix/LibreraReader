/*
 * MethodDetail.java July 2012
 *
 * Copyright (C) 2012, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>MethodDetail</code> represents a method and acts as a 
 * means to cache all of the details associated with the method. 
 * This is primarily used to cache data associated with the method
 * as some platforms do not perform well with reflection.
 * 
 * @author Niall Gallagher
 */
class MethodDetail {
   
   /**
    * This contains all the annotations declared on the method.
    */
   private final Annotation[] list;
   
   /**
    * This is the method that this instance is representing.
    */
   private final Method method;
   
   /**
    * This contains the name of the method that is represented.
    */
   private final String name;
   
   /**
    * Constructor for the <code>MethodDetail</code> object. This takes
    * a method that has been extracted from a class. All of the details
    * such as the annotations and the method name are stored.
    * 
    * @param method this is the method that is represented by this
    */
   public MethodDetail(Method method) {
      this.list = method.getDeclaredAnnotations();
      this.name = method.getName();
      this.method = method;
   }
   
   /**
    * This returns the list of annotations that are associated with 
    * the method. The annotations are extracted only once and cached
    * internally, which improves the performance of serialization as
    * reflection on the method needs to be performed only once.
    * 
    * @return this returns the annotations associated with the method
    */
   public Annotation[] getAnnotations() {
      return list;
   }
   
   /**
    * This is the method that is represented by this detail. The method
    * is provided so that it can be invoked to set or get the data
    * that is referenced by the method during serialization.
    * 
    * @return this returns the method represented by this detail
    */
   public Method getMethod() {
      return method;
   }
   
   /**
    * This is used to extract the name of the method. The name here
    * is the actual name of the method rather than the name used by
    * the XML representation of the method.
    * 
    * @return this returns the actual name of the method
    */
   public String getName() {
      return name;
   }
}
