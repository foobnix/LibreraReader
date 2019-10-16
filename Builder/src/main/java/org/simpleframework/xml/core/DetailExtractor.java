/*
 * DetailExtractor.java July 2006
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

package org.simpleframework.xml.core;

import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>DetailExtractor</code> object is used to extract details
 * for a specific class. All details extracted are cached so that 
 * they can be reused when requested several times. This provides an
 * increase in performance when there are large class hierarchies
 * as annotations does not need to be scanned a second time.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Detail
 */
class DetailExtractor {
   
   /**
    * This is the cache of methods for specific classes scanned.
    */
   private final Cache<ContactList> methods;
   
   /**
    * This is the cache of fields for specific classes scanned.
    */
   private final Cache<ContactList> fields;
   
   /**
    * This contains a cache of the details scanned for classes.
    */
   private final Cache<Detail> details;
   
   /**
    * This is an optional access type for the details created.
    */
   private final DefaultType override;
   
   /**
    * This contains various support functions for the details.
    */
   private final Support support;
   
   /**
    * Constructor for the <code>DetailExtractor</code> object. This
    * is used to extract various details for a class, such as the
    * method and field details as well as the annotations used on
    * the class. The primary purpose for this is to create cachable
    * values that reduce the amount of reflection required.
    * 
    * @param support this contains various support functions
    */
   public DetailExtractor(Support support) {
      this(support, null);
   }
   
   /**
    * Constructor for the <code>DetailExtractor</code> object. This
    * is used to extract various details for a class, such as the
    * method and field details as well as the annotations used on
    * the class. The primary purpose for this is to create cachable
    * values that reduce the amount of reflection required.
    * 
    * @param support this contains various support functions
    * @param override this is the override used for details created
    */
   public DetailExtractor(Support support, DefaultType override) {
      this.methods = new ConcurrentCache<ContactList>();
      this.fields = new ConcurrentCache<ContactList>();
      this.details = new ConcurrentCache<Detail>();
      this.override = override;
      this.support = support;
   }
   
   /**
    * This is used to get a <code>Detail</code> object describing a
    * class and its annotations. Any detail retrieved from this will 
    * be cached to increase the performance of future accesses.
    * 
    * @param type this is the type to acquire the detail for
    * 
    * @return an object describing the type and its annotations
    */
   public Detail getDetail(Class type) {
      Detail detail = details.fetch(type);
      
      if(detail == null) {
         detail = new DetailScanner(type, override);
         details.cache(type, detail);
      }
      return detail;
   }
   
   /**
    * This is used to acquire a list of <code>Contact</code> objects
    * that represent the annotated fields in a type. The entire
    * class hierarchy is scanned for annotated fields. Caching of
    * the contact list is done to increase performance.
    * 
    * @param type this is the type to scan for annotated fields
    * 
    * @return this returns a list of the annotated fields
    */
   public ContactList getFields(Class type) throws Exception {
      ContactList list = fields.fetch(type);
      
      if(list == null) {
         Detail detail = getDetail(type);
         
         if(detail != null) {
            list = getFields(type, detail);
         }
      }
      return list;
   }
   
   /**
    * This is used to acquire a list of <code>Contact</code> objects
    * that represent the annotated fields in a type. The entire
    * class hierarchy is scanned for annotated fields. Caching of
    * the contact list is done to increase performance.
    * 
    * @param detail this is the detail to scan for annotated fields
    * 
    * @return this returns a list of the annotated fields
    */
   private ContactList getFields(Class type, Detail detail) throws Exception {
      ContactList list = new FieldScanner(detail, support);
      
      if(detail != null) {
         fields.cache(type, list);
      }
      return list;
   }
   
   /**
    * This is used to acquire a list of <code>Contact</code> objects
    * that represent the annotated methods in a type. The entire
    * class hierarchy is scanned for annotated methods. Caching of
    * the contact list is done to increase performance.
    * 
    * @param type this is the type to scan for annotated methods
    * 
    * @return this returns a list of the annotated methods
    */
   public ContactList getMethods(Class type) throws Exception {
      ContactList list = methods.fetch(type);
      
      if(list == null) {
         Detail detail = getDetail(type);
         
         if(detail != null) {
            list = getMethods(type, detail);
         }
      }
      return list;
   }
   
   /**
    * This is used to acquire a list of <code>Contact</code> objects
    * that represent the annotated methods in a type. The entire
    * class hierarchy is scanned for annotated methods. Caching of
    * the contact list is done to increase performance.
    * 
    * @param type this is the type to scan for annotated methods
    * @param detail this is the type to scan for annotated methods
    * 
    * @return this returns a list of the annotated methods
    */
   private ContactList getMethods(Class type, Detail detail) throws Exception {
      ContactList list = new MethodScanner(detail, support);
      
      if(detail != null) {
         methods.cache(type, list);
      }
      return list;
   }
}
