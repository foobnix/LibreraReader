/*
 * LabelKey.java April 2012
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

/**
 * The <code>LabelKey</code> object is used to create a key that will
 * uniquely identify an annotated method within a class. Creation of 
 * a key in this way enables annotated methods and fields to be cached
 * and looked up using a key.
 * 
 * @author Niall Gallagher
 */
class LabelKey {

   /**
    * This is the annotation type that this is represented in the key.
    */
   private final Class label;
   
   /**
    * This is the declaring class where the method or field is defined.
    */
   private final Class owner;
   
   /**
    * This is the type that is represented by the annotated contact.
    */
   private final Class type;
   
   /**
    * This is the name of the field or method that is represented.
    */
   private final String name;
   
   /**
    * Constructor for the <code>LabelKey</code> object. This is used
    * to create an object using the contact and associated annotation
    * that can uniquely identity the label.
    * 
    * @param contact this is the contact that has been annotated
    * @param label this is the primary annotation associated with this
    */
   public LabelKey(Contact contact, Annotation label) {
      this.owner = contact.getDeclaringClass();
      this.label = label.annotationType();
      this.name = contact.getName();
      this.type = contact.getType();    
   }
   
   /**
    * This returns the unique has code used for this key. The hash 
    * code is created by combining the hash code of the method or field
    * name with the hash code of the declaring class.
    * 
    * @return this returns the hash code associated with this key
    */
   public int hashCode() {
      return name.hashCode() ^ owner.hashCode();
   }
   
   /**
    * This is used to determine if two keys are the same. Ultimately 
    * two keys are equal if they represent the same contact and
    * annotation from that contact. If everything is equal by
    * identity then this will be true.
    * 
    * @param value this is the value to compare to this key
    * 
    * @return this returns true if both keys have the same data
    */
   public boolean equals(Object value) {
      if(value instanceof LabelKey) {
         return equals((LabelKey)value);
      }
      return false;
   }
   
   /**
    * This is used to determine if two keys are the same. Ultimately 
    * two keys are equal if they represent the same contact and
    * annotation from that contact. If everything is equal by
    * identity then this will be true.
    * 
    * @param key this is the value to compare to this key
    * 
    * @return this returns true if both keys have the same data
    */
   private boolean equals(LabelKey key) {
      if(key == this) {
         return true;
      }
      if(key.label != label) {
         return false;
      }
      if(key.owner != owner) {
         return false;
      }
      if(key.type != type) {
         return false;
      }
      return key.name.equals(name);
   }
   
   /**
    * This returns a string representation of this key. It contains
    * the name and the declaring class for the method or field.
    * This is primarily used for debugging purposes.
    * 
    * @return this returns a string representation of this key
    */
   public String toString() {
      return String.format("key '%s' for %s", name, owner);
   }
}
