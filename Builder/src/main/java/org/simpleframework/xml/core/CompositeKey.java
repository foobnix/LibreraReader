/*
 * CompositeKey.java July 2007
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

package org.simpleframework.xml.core;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.stream.Position;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>CompositeKey</code> object is used to convert an object
 * to an from an XML element. This accepts only composite objects and
 * will throw an exception if the <code>ElementMap</code> annotation
 * is configured to have an attribute key. If a key name is given for
 * the annotation then this will act as a parent element to the 
 * resulting XML element for the composite object. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.ElementMap
 */
class CompositeKey implements Converter {
   
   /**
    * This is the context used to support the serialization process.
    */
   private final Context context;
   
   /**
    * This is the traverser used to read and write the composite key.
    */
   private final Traverser root;
   
   /**
    * This is the style used to style the names used for the XML.
    */
   private final Style style;
   
   /**
    * This is the entry object used to provide configuration details.
    */
   private final Entry entry;
   
   /**
    * This represents the type of object the key is written as.
    */
   private final Type type;
      
   /**
    * Constructor for the <code>CompositeKey</code> object. This will
    * create an object capable of reading an writing composite keys
    * from an XML element. This also allows a parent element to be
    * created to wrap the key object if desired.
    * 
    * @param context this is the root context for the serialization
    * @param entry this is the entry object used for configuration
    * @param type this is the type of object the key represents
    */
   public CompositeKey(Context context, Entry entry, Type type) throws Exception {
      this.root = new Traverser(context);
      this.style = context.getStyle();
      this.context = context;
      this.entry = entry;
      this.type = type;
   }
   
   /**
    * This method is used to read the key value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the key value can not be found according to the annotation
    * attributes then null is assumed and returned.
    * 
    * @param node this is the node to read the key value from
    * 
    * @return this returns the value deserialized from the node
    */ 
   public Object read(InputNode node) throws Exception { 
      Position line = node.getPosition();
      Class expect = type.getType();
      String name = entry.getKey();
      
      if(name == null) {
         name = context.getName(expect);
      }
      if(entry.isAttribute()) {
         throw new AttributeException("Can not have %s as an attribute for %s at %s", expect, entry, line);
      }
      return read(node, name);
   }
   
   /**
    * This method is used to read the key value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the key value can not be found according to the annotation
    * attributes then null is assumed and returned.
    * 
    * @param node this is the node to read the key value from
    * @param value this is the value to deserialize in to
    * 
    * @return this returns the value deserialized from the node
    * 
    * @throws Exception if value is not null an exception is thrown
    */ 
   public Object read(InputNode node, Object value) throws Exception {
      Position line = node.getPosition();
      Class expect = type.getType();
      
      if(value != null) {
         throw new PersistenceException("Can not read key of %s for %s at %s", expect, entry, line);
      }
      return read(node);
   }
   
   /**
    * This method is used to read the key value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the key value can not be found according to the annotation
    * attributes then null is assumed and returned.
    * 
    * @param node this is the node to read the key value from
    * @param key this is the name of the key wrapper XML element
    * 
    * @return this returns the value deserialized from the node
    */ 
   private Object read(InputNode node, String key) throws Exception {
      String name = style.getElement(key);
      Class expect = type.getType();
      
      if(name != null) {
         node = node.getNext(name);
      }    
      if(node == null) {
         return null;
      }   
      if(node.isEmpty()) {
         return null;
      }
      return root.read(node, expect);
   }
   
   /**
    * This method is used to read the key value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the key value can not be found according to the annotation
    * attributes then null is assumed and the node is valid.
    * 
    * @param node this is the node to read the key value from
    * 
    * @return this returns the value deserialized from the node
    */ 
   public boolean validate(InputNode node) throws Exception { 
      Position line = node.getPosition();
      Class expect = type.getType();
      String name = entry.getKey();
      
      if(name == null) {
         name = context.getName(expect);
      }
      if(entry.isAttribute()) {
         throw new ElementException("Can not have %s as an attribute for %s at %s", expect, entry, line);
      }
      return validate(node, name);
   }
   
   /**
    * This method is used to read the key value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the key value can not be found according to the annotation
    * attributes then null is assumed and the node is valid.
    * 
    * @param node this is the node to read the key value from
    * @param key this is the name of the key wrapper XML element
    * 
    * @return this returns the value deserialized from the node
    */ 
   private boolean validate(InputNode node, String key) throws Exception {
      String name = style.getElement(key);
      InputNode next = node.getNext(name);
      Class expect = type.getType();
      
      if(next == null) {
         return true;
      }
      if(next.isEmpty()) {
         return true;
      }
      return root.validate(next, expect);
   }
   
   /**
    * This method is used to write the value to the specified node.
    * The value written to the node must be a composite object and if
    * the element map annotation is configured to have a key attribute
    * then this method will throw an exception.
    * 
    * @param node this is the node that the value is written to
    * @param item this is the item that is to be written
    */
   public void write(OutputNode node, Object item) throws Exception {
      Class expect = type.getType();
      String key = entry.getKey();
      
      if(entry.isAttribute()) {
         throw new ElementException("Can not have %s as an attribute for %s", expect, entry);
      }
      if(key == null) {
         key = context.getName(expect);
      }      
      String name = style.getElement(key);
      
      root.write(node, item, expect, name);      
   }
}
