/*
 * Cache.java July 2006
 *
 * Copyright (C) 2006, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.xml.util;

/**
 * The <code>Cache</code> interface is used to represent a cache
 * that will store key value pairs. The cache exposes only several
 * methods to ensure that implementations can focus on performance
 * concerns rather than how to manage the cached values.
 * 
 * @author Niall Gallagher
 */
public interface Cache<T> {
   
   /**
    * This method is used to determine if the cache is empty. This
    * is done by checking if there are any elements in the cache.
    * If anything has been cached this will return false.
    * 
    * @return this returns true if the cache is empty
    */
   boolean isEmpty();
   
   /**
    * This method is used to insert a key value mapping in to the
    * cache. The value can later be retrieved or removed from the
    * cache if desired. If the value associated with the key is 
    * null then nothing is stored within the cache.
    * 
    * @param key this is the key to cache the provided value to
    * @param value this is the value that is to be cached
    */
   void cache(Object key, T value);
 
   /**
    * This is used to exclusively take the value mapped to the 
    * specified key from the cache. Invoking this is effectively
    * removing the value from the cache.
    * 
    * @param key this is the key to acquire the cache value with
    * 
    * @return this returns the value mapped to the specified key 
    */
   T take(Object key);
   
   /**
    * This method is used to get the value from the cache that is
    * mapped to the specified key. If there is no value mapped to
    * the specified key then this method will return a null.
    * 
    * @param key this is the key to acquire the cache value with
    * 
    * @return this returns the value mapped to the specified key 
    */
   T fetch(Object key);   
   
   /**
    * This is used to determine whether the specified key exists
    * with in the cache. Typically this can be done using the 
    * fetch method, which will acquire the object. 
    * 
    * @param key this is the key to check within this segment
    * 
    * @return true if the specified key is within the cache
    */
   boolean contains(Object key);
}