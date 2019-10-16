/*
 * CompositeValue.java July 2007
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
import org.simpleframework.xml.stream.Style;

/**
 * The <code>CompositeValue</code> object is used to convert an object
 * to an from an XML element. This accepts only composite objects and
 * will maintain all references within the object using the cycle
 * strategy if required. This also ensures that should the value to
 * be written to the XML element be null that nothing is written. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.ElementMap
 */
class CompositeValue implements Converter {
   
   /**
    * This is the context used to support the serialization process.
    */
   private final Context context;
   
   /**
    * This is the traverser used to read and write the value with.
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
    * This represents the type of object the value is written as.
    */
   private final Type type;
   
   /**
    * Constructor for the <code>CompositeValue</code> object. This 
    * will create an object capable of reading an writing composite 
    * values from an XML element. This also allows a parent element 
    * to be created to wrap the key object if desired.
    * 
    * @param context this is the root context for the serialization
    * @param entry this is the entry object used for configuration
    * @param type this is the type of object the value represents
    */
   public CompositeValue(Context context, Entry entry, Type type) throws Exception {
      this.root = new Traverser(context);
      this.style = context.getStyle();
      this.context = context;
      this.entry = entry;
      this.type = type;
   }
   
   /**
    * This method is used to read the value object from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value data can not be found according to the annotation 
    * attributes then null is assumed and returned.
    * 
    * @param node this is the node to read the value object from
    * 
    * @return this returns the value deserialized from the node
    */ 
   public Object read(InputNode node) throws Exception { 
      InputNode next = node.getNext();
      Class expect = type.getType();
      
      if(next == null) {
         return null;
      }
      if(next.isEmpty()) {
         return null;
      }
      return root.read(next, expect);
   }
   
   /**
    * This method is used to read the value object from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value data can not be found according to the annotation 
    * attributes then null is assumed and returned.
    * 
    * @param node this is the node to read the value object from
    * @param value this is the value to deserialize in to
    * 
    * @return this returns the value deserialized from the node
    *
    * @throws Exception if value is not null an exception is thrown
    */ 
   public Object read(InputNode node, Object value) throws Exception { 
      Class expect = type.getType();
      
      if(value != null) {
         throw new PersistenceException("Can not read value of %s for %s", expect, entry);
      }
      return read(node);
   }
   
   /**
    * This method is used to read the value object from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value data can not be found according to the annotation 
    * attributes then null is assumed and the node is valid.
    * 
    * @param node this is the node to read the value object from
    * 
    * @return this returns true if this represents a valid value
    */ 
   public boolean validate(InputNode node) throws Exception { 
      Class expect = type.getType();
      String name = entry.getValue();
      
      if(name == null) {
         name = context.getName(expect);
      }
      return validate(node, name);
   }  
   
   /**
    * This method is used to read the value object from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value data can not be found according to the annotation 
    * attributes then null is assumed and the node is valid.
    * 
    * @param node this is the node to read the value object from
    * @param key this is the name of the value element
    * 
    * @return this returns true if this represents a valid value
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
    * the object provided to this is null then nothing is written.
    * 
    * @param node this is the node that the value is written to
    * @param item this is the item that is to be written
    */
   public void write(OutputNode node, Object item) throws Exception {
      Class expect = type.getType();
      String key = entry.getValue();
      
      if(key == null) {
         key = context.getName(expect);
      }
      String name = style.getElement(key);
      
      root.write(node, item, expect, name);      
   }
}
