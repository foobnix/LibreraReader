/*
 * FieldDetail.java July 2012
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
import java.lang.reflect.Field;

/**
 * The <code>FieldDetail</code> represents a field and acts as a 
 * means to cache all of the details associated with the field. 
 * This is primarily used to cache data associated with the field
 * as some platforms do not perform well with reflection.
 * 
 * @author Niall Gallagher
 */
class FieldDetail {

   /**
    * This contains all the annotations declared on the field.
    */
   private final Annotation[] list;
   
   /**
    * This is the field that this instance is representing.
    */
   private final Field field;
   
   /**
    * This contains the name of the field that is represented.
    */
   private final String name;
   
   /**
    * Constructor for the <code>FieldDetail</code> object. This takes
    * a field that has been extracted from a class. All of the details
    * such as the annotations and the field name are stored.
    * 
    * @param field this is the field that is represented by this
    */
   public FieldDetail(Field field) {
      this.list = field.getDeclaredAnnotations();
      this.name = field.getName();
      this.field = field;
   }
   
   /**
    * This returns the list of annotations that are associated with 
    * the field. The annotations are extracted only once and cached
    * internally, which improves the performance of serialization as
    * reflection on the field needs to be performed only once.
    * 
    * @return this returns the annotations associated with the field
    */
   public Annotation[] getAnnotations() {
      return list;
   }
   
   /**
    * This is the field that is represented by this detail. The field
    * is provided so that it can be invoked to set or get the data
    * that is referenced by the field during serialization.
    * 
    * @return this returns the field represented by this detail
    */
   public Field getField() {
      return field;
   }
   
   /**
    * This is used to extract the name of the field. The name here
    * is the actual name of the field rather than the name used by
    * the XML representation of the field.
    * 
    * @return this returns the actual name of the field
    */
   public String getName() {
      return name;
   }
}
