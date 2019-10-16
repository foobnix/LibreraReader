/*
 * CompositeMap.java July 2007
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

import java.util.Map;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>CompositeMap</code> is used to serialize and deserialize
 * maps to and from a source XML document. The structure of the map in
 * the XML format is determined by the annotation. Keys can be either
 * attributes or elements, and values can be inline. This can perform
 * serialization and deserialization of the key and value objects 
 * whether the object types are primitive or composite.
 * <pre>
 * 
 *    &lt;map&gt;
 *       &lt;entry key='1'&gt;           
 *          &lt;value&gt;one&lt;/value&gt;
 *       &lt;/entry&gt;
 *       &lt;entry key='2'&gt;
 *          &lt;value&gt;two&lt;/value&gt;
 *       &lt;/entry&gt;      
 *    &lt;/map&gt;
 *    
 * </pre>
 * For the above XML element map the element <code>entry</code> is 
 * used to wrap the key and value such that they can be grouped. This
 * element does not represent any real object. The names of each of
 * the XML elements serialized and deserialized can be configured.
 *  
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Entry
 */
class CompositeMap implements Converter {
      
   /**
    * The factory used to create suitable map object instances.
    */
   private final MapFactory factory;
   
   /**
    * This is the type that the value objects are instances of. 
    */
   private final Converter value;
   
   /**
    * This is the name of the entry wrapping the key and value.
    */
   private final Converter key;  
   
   /**
    * This is the style used to style the names used for the XML.
    */
   private final Style style;
   
   /**
    * The entry object contains the details on how to write the map.
    */
   private final Entry entry;
   
   /**
    * Constructor for the <code>CompositeMap</code> object. This will
    * create a converter that is capable of writing map objects to 
    * and from XML. The resulting XML is configured by an annotation
    * such that key values can attributes and values can be inline. 
    * 
    * @param context this is the root context for the serialization
    * @param entry this provides configuration for the resulting XML
    * @param type this is the map type that is to be converted
    */
   public CompositeMap(Context context, Entry entry, Type type) throws Exception {
      this.factory = new MapFactory(context, type);
      this.value = entry.getValue(context);
      this.key = entry.getKey(context);
      this.style = context.getStyle();
      this.entry = entry;
   }

   /**
    * This <code>read</code> method will read the XML element map from
    * the provided node and deserialize its children as entry types.
    * Each entry type must contain a key and value so that the entry 
    * can be inserted in to the map as a pair. If either the key or 
    * value is composite it is read as a root object, which means its
    * <code>Root</code> annotation must be present and the name of the
    * object element must match that root element name.
    * 
    * @param node this is the XML element that is to be deserialized
    * 
    * @return this returns the item to attach to the object contact
    */
   public Object read(InputNode node) throws Exception{
      Instance type = factory.getInstance(node);
      Object map = type.getInstance();
      
      if(!type.isReference()) {
         return populate(node, map);
      }
      return map;
   }
   
   /**
    * This <code>read</code> method will read the XML element map from
    * the provided node and deserialize its children as entry types.
    * Each entry type must contain a key and value so that the entry 
    * can be inserted in to the map as a pair. If either the key or 
    * value is composite it is read as a root object, which means its
    * <code>Root</code> annotation must be present and the name of the
    * object element must match that root element name.
    * 
    * @param node this is the XML element that is to be deserialized
    * @param result this is the map object that is to be populated
    * 
    * @return this returns the item to attach to the object contact
    */
   public Object read(InputNode node, Object result) throws Exception {
      Instance type = factory.getInstance(node);
      
      if(type.isReference()) {
         return type.getInstance();
      }
      type.setInstance(result);
      
      if(result != null) {
         return populate(node, result);
      }
      return result;
   }
   
   /**
    * This <code>populate</code> method will read the XML element map 
    * from the provided node and deserialize its children as entry types.
    * Each entry type must contain a key and value so that the entry 
    * can be inserted in to the map as a pair. If either the key or 
    * value is composite it is read as a root object, which means its
    * <code>Root</code> annotation must be present and the name of the
    * object element must match that root element name.
    * 
    * @param node this is the XML element that is to be deserialized
    * @param result this is the map object that is to be populated
    * 
    * @return this returns the item to attach to the object contact
    */
   private Object populate(InputNode node, Object result) throws Exception {
      Map map = (Map) result;                 
      
      while(true) {
         InputNode next = node.getNext();
        
         if(next == null) {
            return map;
         }
         Object index = key.read(next);
         Object item = value.read(next);
            
         map.put(index, item); 
      }
   }
   
   /**
    * This <code>validate</code> method will validate the XML element 
    * map from the provided node and validate its children as entry 
    * types. Each entry type must contain a key and value so that the 
    * entry can be inserted in to the map as a pair. If either the key 
    * or value is composite it is read as a root object, which means its
    * <code>Root</code> annotation must be present and the name of the
    * object element must match that root element name.
    * 
    * @param node this is the XML element that is to be validate
    * 
    * @return true if the element matches the XML schema class given 
    */
   public boolean validate(InputNode node) throws Exception{
      Instance value = factory.getInstance(node);
      
      if(!value.isReference()) {
         Object result = value.setInstance(null);
         Class type = value.getType();
            
         return validate(node, type);
      }
      return true; 
   }
   
   /**
    * This <code>validate</code> method will validate the XML element 
    * map from the provided node and validate its children as entry 
    * types. Each entry type must contain a key and value so that the 
    * entry can be inserted in to the map as a pair. If either the key 
    * or value is composite it is read as a root object, which means its
    * <code>Root</code> annotation must be present and the name of the
    * object element must match that root element name.
    * 
    * @param node this is the XML element that is to be validate
    * @param type this is the type to validate the input node against
    * 
    * @return true if the element matches the XML schema class given 
    */
   private boolean validate(InputNode node, Class type) throws Exception {
      while(true) {
         InputNode next = node.getNext();
        
         if(next == null) {
            return true;
         }
         if(!key.validate(next)) {
            return false;
         }
         if(!value.validate(next)) {
            return false;
         }                     
      }
   }

   /**
    * This <code>write</code> method will write the key value pairs
    * within the provided map to the specified XML node. This will 
    * write each entry type must contain a key and value so that
    * the entry can be deserialized in to the map as a pair. If the
    * key or value object is composite it is read as a root object 
    * so its <code>Root</code> annotation must be present.
    * 
    * @param node this is the node the map is to be written to
    * @param source this is the source map that is to be written 
    */
   public void write(OutputNode node, Object source) throws Exception {
      Map map = (Map) source;                
      
      for(Object index : map.keySet()) {
         String root = entry.getEntry();
         String name = style.getElement(root);
         OutputNode next = node.getChild(name);
         Object item = map.get(index);            
         
         key.write(next, index);            
         value.write(next, item);                  
      }
   }
}
