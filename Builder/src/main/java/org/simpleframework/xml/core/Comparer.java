/*
 * Comparer.java December 2009
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
import java.lang.reflect.Method;

/**
 * The <code>Comparer</code> is used to compare annotations on the
 * attributes of that annotation. Unlike the <code>equals</code>
 * method, this can ignore some attributes based on the name of the
 * attributes. This is useful if some annotations have overridden
 * values, such as the field or method name.
 * 
 * @author Niall Gallagher
 */
class Comparer {
   
   /**
    * This is the default attribute to ignore for the comparer.
    */
   private static final String NAME = "name";
   
   /**
    * This is the list of names to ignore for this instance.
    */
   private final String[] ignore;
   
   /**
    * Constructor for the <code>Comparer</code> object. This is
    * used to create a comparer that has a default set of names
    * to be ignored during the comparison of annotations.
    */
   public Comparer() {
      this(NAME);
   }

   /**
    * Constructor for the <code>Comparer</code> object. This is
    * used to create a comparer that has a default set of names
    * to be ignored during the comparison of annotations.
    * 
    * @param ignore this is the set of attributes to be ignored
    */
   public Comparer(String... ignore) {
      this.ignore = ignore;
   }
   
   /**
    * This is used to determine if two annotations are equals based
    * on the attributes of the annotation. The comparison done can
    * ignore specific attributes, for instance the name attribute.
    * 
    * @param left this is the left side of the comparison done
    * @param right this is the right side of the comparison done
    * 
    * @return this returns true if the annotations are equal
    */
   public boolean equals(Annotation left, Annotation right) throws Exception {
      Class type = left.annotationType();
      Class expect = right.annotationType();
      Method[] list = type.getDeclaredMethods();

      if(type.equals(expect)) {
         for(Method method : list) {
            if(!isIgnore(method)) {
               Object value = method.invoke(left);
               Object other = method.invoke(right);
               
               if(!value.equals(other)) {
                  return false;
               }
            }
         }
         return true;
      }
      return false;
   }
   
   /**
    * This is used to determine if the method for an attribute is 
    * to be ignore. To determine if it should be ignore the method
    * name is compared against the list of attributes to ignore.
    * 
    * @param method this is the method to be evaluated
    * 
    * @return this returns true if the method should be ignored
    */
   private boolean isIgnore(Method method) {
      String name = method.getName();
      
      if(ignore != null) {
         for(String value : ignore) {
            if(name.equals(value)) {
               return true;
            }
         }
      }
      return false;
   }
}
