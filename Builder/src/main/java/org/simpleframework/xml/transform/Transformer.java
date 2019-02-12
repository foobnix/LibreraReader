/*
 * Transformer.java May 2007
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

import java.util.Map;

import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>Transformer</code> object is used to convert strings to
 * and from object instances. This is used during the serialization
 * and deserialization process to transform types from the Java class
 * libraries, as well as other types which do not contain XML schema
 * annotations. Typically this will be used to transform primitive
 * types to and from strings, such as <code>int</code> values.
 * <pre>
 * 
 *    &#64;Element
 *    private String[] value;
 *    
 * </pre>
 * For example taking the above value the array of strings needs to 
 * be converted in to a single string value that can be inserted in 
 * to the element in such a way that in can be read later. In this
 * case the serialized value of the string array would be as follows.
 * <pre>
 * 
 *    &lt;value&gt;one, two, three&lt;/value&gt;
 * 
 * </pre>
 * Here each non-null string is inserted in to a comma separated  
 * list of values, which can later be deserialized. Just to note the
 * above array could be annotated with <code>ElementList</code> just
 * as easily, in which case each entry would have its own element.
 * The choice of which annotation to use is up to the developer. A
 * more obvious benefit to transformations like this can be seen for
 * values annotated with the <code>Attribute</code> annotation.
 * 
 * @author Niall Gallagher
 */
public class Transformer {

   /**
    * This is used to cache all transforms matched to a given type.
    */
   private final Cache<Transform> cache;
   
   /**
    * This is used to cache the types that to not have a transform.
    */ 
   private final Cache<Object> error;

   /**
    * This is used to perform the matching of types to transforms.
    */
   private final Matcher matcher;
   
   /**
    * Constructor for the <code>Transformer</code> object. This is
    * used to create a transformer which will transform specified
    * types using transforms loaded from the class path. Transforms
    * are matched to types using the specified matcher object.
    * 
    * @param matcher this is used to match types to transforms
    */
   public Transformer(Matcher matcher) {  
      this.cache = new ConcurrentCache<Transform>();
      this.error = new ConcurrentCache<Object>();
      this.matcher = new DefaultMatcher(matcher);
   }
   
   /**
    * This method is used to convert the string value given to an
    * appropriate representation. This is used when an object is
    * being deserialized from the XML document and the value for
    * the string representation is required.
    * 
    * @param value this is the string representation of the value
    * @param type this is the type to convert the string value to
    * 
    * @return this returns an appropriate instanced to be used
    */
   public Object read(String value, Class type) throws Exception {
      Transform transform = lookup(type);

      if(transform == null) {
         throw new TransformException("Transform of %s not supported", type);
      }      
      return transform.read(value);
   }
   
   /**
    * This method is used to convert the provided value into an XML
    * usable format. This is used in the serialization process when
    * there is a need to convert a field value in to a string so 
    * that that value can be written as a valid XML entity.
    * 
    * @param value this is the value to be converted to a string
    * @param type this is the type to convert to a string value
    * 
    * @return this is the string representation of the given value
    */
   public String write(Object value, Class type) throws Exception {
      Transform transform = lookup(type);

      if(transform == null) {
         throw new TransformException("Transform of %s not supported", type);
      }
      return transform.write(value);
   }

   /**
    * This method is used to determine if the type specified can be
    * transformed. This will use the <code>Matcher</code> to find a
    * suitable transform, if one exists then this returns true, if
    * not then this returns false. This is used during serialization
    * to determine how to convert a field or method parameter. 
    *
    * @param type the type to determine whether its transformable
    * 
    * @return true if the type specified can be transformed by this
    */ 
   public boolean valid(Class type) throws Exception {   
      return lookup(type) != null;
   }

   /**
    * This method is used to acquire a <code>Transform</code> for 
    * the the specified type. If there is no transform for the type
    * then this will return null. Once acquired once the transform
    * is cached so that subsequent lookups will be performed faster.
    *
    * @param type the type to determine whether its transformable
    *
    * @return this will return a transform for the specified type
    */ 
   private Transform lookup(Class type) throws Exception {
      if(!error.contains(type)) {
         Transform transform = cache.fetch(type);            
   
         if(transform != null) {
            return transform;
         }          
         return match(type);
      }
      return null;
   }

   /**
    * This method is used to acquire a <code>Transform</code> for 
    * the the specified type. If there is no transform for the type
    * then this will return null. Once acquired once the transform
    * is cached so that subsequent lookups will be performed faster.
    *
    * @param type the type to determine whether its transformable
    *
    * @return this will return a transform for the specified type
    */ 
   private Transform match(Class type) throws Exception {
      Transform transform = matcher.match(type);
      
      if(transform != null) {
         cache.cache(type, transform);
      } else {
         error.cache(type, this);               
      }
      return transform;
   }
}
