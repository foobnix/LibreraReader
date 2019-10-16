/*
 * KeyBuilder.java April 2012
 *
 * Copyright (C) 2012, Niall Gallagher <niallg@users.sf.net>
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

import java.util.Arrays;

/**
 * The <code>KeyBuilder</code> is used build unique keys for labels
 * using the paths defined in those labels. Building keys in this
 * way ensures that labels can be uniquely identified. This helps in
 * constructor injection when we need to match a parameter with a
 * label to determine which constructor signature should be used.
 * 
 * @author Niall Gallagher
 */
class KeyBuilder {
   
   /**
    * This is the label that the keys should be built with.
    */
   private final Label label;
   
   /**
    * Constructor for the <code>KeyBuilder</code> object. This is
    * used to create a builder using the provided label. A key will 
    * be unique based on the XPath options for the label. 
    * 
    * @param label this is the label to build he key for
    */
   public KeyBuilder(Label label) {
      this.label = label;
   }

   /**
    * This generates a key for the <code>Label</code> object that
    * this represents. Each key is generated based on the label type
    * and the XPath options for the label. This ensures that similar
    * labels create the same key values.
    * 
    * @return this returns a key to represent the label
    */
   public Object getKey() throws Exception {
      if(label.isAttribute()) {
         return getKey(KeyType.ATTRIBUTE);
      }
      return getKey(KeyType.ELEMENT);
   }
   
   /**
    * This generates a key for the <code>Label</code> object that
    * this represents. Each key is generated based on the label type
    * and the XPath options for the label. This ensures that similar
    * labels create the same key values.
    * 
    * @param type this is the type that this key builder represents
    * 
    * @return this returns a key to represent the label
    */
   private Object getKey(KeyType type) throws Exception {
      String[] list = label.getPaths();
      String text = getKey(list);
      
      if(type == null) {
         return text;
      }
      return new Key(type, text);
   }
   
   /**
    * This generates a key for the <code>Label</code> object that
    * this represents. Each key is generated based on the label type
    * and the XPath options for the label. This ensures that similar
    * labels create the same key values.
    * 
    * @param list this is the list of XPath expressions to be used
    * 
    * @return this returns a key to represent the label
    */
   private String getKey(String[] list) throws Exception { 
      StringBuilder builder = new StringBuilder();
      
      if(list.length > 0) {
         Arrays.sort(list);
         
         for(String path : list) {
            builder.append(path);
            builder.append('>');
         }
      }
      return builder.toString();
   }
   
   /**
    * The <code>KeyType</code> enumeration is used to differentiate
    * keys created for attributes from those created from elements.
    * This is important when determining if XML paths would clash.
    */
   private static enum KeyType {
      TEXT,
      ATTRIBUTE,
      ELEMENT;
   }
   
   /**
    * The <code>Key</code> object represents an object that can be
    * used in a hash container. The <code>hashCode</code> and the 
    * <code>equals</code> method will ensure that a label with the
    * same XPath options will hash to the same position.
    */
   private static class Key {
      
      /**
       * This is the type of key that this represents.
       */
      private final KeyType type;
      
      /**
       * This is the value that is used to provide the hash code.
       */
      private final String value;
      
      /**
       * Constructor for the <code>Key</code> object. This requires
       * the label and a key type to create a unique key. The key
       * type allows keys based on attributes to be differentiated
       * from those created for elements.
       * 
       * @param type this is the type that the key is created for
       * @param value this is the value used for the hash code
       */
      public Key(KeyType type, String value) throws Exception {
         this.value = value;
         this.type = type;
      }
      
      /**
       * This is used to compare keys and determine equality. If the
       * key value is the same and the key type is the same then the
       * key is considered equal, even if the labels are different.
       * 
       * @param value this is the value to compared to this
       * 
       * @return this returns true if the object is equal
       */
      public boolean equals(Object value) {
         if(value instanceof Key) {
            Key key = (Key)value;
            return equals(key);
         }
         return false;
      }
      
      /**
       * This is used to compare keys and determine equality. If the
       * key value is the same and the key type is the same then the
       * key is considered equal, even if the labels are different.
       * 
       * @param key this is the value to compared to this
       * 
       * @return this returns true if the object is equal
       */
      public boolean equals(Key key) {
         if(type == key.type) {
            return key.value.equals(value);
         }
         return false;
      }
      
      /**
       * This returns the hash code for the key. The hash code is
       * generated from the internal string the key represents. 
       * 
       * @return this is the hash code generated from the value
       */
      public int hashCode() {
         return value.hashCode();
      }
      
      /**
       * This returns the string representation of the key. This is
       * used for debugging purposes in order to determine what the
       * key was generated from.
       * 
       * @return this returns the string representation of the key
       */
      public String toString() {
         return value;
      }
   }
}
