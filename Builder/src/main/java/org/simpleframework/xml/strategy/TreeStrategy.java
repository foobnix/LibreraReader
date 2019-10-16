/*
 * TreeStrategy.java July 2006
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

package org.simpleframework.xml.strategy;

import static org.simpleframework.xml.strategy.Name.LABEL;
import static org.simpleframework.xml.strategy.Name.LENGTH;

import java.lang.reflect.Array;
import java.util.Map;

import org.simpleframework.xml.stream.Node;
import org.simpleframework.xml.stream.NodeMap;

/**
 * The <code>TreeStrategy</code> object is used to provide a simple
 * strategy for handling object graphs in a tree structure. This does
 * not resolve cycles in the object graph. This will make use of the
 * specified class attribute to resolve the class to use for a given 
 * element during the deserialization process. For the serialization 
 * process the "class" attribute will be added to the element specified.
 * If there is a need to use an attribute name other than "class" then 
 * the name of the attribute to use can be specified.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.strategy.CycleStrategy
 */
public class TreeStrategy implements Strategy {
   
   /**
    * This is the loader that is used to load the specified class.
    */
   private final Loader loader;
   
   /**
    * This is the attribute that is used to determine an array size.
    */
   private final String length;
   
   /**   
    * This is the attribute that is used to determine the real type.
    */   
   private final String label;
   
   /**
    * Constructor for the <code>TreeStrategy</code> object. This 
    * is used to create a strategy that can resolve and load class
    * objects for deserialization using a "class" attribute. Also
    * for serialization this will add the appropriate "class" value.
    */
   public TreeStrategy() {
      this(LABEL, LENGTH);           
   }        
   
   /**
    * Constructor for the <code>TreeStrategy</code> object. This 
    * is used to create a strategy that can resolve and load class
    * objects for deserialization using the specified attribute. 
    * The attribute value can be any legal XML attribute name.
    * 
    * @param label this is the name of the attribute to use
    * @param length this is used to determine the array length
    */
   public TreeStrategy(String label, String length) {
      this.loader = new Loader();
      this.length = length;
      this.label = label;         
   } 
   
   /**
    * This is used to resolve and load a class for the given element.
    * Resolution of the class to used is done by inspecting the
    * XML element provided. If there is a "class" attribute on the
    * element then its value is used to resolve the class to use.
    * If no such attribute exists on the element this returns null.
    * 
    * @param type this is the type of the XML element expected
    * @param node this is the element used to resolve an override
    * @param map this is used to maintain contextual information
    * 
    * @return returns the class that should be used for the object
    * 
    * @throws Exception thrown if the class cannot be resolved
    */
   public Value read(Type type, NodeMap node, Map map) throws Exception {
      Class actual = readValue(type, node);
      Class expect = type.getType();
      
      if(expect.isArray()) {
         return readArray(actual, node);   
      }
      if(expect != actual) {
         return new ObjectValue(actual);
      }
      return null;
   }
   
   /**
    * This is used to resolve and load a class for the given element.
    * Resolution of the class to used is done by inspecting the
    * XML element provided. If there is a "class" attribute on the
    * element then its value is used to resolve the class to use.
    * This also expects a "length" attribute for the array length.
    * 
    * @param type this is the type of the XML element expected
    * @param node this is the element used to resolve an override
    * 
    * @return returns the class that should be used for the object
    * 
    * @throws Exception thrown if the class cannot be resolved
    */   
   private Value readArray(Class type, NodeMap node) throws Exception {      
      Node entry = node.remove(length);
      int size = 0;
      
      if(entry != null) {
         String value = entry.getValue();
         size = Integer.parseInt(value);
      }      
      return new ArrayValue(type, size);
   }
   
   /**
    * This is used to resolve and load a class for the given element.
    * Resolution of the class to used is done by inspecting the
    * XML element provided. If there is a "class" attribute on the
    * element then its value is used to resolve the class to use.
    * If no such attribute exists the specified field is returned,
    * or if the field type is an array then the component type.
    * 
    * @param type this is the type of the XML element expected
    * @param node this is the element used to resolve an override
    * 
    * @return returns the class that should be used for the object
    * 
    * @throws Exception thrown if the class cannot be resolved
    */   
   private Class readValue(Type type, NodeMap node) throws Exception {      
      Node entry = node.remove(label);      
      Class expect = type.getType();
      
      if(expect.isArray()) {
         expect = expect.getComponentType();
      }
      if(entry != null) {
         String name = entry.getValue();
         expect = loader.load(name);
      }    
      return expect;
   }       
   
   /**
    * This is used to attach a attribute to the provided element
    * that is used to identify the class. The attribute name is
    * "class" and has the value of the fully qualified class 
    * name for the object provided. This will only be invoked
    * if the object class is different from the field class.
    *
    * @param type this is the declared class for the field used
    * @param value this is the instance variable being serialized
    * @param node this is the element used to represent the value
    * @param map this is used to maintain contextual information
    * 
    * @return this returns true if serialization is complete
    */   
   public boolean write(Type type, Object value, NodeMap node, Map map){
      Class actual = value.getClass();
      Class expect = type.getType();
      Class real = actual;
      
      if(actual.isArray()) {
         real = writeArray(expect, value, node);
      }
      if(actual != expect) {
         node.put(label, real.getName());
      }       
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
      
      if(length != null) {       
         node.put(length, String.valueOf(size));
      }
      return field.getComponentType();
   }
}
