/*
 * PrimitiveValue.java July 2007
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
 * The <code>PrimitiveValue</code> is used to serialize a primitive 
 * value to and from a node. If a value name is provided in the 
 * annotation then this will serialize and deserialize that value 
 * with the given name, if the value is primitive and no name is
 * specified then the value is written inline, that is without any
 * enclosing XML element.
 * <pre>
 * 
 *    &lt;entry key="one"&gt;example one&lt;/entry&gt;
 *    &lt;entry key="two"&gt;example two&lt;/entry&gt;
 *    &lt;entry key="three"&gt;example three&lt;/entry&gt;    
 * 
 * </pre>
 * Allowing the value to be written as either an XML element or an
 * inline text value enables a more flexible means for representing 
 * the value. The only condition for having an inline value is that
 * the key is specified as an attribute in the annotation.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.CompositeMap
 */
class PrimitiveValue implements Converter {
   
   /**
    * The primitive factory used to resolve the primitive to a string.
    */
   private final PrimitiveFactory factory;
   
   /**
    * This is the context used to support the serialization process.
    */
   private final Context context;
   
   /**
    * The primitive converter used to read the value from the node.
    */
   private final Primitive root;
   
   /**
    * This is the style used to style the XML names for the value.
    */
   private final Style style;
   
   /**
    * The entry object contains the details on how to write the value.
    */
   private final Entry entry; 
   
   /**
    * Represents the primitive type the value is serialized to and from.
    */
   private final Type type;
   
   /**
    * Constructor for the <code>PrimitiveValue</code> object. This is 
    * used to create the value object which converts the map value to 
    * an instance of the value type. This can also resolve references. 
    * 
    * @param context this is the context object used for serialization
    * @param entry this is the entry object that describes entries
    * @param type this is the type that this converter deals with
    */   
   public PrimitiveValue(Context context, Entry entry, Type type) {
      this.factory = new PrimitiveFactory(context, type);
      this.root = new Primitive(context, type);
      this.style = context.getStyle();
      this.context = context;
      this.entry = entry;
      this.type = type;
   }
   
   /**
    * This method is used to read the value value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value value can not be found according to the annotation
    * attributes then an exception is thrown.
    * 
    * @param node this is the node to read the value object from
    * 
    * @return this returns the value deserialized from the node
    */ 
   public Object read(InputNode node) throws Exception {
      Class expect = type.getType();
      String name = entry.getValue();
      
      if(!entry.isInline()) {
         if(name == null) {
            name = context.getName(expect);
         }
         return readElement(node, name);
      }
      return readAttribute(node, name);
   }
   
   /**
    * This method is used to read the value value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value value can not be found according to the annotation
    * attributes then an exception is thrown.
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
    * This method is used to read the element value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value value can not be found according to the annotation
    * attributes then null is assumed and returned.
    * 
    * @param node this is the node to read the value object from
    * @param key this is the name of the value XML element
    * 
    * @return this returns the value deserialized from the node
    */ 
   private Object readElement(InputNode node, String key) throws Exception {
      String name = style.getAttribute(key);
      InputNode child = node.getNext(name);
      
      if(child == null) {
         return null;        
      }
      return root.read(child);      
   }
   
   /**
    * This method is used to read the text value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value value can not be found according to the annotation
    * attributes then null is assumed and returned.
    * 
    * @param node this is the node to read the value object from
    * @param name this is the name of the value XML attribute, if any
    * 
    * @return this returns the value deserialized from the node
    */ 
   private Object readAttribute(InputNode node, String name) throws Exception {
      if(name != null) {
         name = style.getAttribute(name);
         node = node.getAttribute(name);
      }       
      if(node == null) {
         return null;        
      }
      return root.read(node);      
   }
   
   /**
    * This method is used to validate the value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value value can not be found according to the annotation
    * attributes then null is assumed and the node is valid.
    * 
    * @param node this is the node to read the value object from
    * 
    * @return this returns true if the primitive key is valid
    */ 
   public boolean validate(InputNode node) throws Exception {
      Class expect = type.getType();
      String name = entry.getValue();
      
      if(!entry.isInline()) {
         if(name == null) {
            name = context.getName(expect);
         }
         return validateElement(node, name);
      }
      return validateAttribute(node, name);
   }
   
   /**
    * This method is used to validate the value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value value can not be found according to the annotation
    * attributes then null is assumed and the node is valid.
    *  
    * @param node this is the node to read the value object from
    * @param key this is the name of the node to be validated
    * 
    * @return this returns true if the primitive key is valid
    */    
   private boolean validateElement(InputNode node, String key) throws Exception {
      String name = style.getAttribute(key);
      InputNode child = node.getNext(name);
      
      if(child == null) {
         return true;        
      }
      return root.validate(node);
   }
   
   /**
    * This method is used to validate the value from the node. The 
    * value read from the node is resolved using the template filter.
    * If the value value can not be found according to the annotation
    * attributes then null is assumed and the node is valid.
    *  
    * @param node this is the node to read the value object from
    * @param key this is the name of the node to be validated
    * 
    * @return this returns true if the primitive key is valid
    */    
   private boolean validateAttribute(InputNode node, String key) throws Exception {
      if(key != null) {
         key = style.getAttribute(key);
         node = node.getNext(key);
      }
      if(node == null) {
         return true;        
      }
      return root.validate(node);
   }

   /**
    * This method is used to write the value to the specified node.
    * The value written to the node can be an attribute or an element
    * depending on the annotation attribute values. This method will
    * maintain references for serialized elements.
    * 
    * @param node this is the node that the value is written to
    * @param item this is the item that is to be written
    */
   public void write(OutputNode node, Object item) throws Exception {
      Class expect = type.getType();
      String name = entry.getValue();
      
      if(!entry.isInline()) {
         if(name == null) {
            name = context.getName(expect);
         } 
         writeElement(node, item, name);
      } else {
         writeAttribute(node, item, name);
      }
   }
   
   /**
    * This method is used to write the value to the specified node.
    * The value written to the node can be an attribute or an element
    * depending on the annotation attribute values. This method will
    * maintain references for serialized elements.
    * 
    * @param node this is the node that the value is written to
    * @param item this is the item that is to be written
    * @param key this is the name of the element to be created
    */   
   private void writeElement(OutputNode node, Object item, String key) throws Exception {
      String name = style.getAttribute(key);
      OutputNode child = node.getChild(name);

      if(item != null) {        
         if(!isOverridden(child, item)) {
            root.write(child, item);
         }
      }
   }
   
   /**
    * This method is used to write the value to the specified node.
    * The value written to the node can be an attribute or an element
    * depending on the annotation attribute values. This method will
    * maintain references for serialized elements.
    * 
    * @param node this is the node that the value is written to
    * @param item this is the item that is to be written
    * @param key this is the name of the attribute to be created
    */   
   private void writeAttribute(OutputNode node, Object item, String key) throws Exception {
      if(item != null) {
         if(key != null) {
            key = style.getAttribute(key);
            node = node.setAttribute(key, null);
         }     
         root.write(node, item);
      }
   }
   
   /**
    * This is used to determine whether the specified value has been
    * overridden by the strategy. If the item has been overridden
    * then no more serialization is require for that value, this is
    * effectively telling the serialization process to stop writing.
    * 
    * @param node the node that a potential override is written to
    * @param value this is the object instance to be serialized
    * 
    * @return returns true if the strategy overrides the object
    */
   private boolean isOverridden(OutputNode node, Object value) throws Exception{
      return factory.setOverride(type, value, node);
   }

}
