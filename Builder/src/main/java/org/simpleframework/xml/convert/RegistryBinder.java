/*
 * RegistryBinder.java January 2010
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
 * The <code>RegistryBinder</code> object is used acquire converters
 * using a binding between a type and its converter. All converters
 * instantiated are cached internally to ensure that the overhead
 * of acquiring a converter is reduced. Converters are created on
 * demand to ensure they are instantiated only if required.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.convert.Registry
 */
class RegistryBinder {
   
   /**
    * This is used to instantiate and cache the converter objects.
    */
   private final ConverterFactory factory;
   
   /**
    * This is used to cache bindings between types and converters.
    */
   private final Cache<Class> cache;
   
   /**
    * Constructor for the <code>RegistryBinder</code> object. This 
    * is used to create bindings between classes and the converters 
    * that should be used to serialize and deserialize the instances. 
    * All converters are instantiated once and cached for reuse.
    */
   public RegistryBinder() {
      this.cache = new ConcurrentCache<Class>();
      this.factory = new ConverterFactory();
   }
   
   /**
    * This is used to acquire a <code>Converter</code> instance from
    * this binder. All instances are cached to reduce the overhead
    * of lookups during the serialization process. Converters are
    * lazily instantiated and so are only created if demanded.
    * 
    * @param type this is the type to find the converter for
    * 
    * @return this returns the converter instance for the type
    */
   public Converter lookup(Class type) throws Exception {
      Class result = cache.fetch(type);
      
      if(result != null) {
         return create(result);
      }
      return null;
   }
   
   /**
    * This is used to acquire a <code>Converter</code> instance from
    * this binder. All instances are cached to reduce the overhead
    * of lookups during the serialization process. Converters are
    * lazily instantiated and so are only created if demanded.
    * 
    * @param type this is the type to find the converter for
    * 
    * @return this returns the converter instance for the type
    */
   private Converter create(Class type) throws Exception {
      return factory.getInstance(type);
   }
   
   /**
    * This is used to register a binding between a type and the
    * converter used to serialize and deserialize it. During the
    * serialization process the converters are retrieved and 
    * used to convert the object properties to XML.
    * 
    * @param type this is the object type to bind to a converter
    * @param converter this is the converter class to be used
    */
   public void bind(Class type, Class converter) throws Exception {
      cache.cache(type, converter);
   }
}
