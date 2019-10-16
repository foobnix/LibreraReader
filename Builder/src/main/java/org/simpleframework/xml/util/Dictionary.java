/*
 * Dictionary.java July 2006
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

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The <code>Dictionary</code> object represents a mapped set of entry
 * objects that can be serialized and deserialized. This is used when
 * there is a need to load a list of objects that can be mapped using 
 * a name attribute. Using this object avoids the need to implement a
 * commonly required pattern of building a map of XML element objects.
 * <pre>
 *
 *    &lt;dictionary&gt;
 *       &lt;entry name="example"&gt;
 *          &lt;element&gt;example text&lt;/element&gt;
 *       &lt;/entry&gt;
 *       &lt;entry name="example"&gt;
 *          &lt;element&gt;example text&lt;/element&gt;
 *       &lt;/entry&gt;       
 *    &lt;/dictionary&gt;
 * 
 * </pre>
 * This can contain implementations of the <code>Entry</code> object 
 * which contains a required "name" attribute. Implementations of the
 * entry object can add further XML attributes an elements. This must
 * be annotated with the <code>ElementList</code> annotation in order
 * to be serialized and deserialized as an object field.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.util.Entry
 */ 
public class Dictionary<T extends Entry> extends AbstractSet<T> {

   /**
    * Used to map the entries to their configured names.
    */         
   protected final Table<T> map;
        
   /**
    * Constructor for the <code>Dictionary</code> object. This 
    * is used to create a set that contains entry objects mapped 
    * to an XML attribute name value. Entry objects added to this
    * dictionary can be retrieved using its name value.
    */ 
   public Dictionary() {
      this.map = new Table<T>();           
   }

   /**
    * This method is used to add the provided entry to this set. If
    * an entry of the same name already existed within the set then
    * it is replaced with the specified <code>Entry</code> object.
    * 
    * @param item this is the entry object that is to be inserted
    */ 
   public boolean add(T item) {
      return map.put(item.getName(), item) != null;           
   }

   /**
    * This returns the number of <code>Entry</code> objects within
    * the dictionary. This will use the internal map to acquire the
    * number of entry objects that have been inserted to the map.
    *
    * @return this returns the number of entry objects in the set
    */ 
   public int size() {
      return map.size();            
   }

   /**
    * Returns an iterator of <code>Entry</code> objects which can be
    * used to remove items from this set. This will use the internal
    * map object and return the iterator for the map values.
    * 
    * @return this returns an iterator for the entry objects
    */ 
   public Iterator<T> iterator() {
      return map.values().iterator();            
   }

   /**
    * This is used to acquire an <code>Entry</code> from the set by
    * its name. This uses the internal map to look for the entry, if
    * the entry exists it is returned, if not this returns null.
    * 
    * @param name this is the name of the entry object to retrieve
    *
    * @return this returns the entry mapped to the specified name
    */ 
   public T get(String name) {
      return map.get(name);           
   }

   /**
    * This is used to remove an <code>Entry</code> from the set by
    * its name. This uses the internal map to look for the entry, if
    * the entry exists it is returned and removed from the map.
    * 
    * @param name this is the name of the entry object to remove
    *
    * @return this returns the entry mapped to the specified name
    */ 
   public T remove(String name) {
      return map.remove(name);           
   }
 
   /**
    * The <code>Table</code> object is used to represent a map of
    * entries mapped to a string name. Each implementation of the
    * entry must contain a name attribute, which is used to insert
    * the entry into the map. This acts as a typedef.
    *
    * @see org.simpleframework.xml.util.Entry
    */
   private static class Table<T> extends HashMap<String, T> {
      
      /**
       * Constructor for the <code>Table</code> object. This will
       * create a map that is used to store the entry objects that
       * are serialized and deserialized to and from an XML source.
       */
      public Table() {
         super();
      }         
   }     
}
