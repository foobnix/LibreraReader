/*
 * ArrayMatcher.java May 2007
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

package org.simpleframework.xml.transform;

/**
 * The <code>ArrayMatcher</code> object performs matching of array
 * types to array transforms. This uses the array component type to
 * determine the transform to be used. All array transforms created
 * by this will be <code>ArrayTransform</code> object instances. 
 * These will use a type transform for the array component to add
 * values to the individual array indexes. Also such transforms are
 * typically treated as a comma separated list of individual values.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.transform.ArrayTransform
 */
class ArrayMatcher implements Matcher {

   /**
    * This is the primary matcher that can resolve transforms.
    */
   private final Matcher primary;
   
   /**
    * Constructor for the <code>ArrayTransform</code> object. This
    * is used to match array types to their respective transform
    * using the <code>ArrayTransform</code> object. This will use
    * a comma separated list of tokens to populate the array.
    * 
    * @param primary this is the primary matcher to be used 
    */
   public ArrayMatcher(Matcher primary) {
      this.primary = primary;
   }
   
   /**
    * This is used to match a <code>Transform</code> based on the
    * array component type of an object to be transformed. This will
    * attempt to match the transform using the fully qualified class
    * name of the array component type. If a transform can not be
    * found then this method will throw an exception.
    * 
    * @param type this is the array to find the transform for
    * 
    * @throws Exception thrown if a transform can not be matched
    */
   public Transform match(Class type) throws Exception {
      Class entry = type.getComponentType();
      
      if(entry == char.class) {
         return new CharacterArrayTransform(entry);
      } 
      if(entry == Character.class) {
         return new CharacterArrayTransform(entry);
      }
      if(entry == String.class) {
         return new StringArrayTransform();
      }
      return matchArray(entry);
   }
   
   /**
    * This is used to match a <code>Transform</code> based on the
    * array component type of an object to be transformed. This will
    * attempt to match the transform using the fully qualified class
    * name of the array component type. If a transform can not be
    * found then this method will throw an exception.
    * 
    * @param entry this is the array component type to be matched
    * 
    * @throws Exception thrown if a transform can not be matched
    */
   private Transform matchArray(Class entry) throws Exception {            
      Transform transform = primary.match(entry);
     
      if(transform == null) {
         return null;
      }
      return new ArrayTransform(transform, entry);
   }
}
