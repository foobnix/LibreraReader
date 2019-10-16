/*
 * ReadGraph.java April 2007
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

package org.simpleframework.xml.strategy;

import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.Node;

import java.util.HashMap;

/**
 * The <code>ReadGraph</code> object is used to build a graph of the
 * objects that have been deserialized from the XML document. This is
 * required so that cycles in the object graph can be recreated such
 * that the deserialized object is an exact duplicate of the object
 * that was serialized. Objects are stored in the graph using unique
 * keys, which for this implementation are unique strings.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.strategy.WriteGraph
 */
class ReadGraph extends HashMap {
   
   /**
    * This is the class loader that is used to load the types used.
    */
   private final Loader loader;
   
   /**
    * This is used to represent the length of array object values.
    */
   private final String length;
   
   /**
    * This is the label used to mark the type of an object.
    */
   private final String label;
   
   /**
    * This is the attribute used to mark the identity of an object.
    */
   private final String mark;
   
   /**
    * This is the attribute used to refer to an existing instance.
    */
   private final String refer;
   
   /**
    * Constructor for the <code>ReadGraph</code> object. This is used
    * to create graphs that are used for reading objects from the XML
    * document. The specified strategy is used to acquire the names
    * of the special attributes used during the serialization.
    * 
    * @param contract this is the name scheme used by the strategy
    * @param loader this is the class loader to used for the graph 
    */
   public ReadGraph(Contract contract, Loader loader) {      
      this.refer = contract.getReference();
      this.mark = contract.getIdentity();
      this.length = contract.getLength();
      this.label = contract.getLabel();
      this.loader = loader;
   }
   
   /**
    * This is used to recover the object references from the document
    * using the special attributes specified. This allows the element
    * specified by the <code>NodeMap</code> to be used to discover
    * exactly which node in the object graph the element represents.
    * 
    * @param type the type of the field or method in the instance
    * @param node this is the XML element to be deserialized
    * 
    * @return this is used to return the type to acquire the value
    */
   public Value read(Type type, NodeMap node) throws Exception {
      Node entry = node.remove(label);
      Class expect = type.getType();
      
      if(expect.isArray()) {
         expect = expect.getComponentType();
      }
      if(entry != null) {      
         String name = entry.getValue();
         expect = loader.load(name);
      }  
      return readInstance(type, expect, node); 
   }
   
   /**
    * This is used to recover the object references from the document
    * using the special attributes specified. This allows the element
    * specified by the <code>NodeMap</code> to be used to discover
    * exactly which node in the object graph the element represents.
    * 
    * @param type the type of the field or method in the instance
    * @param real this is the overridden type from the XML element
    * @param node this is the XML element to be deserialized
    * 
    * @return this is used to return the type to acquire the value
    */
   private Value readInstance(Type type, Class real, NodeMap node) throws Exception {      
      Node entry = node.remove(mark);
      
      if(entry == null) {
         return readReference(type, real, node);
      }      
      String key = entry.getValue();
      
      if(containsKey(key)) {
         throw new CycleException("Element '%s' already exists", key);
      }
      return readValue(type, real, node, key);
   }
   
   /**
    * This is used to recover the object references from the document
    * using the special attributes specified. This allows the element
    * specified by the <code>NodeMap</code> to be used to discover
    * exactly which node in the object graph the element represents.
    * 
    * @param type the type of the field or method in the instance
    * @param real this is the overridden type from the XML element
    * @param node this is the XML element to be deserialized    
    * 
    * @return this is used to return the type to acquire the value
    */ 
   private Value readReference(Type type, Class real, NodeMap node) throws Exception {
      Node entry = node.remove(refer);
      
      if(entry == null) {
         return readValue(type, real, node);
      }
      String key = entry.getValue();
      Object value = get(key); 
         
      if(!containsKey(key)) {        
         throw new CycleException("Invalid reference '%s' found", key);
      }
      return new Reference(value, real);
   }
   
   /**
    * This is used to acquire the <code>Value</code> which can be used 
    * to represent the deserialized value. The type create cab be
    * added to the graph of created instances if the XML element has
    * an identification attribute, this allows cycles to be completed.
    *
    * @param type the type of the field or method in the instance
    * @param real this is the overridden type from the XML element
    * @param node this is the XML element to be deserialized    
    * 
    * @return this is used to return the type to acquire the value
    */
   private Value readValue(Type type, Class real, NodeMap node) throws Exception {      
      Class expect = type.getType();
      
      if(expect.isArray()) {
         return readArray(type, real, node);
      }
      return new ObjectValue(real);
   }
   
   /**
    * This is used to acquire the <code>Value</code> which can be used 
    * to represent the deserialized value. The type create cab be
    * added to the graph of created instances if the XML element has
    * an identification attribute, this allows cycles to be completed.
    *
    * @param type the type of the field or method in the instance
    * @param real this is the overridden type from the XML element
    * @param node this is the XML element to be deserialized
    * @param key the key the instance is known as in the graph    
    * 
    * @return this is used to return the type to acquire the value
    */
   private Value readValue(Type type, Class real, NodeMap node, String key) throws Exception {
      Value value = readValue(type, real, node);
      
      if(key != null) {
         return new Allocate(value, this, key);
      }
      return value;      
   }
   
   /**
    * This is used to acquire the <code>Value</code> which can be used 
    * to represent the deserialized value. The type create cab be
    * added to the graph of created instances if the XML element has
    * an identification attribute, this allows cycles to be completed.
    *
    * @param type the type of the field or method in the instance
    * @param real this is the overridden type from the XML element
    * @param node this is the XML element to be deserialized  
    * 
    * @return this is used to return the type to acquire the value
    */  
   private Value readArray(Type type, Class real, NodeMap node) throws Exception {
      Node entry = node.remove(length);
      int size = 0;
      
      if(entry != null) {
         String value = entry.getValue();
         size = Integer.parseInt(value);
      }      
      return new ArrayValue(real, size);      
   }
}
