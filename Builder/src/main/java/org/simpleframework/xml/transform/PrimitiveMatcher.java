/*
 * PrimitiveMatcher.java May 2007
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
 * The <code>PrimitiveMatcher</code> object is used to resolve the
 * primitive types to a stock transform. This will basically use
 * a transform that is used with the primitives language object.
 * This will always return a suitable transform for a primitive.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.transform.DefaultMatcher
 */
class PrimitiveMatcher implements Matcher { 
   
   /**
    * Constructor for the <code>PrimitiveMatcher</code> object. The
    * primitive matcher is used to resolve a transform instance to
    * convert primitive types to an from strings. If a match is not
    * found with this matcher then an exception is thrown.
    */
   public PrimitiveMatcher() {
      super();
   }
   
   /**
    * This method is used to match the specified type to primitive
    * transform implementations. If this is given a primitive then
    * it will always return a suitable <code>Transform</code>. If
    * however it is given an object type an exception is thrown.
    * 
    * @param type this is the primitive type to be transformed
    * 
    * @return this returns a stock transform for the primitive
    */
   public Transform match(Class type) throws Exception {     
      if(type == int.class) {
         return new IntegerTransform();
      }
      if(type == boolean.class) {
         return new BooleanTransform();
      }
      if(type == long.class) {
         return new LongTransform();
      }
      if(type == double.class) {
         return new DoubleTransform();
      }
      if(type == float.class) {
         return new FloatTransform();
      }
      if(type == short.class) {
         return new ShortTransform();
      }
      if(type == byte.class) {
         return new ByteTransform();
      }
      if(type == char.class) {
         return new CharacterTransform();
      }
      return null;
   }
}