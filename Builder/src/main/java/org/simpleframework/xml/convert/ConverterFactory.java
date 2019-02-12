/*
 * ConverterFactory.java January 2010
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

import java.lang.reflect.Constructor;

import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>ConverterFactory</code> is used to instantiate objects
 * based on a provided type or annotation. This provides a single
 * point of creation for all converters within the framework. For
 * performance all the instantiated converters are cached against
 * the class for that converter. This ensures the converters can
 * be acquired without the overhead of instantiation.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.convert.ConverterCache
 */
class ConverterFactory {
   
   /**
    * This is the cache that is used to cache converter instances.
    */
   private final Cache<Converter> cache; 
   
   /**
    * Constructor for the <code>ConverterFactory</code> object. 
    * This will create an internal cache which is used to cache all
    * instantiations made by the factory. Caching the converters
    * ensures there is no overhead with instantiations.
    */
   public ConverterFactory() {
      this.cache = new ConcurrentCache<Converter>();
   }
   
   /**
    * This is used to instantiate the converter based on the type
    * provided. If the type provided can not be instantiated for
    * some reason then an exception is thrown from this method.
    * 
    * @param type this is the converter type to be instantiated
    * 
    * @return this returns an instance of the provided type
    */
   public Converter getInstance(Class type) throws Exception {
      Converter converter = cache.fetch(type);
      
      if(converter == null) {
         return getConverter(type);
      }
      return converter;
   }
   
   /**
    * This is used to instantiate the converter based on the type
    * of the <code>Convert</code> annotation provided. If the type 
    * can not be instantiated for some reason then an exception is 
    * thrown from this method.
    * 
    * @param convert this is the annotation containing the type
    * 
    * @return this returns an instance of the provided type
    */
   public Converter getInstance(Convert convert) throws Exception {
      Class type = convert.value();
      
      if(type.isInterface()) {
         throw new ConvertException("Can not instantiate %s", type);
      }
      return getInstance(type);
   }
   
   /**
    * This is used to instantiate the converter based on the type
    * provided. If the type provided can not be instantiated for
    * some reason then an exception is thrown from this method.
    * 
    * @param type this is the converter type to be instantiated
    * 
    * @return this returns an instance of the provided type
    */
   private Converter getConverter(Class type) throws Exception {
      Constructor factory = getConstructor(type);
      
      if(factory == null){
         throw new ConvertException("No default constructor for %s", type);
      }
      return getConverter(type, factory);
   }
   
   /**
    * This is used to instantiate the converter based on the type
    * provided. If the type provided can not be instantiated for
    * some reason then an exception is thrown from this method.
    * 
    * @param type this is the converter type to be instantiated
    * @param factory this is the constructor used to instantiate
    * 
    * @return this returns an instance of the provided type
    */
   private Converter getConverter(Class type, Constructor factory) throws Exception {
      Converter converter = (Converter)factory.newInstance();
      
      if(converter != null){
         cache.cache(type, converter);
      }
      return converter;
   }
   
   /**
    * This is used to acquire the default no argument constructor
    * for the the provided type. If the constructor is not accessible
    * then it will be made accessible so that it can be instantiated.
    * 
    * @param type this is the type to acquire the constructor for
    * 
    * @return this returns the constructor for the type provided
    */
   private Constructor getConstructor(Class type) throws Exception {
      Constructor factory = type.getDeclaredConstructor();
      
      if(!factory.isAccessible()) {
         factory.setAccessible(true);
      }
      return factory;
   }
}
