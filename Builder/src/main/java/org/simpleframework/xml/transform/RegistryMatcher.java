/*
 * RegistryMatcher.java May 2011
 *
 * Copyright (C) 2011, Niall Gallagher <niallg@users.sf.net>
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

import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>RegistryMatcher</code> provides a simple matcher backed
 * by a registry. Registration can be done to match a type to a
 * <code>Transform</code> class or instance. If a transform class is
 * registered an instance of it is created when requested using the
 * default no argument constructor of the type, it is then cached so 
 * it can be reused on future requests.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Persister
 */
public class RegistryMatcher implements Matcher {
   
   /**
    * This is used to fetch transform instances by type.
    */
   private final Cache<Transform> transforms;
   
   /**
    * This is used to determine the transform  for a type.
    */
   private final Cache<Class> types;
   
   /**
    * Constructor for the <code>RegistryMatcher</code>. This is used
    * to create a matcher instance that can resolve a transform by
    * type and can also instantiate new transforms if required. It 
    * is essentially a convenience implementation.
    */
   public RegistryMatcher() {
      this.transforms = new ConcurrentCache<Transform>();
      this.types = new ConcurrentCache<Class>();
   }
   
   /**
    * This is used to bind a <code>Transform</code> type. The first 
    * time a transform is requested for the specified type a new 
    * instance of this <code>Transform</code> will be instantiated.
    * 
    * @param type this is the type to resolve the transform for
    * @param transform this is the transform type to instantiate
    */
   public void bind(Class type, Class transform) {
      types.cache(type, transform);
   }
   
   /**
    * This is used to bind a <code>Transform</code> instance to the
    * specified type. Each time a transform is requested for this
    * type the provided instance will be returned.
    * 
    * @param type this is the type to resolve the transform for
    * @param transform this transform instance to be used
    */
   public void bind(Class type, Transform transform) {
      transforms.cache(type, transform);
   }
   
   /**
    * This is used to match a <code>Transform</code> using the type
    * specified. If no transform can be acquired then this returns
    * a null value indicating that no transform could be found.
    * 
    * @param type this is the type to acquire the transform for
    * 
    * @return returns a transform for processing the type given
    */ 
   public Transform match(Class type) throws Exception {
      Transform transform = transforms.fetch(type);
      
      if(transform == null) {
         return create(type);
      }
      return transform;
   }
   
   /**
    * This is used to create a <code>Transform</code> using the type
    * specified. If no transform can be acquired then this returns
    * a null value indicating that no transform could be found.
    * 
    * @param type this is the type to acquire the transform for
    * 
    * @return returns a transform for processing the type given
    */ 
   private Transform create(Class type) throws Exception {
      Class factory = types.fetch(type);
      
      if(factory != null) {
         return create(type, factory);
      }
      return null;
   }
   
   /**
    * This is used to create a <code>Transform</code> using the type
    * specified. If the transform can not be instantiated then this
    * will throw an exception. If it can then it is cached.
    * 
    * @param type this is the type to acquire the transform for
    * @param factory the class for instantiating the transform
    * 
    * @return returns a transform for processing the type given
    */ 
   private Transform create(Class type, Class factory) throws Exception {
      Object value = factory.newInstance();
      Transform transform = (Transform)value;
         
      if(transform != null) {
         transforms.cache(type, transform);
      }
      return transform;
   }
}
