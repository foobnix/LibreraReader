/*
 * WeakCache.java July 2006
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * The <code>WeakCache</code> object is an implementation of a cache
 * that holds on to cached items only if the key remains in memory.
 * This is effectively like a concurrent hash map with weak keys, it
 * ensures that multiple threads can concurrently access weak hash
 * maps in a way that lowers contention for the locks used. 
 * 
 * @author Niall Gallagher
 */
public class WeakCache<T> implements Cache<T> {
   
   /**
    * This is used to store a list of segments for the cache.
    */
   private SegmentList list;
   
   /**
    * Constructor for the <code>WeakCache</code> object. This is
    * used to create a cache that stores values in such a way that
    * when the key is garbage collected the value is removed from
    * the map. This is similar to the concurrent hash map.
    */
   public WeakCache() {
      this(10);
   }
   
   /**
    * Constructor for the <code>WeakCache</code> object. This is
    * used to create a cache that stores values in such a way that
    * when the key is garbage collected the value is removed from
    * the map. This is similar to the concurrent hash map.
    * 
    * @param size this is the number of segments within the cache
    */
   public WeakCache(int size) {      
      this.list = new SegmentList(size);
   }
   
   public boolean isEmpty() {
      for(Segment segment : list) {
         if(!segment.isEmpty()) {
            return false;
         }
      }
      return true;
   }
   
   
   /**
    * This method is used to insert a key value mapping in to the
    * cache. The value can later be retrieved or removed from the
    * cache if desired. If the value associated with the key is 
    * null then nothing is stored within the cache.
    * 
    * @param key this is the key to cache the provided value to
    * @param value this is the value that is to be cached
    */
   public void cache(Object key, T value) {
      map(key).cache(key, value);
   }
   
   /**
    * This is used to exclusively take the value mapped to the 
    * specified key from the cache. Invoking this is effectively
    * removing the value from the cache.
    * 
    * @param key this is the key to acquire the cache value with
    * 
    * @return this returns the value mapped to the specified key 
    */
   public T take(Object key) {
      return map(key).take(key);
   }
   
   /**
    * This method is used to get the value from the cache that is
    * mapped to the specified key. If there is no value mapped to
    * the specified key then this method will return a null.
    * 
    * @param key this is the key to acquire the cache value with
    * 
    * @return this returns the value mapped to the specified key 
    */
   public T fetch(Object key) {
      return map(key).fetch(key);
   }
   
   /**
    * This is used to determine whether the specified key exists
    * with in the cache. Typically this can be done using the 
    * fetch method, which will acquire the object. 
    * 
    * @param key this is the key to check within this segment
    * 
    * @return true if the specified key is within the cache
    */
   public boolean contains(Object key) {
      return map(key).contains(key);
   }
   
   /**
    * This method is used to acquire a <code>Segment</code> using
    * the keys has code. This method effectively uses the hash to
    * find a specific segment within the fixed list of segments.
    * 
    * @param key this is the key used to acquire the segment
    * 
    * @return this returns the segment used to get acquire value
    */
   private Segment map(Object key) {
      return list.get(key);
   }   
   
   /**
    * This is used to maintain a list of segments. All segments that
    * are stored by this object can be acquired using a given key.
    * The keys hash is used to select the segment, this ensures that
    * all read and write operations with the same key result in the
    * same segment object within this list.
    * 
    * @author Niall Gallagher
    */
   private class SegmentList implements Iterable<Segment> {
      
      /**
       * The list of segment objects maintained by this object.
       */
      private List<Segment> list;
      
      /**
       * Represents the number of segments this object maintains.
       */
      private int size;
      
      /**
       * Constructor for the <code>SegmentList</code> object. This
       * is used to create a list of weak hash maps that can be
       * acquired using the hash code of a given key. 
       * 
       * @param size this is the number of hash maps to maintain
       */
      public SegmentList(int size) {
         this.list = new ArrayList<Segment>();
         this.size = size;
         this.create(size);         
      }
      
      public Iterator<Segment> iterator() {
         return list.iterator();
      }
      
      /**
       * This is used to acquire the segment using the given key.
       * The keys hash is used to determine the index within the 
       * list to acquire the segment, which is a synchronized weak        
       * hash map storing the key value pairs for a given hash.
       * 
       * @param key this is the key used to determine the segment
       * 
       * @return the segment that is stored at the resolved hash 
       */
      public Segment get(Object key) {
         int segment = segment(key);
         
         if(segment < size) {
            return list.get(segment);
         }
         return null;
      }
      
      /**
       * Upon initialization the segment list is populated in such
       * a way that synchronization is not needed. Each segment is
       * created and stored in an increasing index within the list.
       * 
       * @param size this is the number of segments to be used
       */
      private void create(int size) {
         int count = size;
         
         while(count-- > 0) {
            list.add(new Segment());
         }
      }
      
      /**
       * This method performs the translation of the key hash code
       * to the segment index within the list. Translation is done
       * by acquiring the modulus of the hash and the list size.
       * 
       * @param key this is the key used to resolve the index
       * 
       * @return the index of the segment within the list 
       */
      private int segment(Object key) {
         return Math.abs(key.hashCode() % size);
      }
   }
   
   /**
    * The segment is effectively a synchronized weak hash map. If is
    * used to store the key value pairs in such a way that they are
    * kept only as long as the garbage collector does not collect 
    * the key. This ensures the cache does not cause memory issues. 
    * 
    * @author Niall Gallagher
    */
   private class Segment extends WeakHashMap<Object, T> {      
      
      /**
       * This method is used to insert a key value mapping in to the
       * cache. The value can later be retrieved or removed from the
       * cache if desired. If the value associated with the key is 
       * null then nothing is stored within the cache.
       * 
       * @param key this is the key to cache the provided value to
       * @param value this is the value that is to be cached
       */
      public synchronized void cache(Object key, T value) {
         put(key, value);
      }
      
      /**
       * This method is used to get the value from the cache that is
       * mapped to the specified key. If there is no value mapped to
       * the specified key then this method will return a null.
       * 
       * @param key this is the key to acquire the cache value with
       * 
       * @return this returns the value mapped to the specified key 
       */
      public synchronized T fetch(Object key) {
         return get(key);
      }
      
      /**
       * This is used to exclusively take the value mapped to the 
       * specified key from the cache. Invoking this is effectively
       * removing the value from the cache.
       * 
       * @param key this is the key to acquire the cache value with
       * 
       * @return this returns the value mapped to the specified key 
       */
      public synchronized T take(Object key) {
         return remove(key);
      }
 
      /**
       * This is used to determine whether the specified key exists
       * with in the cache. Typically this can be done using the 
       * fetch method, which will acquire the object. 
       * 
       * @param key this is the key to check within this segment
       * 
       * @return true if the specified key is within the cache
       */
      public synchronized boolean contains(Object key) {
         return containsKey(key);
      }
   }
}
