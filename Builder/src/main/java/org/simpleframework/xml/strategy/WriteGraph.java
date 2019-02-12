/*
 * WriteGraph.java April 2007
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

import java.lang.reflect.Array;
import java.util.IdentityHashMap;

import org.simpleframework.xml.stream.NodeMap;

/**
 * The <code>WriteGraph</code> object is used to build the graph that
 * is used to represent the serialized object and its references. The
 * graph is stored in an <code>IdentityHashMap</code> which will 
 * store the objects in such a way that this graph object can tell if
 * it has already been written to the XML document. If an object has
 * already been written to the XML document an reference attribute
 * is added to the element representing the object and serialization
 * of that object is complete, that is, no more elements are written.
 * <p>
 * The attribute values written by this are unique strings, which 
 * allows the deserialization process to identify object references
 * easily. By default these references are incrementing integers 
 * however for deserialization they can be any unique string value.
 * 
 * @author Niall Gallagher
 */
class WriteGraph extends IdentityHashMap<Object, String> {
   
   /**
    * This is used to specify the length of array instances.
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
    * Constructor for the <code>WriteGraph</code> object. This is
    * used to build the graph used for writing objects to the XML 
    * document. The specified strategy is used to acquire the names
    * of the special attributes used during the serialization.
    * 
    * @param contract this is the name scheme used by the strategy 
    */
   public WriteGraph(Contract contract) {
      this.refer = contract.getReference();
      this.mark = contract.getIdentity();
      this.length = contract.getLength();
      this.label = contract.getLabel();
   }
   
   /**
    * This is used to write the XML element attributes representing
    * the serialized object instance. If the object has already been
    * serialized to the XML document then a reference attribute is
    * inserted and this returns true, if not, then this will write
    * a unique identity marker attribute and return false.
    * 
    * @param type this is the type of the object to be serialized
    * @param value this is the instance that is to be serialized    
    * @param node this is the node that contains the attributes
    * 
    * @return returns true if the element has been fully written
    */
   public boolean write(Type type, Object value, NodeMap node){
      Class actual = value.getClass();
      Class expect = type.getType();
      Class real = actual;
      
      if(actual.isArray()) {
         real = writeArray(actual, value, node);
      }
      if(actual != expect) {
         node.put(label, real.getName());
      }       
      return writeReference(value, node);
   }
   
   /**
    * This is used to write the XML element attributes representing
    * the serialized object instance. If the object has already been
    * serialized to the XML document then a reference attribute is
    * inserted and this returns true, if not, then this will write
    * a unique identity marker attribute and return false.
    *
    * @param value this is the instance that is to be serialized    
    * @param node this is the node that contains the attributes
    * 
    * @return returns true if the element has been fully written
    */   
   private boolean writeReference(Object value, NodeMap node) {
      String name = get(value);
      int size = size();
      
      if(name != null) {
         node.put(refer, name);
         return true;
      } 
      String unique = String.valueOf(size);
      
      node.put(mark, unique);
      put(value, unique);
      
      return false;   
   }
   
   /**
    * This is used to add a length attribute to the element due to
    * the fact that the serialized value is an array. The length
    * of the array is acquired and inserted in to the attributes.
    * 
    * @param field this is the field type for the array to set
    * @param value this is the actual value for the array to set
    * @param node this is the map of attributes for the element
    * 
    * @return returns the array component type that is set
    */
   private Class writeArray(Class field, Object value, NodeMap node){
      int size = Array.getLength(value);
      
      if(!containsKey(value)) {       
         node.put(length, String.valueOf(size));
      }
      return field.getComponentType();
   }
}
