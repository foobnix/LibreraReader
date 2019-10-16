/*
 * RegistryStrategy.java January 2010
 *
 * Copyright (C) 2010, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.xml.convert;

import java.util.Map;

import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.strategy.TreeStrategy;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Value;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;

/**
 * The <code>RegistryStrategy</code> object is used to intercept
 * the serialization process and delegate to custom converters. The
 * custom converters are resolved from a <code>Registry</code>
 * object, which is provided to the constructor. If there is no
 * binding for a particular object then serialization is delegated
 * to an internal strategy. All converters resolved by this are
 * instantiated once and cached internally for performance.
 * <p>
 * By default the <code>TreeStrategy</code> is used to perform the
 * normal serialization process should there be no class binding
 * specifying a converter to use. However, any implementation can
 * be used, including the <code>CycleStrategy</code>, which handles
 * cycles in the object graph. To specify the internal strategy to
 * use it can be provided in the constructor.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.convert.Registry
 */
public class RegistryStrategy implements Strategy {
   
   /**
    * This is the registry that is used to resolve bindings.
    */
   private final Registry registry;
   
   /**
    * This is the strategy used if there is no bindings.
    */
   private final Strategy strategy;
   
   /**
    * Constructor for the <code>RegistryStrategy</code> object. This
    * is used to create a strategy that will intercept the normal
    * serialization process by searching for bindings within the
    * provided <code>Registry</code> instance.
    * 
    * @param registry this is the registry instance with bindings
    */
   public RegistryStrategy(Registry registry) {
      this(registry, new TreeStrategy());
   }
   
   /**
    * Constructor for the <code>RegistryStrategy</code> object. This
    * is used to create a strategy that will intercept the normal
    * serialization process by searching for bindings within the
    * provided <code>Registry</code> instance.
    * 
    * @param registry this is the registry instance with bindings
    * @param strategy this is the strategy to delegate to
    */
   public RegistryStrategy(Registry registry, Strategy strategy){
      this.registry = registry;
      this.strategy = strategy;
   }
   
   /**
    * This is used to read the <code>Value</code> which will be used 
    * to represent the deserialized object. If there is an binding
    * present then the value will contain an object instance. If it
    * does not then it is up to the internal strategy to determine 
    * what the returned value contains.
    * 
    * @param type this is the type that represents a method or field
    * @param node this is the node representing the XML element
    * @param map this is the session map that contain variables
    * 
    * @return the value representing the deserialized value
    */
   public Value read(Type type, NodeMap<InputNode> node, Map map) throws Exception {
      Value value = strategy.read(type, node, map);
      
      if(isReference(value)) {
         return value;
      }
      return read(type, node, value);
   }
  
   /**
    * This is used to read the <code>Value</code> which will be used 
    * to represent the deserialized object. If there is an binding
    * present then the value will contain an object instance. If it
    * does not then it is up to the internal strategy to determine 
    * what the returned value contains.
    * 
    * @param type this is the type that represents a method or field
    * @param node this is the node representing the XML element
    * @param value this is the value from the internal strategy
    * 
    * @return the value representing the deserialized value
    */   
   private Value read(Type type, NodeMap<InputNode> node, Value value) throws Exception {
      Converter converter = lookup(type, value);
      InputNode source = node.getNode();
      
      if(converter != null) {
         Object data = converter.read(source);
         Class actual = type.getType();
      
         if(value != null) {
            value.setValue(data);
         }
         return new Reference(value, data, actual);
      }
      return value;
   }
   
   /**
    * This is used to serialize a representation of the object value
    * provided. If there is a <code>Registry</code> binding present
    * for the provided type then this will use the converter specified
    * to serialize a representation of the object. If however there
    * is no binding present then this will delegate to the internal 
    * strategy. This returns true if the serialization has completed.
    * 
    * @param type this is the type that represents the field or method
    * @param value this is the object instance to be serialized
    * @param node this is the XML element to be serialized to
    * @param map this is the session map used by the serializer
    * 
    * @return this returns true if it was serialized, false otherwise
    */
   public boolean write(Type type, Object value, NodeMap<OutputNode> node, Map map) throws Exception {
      boolean reference = strategy.write(type, value, node, map);
      
      if(!reference) {
         return write(type, value, node);
      }
      return reference;
   }
   
   /**
    * This is used to serialize a representation of the object value
    * provided. If there is a <code>Registry</code> binding present
    * for the provided type then this will use the converter specified
    * to serialize a representation of the object. If however there
    * is no binding present then this will delegate to the internal 
    * strategy. This returns true if the serialization has completed.
    * 
    * @param type this is the type that represents the field or method
    * @param value this is the object instance to be serialized
    * @param node this is the XML element to be serialized to
    * 
    * @return this returns true if it was serialized, false otherwise
    */
   private boolean write(Type type, Object value, NodeMap<OutputNode> node) throws Exception {
      Converter converter = lookup(type, value);
      OutputNode source = node.getNode();
      
      if(converter != null) {
         converter.write(source, value);
         return true;
      }
      return false;  
   }
   
   /**
    * This is used to acquire a <code>Converter</code> instance for 
    * the provided value object. The value object is used to resolve
    * the converter to use for the serialization process.
    * 
    * @param type this is the type representing the field or method
    * @param value this is the value that is to be serialized
    * 
    * @return this returns the converter instance that is matched
    */
   private Converter lookup(Type type, Value value) throws Exception {
      Class real = type.getType();
      
      if(value != null) {
         real = value.getType();
      }
      return registry.lookup(real);
   }
   
   /**
    * This is used to acquire a <code>Converter</code> instance for 
    * the provided object instance. The instance class is used to
    * resolve the converter to use for the serialization process.
    * 
    * @param type this is the type representing the field or method
    * @param value this is the value that is to be serialized
    * 
    * @return this returns the converter instance that is matched
    */
   private Converter lookup(Type type, Object value) throws Exception {
      Class real = type.getType();
      
      if(value != null) {
         real = value.getClass();
      }
      return registry.lookup(real);
   }
   
   /**
    * This is used to determine if the <code>Value</code> provided
    * represents a reference. If it does represent a reference then
    * this will return true, if it does not then this returns false.
    * 
    * @param value this is the value instance to be evaluated
    * 
    * @return this returns true if the value represents a reference
    */
   private boolean isReference(Value value) {
      return value != null && value.isReference();
   }
}
