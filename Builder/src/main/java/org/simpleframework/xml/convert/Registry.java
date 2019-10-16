/*
 * Registry.java January 2010
 *
 * Copyright (C) 2010, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.xml.convert;

import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>Registry</code> represents an object that is used to
 * register bindings between a class and a converter implementation.
 * Converter instances created by this registry are lazily created
 * and cached so that they are instantiated only once. This ensures 
 * that the overhead of serialization is reduced. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.convert.RegistryStrategy
 */
public class Registry {
   
   /**
    * This is used to cache the converters based on object types.
    */
   private final Cache<Converter> cache;
   
   /**
    * This is used to bind converter types to serializable types.
    */
   private final RegistryBinder binder;
   
   /**
    * Constructor for the <code>Registry</code> object. This is used
    * to create a registry between classes and the converters that
    * should be used to serialize and deserialize the instances. All
    * converters are instantiated once and cached for reuse.
    */
   public Registry() {
      this.cache = new ConcurrentCache<Converter>();
      this.binder = new RegistryBinder();
   }
   
   /**
    * This is used to acquire a <code>Converter</code> instance from
    * the registry. All instances are cache to reduce the overhead
    * of lookups during the serialization process. Converters are
    * lazily instantiated and so are only created if demanded.
    * 
    * @param type this is the type to find the converter for
    * 
    * @return this returns the converter instance for the type
    */
   public Converter lookup(Class type) throws Exception {
      Converter converter = cache.fetch(type);
      
      if(converter == null) {
         return create(type);
      }
      return converter;
   }
   
   /**
    * This is used to acquire a <code>Converter</code> instance from
    * the registry. All instances are cached to reduce the overhead
    * of lookups during the serialization process. Converters are
    * lazily instantiated and so are only created if demanded.
    * 
    * @param type this is the type to find the converter for
    * 
    * @return this returns the converter instance for the type
    */
   private Converter create(Class type) throws Exception {
      Converter converter = binder.lookup(type);
      
      if(converter != null) {
         cache.cache(type, converter);
      }
      return converter;
   }
   
   /**
    * This is used to register a binding between a type and the
    * converter used to serialize and deserialize it. During the
    * serialization process the converters are retrieved and 
    * used to convert the object members to XML.
    * 
    * @param type this is the object type to bind to a converter
    * @param converter this is the converter class to be used
    * 
    * @return this will return this registry instance to use
    */
   public Registry bind(Class type, Class converter) throws Exception {
      if(type != null) {
         binder.bind(type, converter);
      }
      return this;
   }
   
   /**
    * This is used to register a binding between a type and the
    * converter used to serialize and deserialize it. During the
    * serialization process the converters are retrieved and 
    * used to convert the object properties to XML.
    * 
    * @param type this is the object type to bind to a converter
    * @param converter this is the converter instance to be used
    * 
    * @return this will return this registry instance to use
    */
   public Registry bind(Class type, Converter converter) throws Exception {
      if(type != null) {
         cache.cache(type, converter);
      }
      return this;
   }
}
