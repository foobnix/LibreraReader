/*
 * AnnotationStrategy.java January 2010
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
 * The <code>AnnotationStrategy</code> object is used to intercept
 * the serialization process and delegate to custom converters. This
 * strategy uses the <code>Convert</code> annotation to specify the
 * converter to use for serialization and deserialization. If there
 * is no annotation present on the field or method representing the
 * object instance to be serialized then this acts as a transparent
 * proxy to an internal strategy.
 * <p>
 * By default the <code>TreeStrategy</code> is used to perform the
 * normal serialization process should there be no annotation
 * specifying a converter to use. However, any implementation can
 * be used, including the <code>CycleStrategy</code>, which handles
 * cycles in the object graph. To specify the internal strategy to
 * use it can be provided in the constructor.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.strategy.TreeStrategy
 */
public class AnnotationStrategy implements Strategy {
   
   /**
    * This is used to scan for an annotation and create a converter.
    */
   private final ConverterScanner scanner;
   
   /**
    * This is the strategy that is delegated to for serialization.
    */
   private final Strategy strategy;
   
   /**
    * Constructor for the <code>AnnotationStrategy</code> object. 
    * This creates a strategy that intercepts serialization on any
    * annotated method or field. If no annotation exists then this
    * delegates to an internal <code>TreeStrategy</code> object.
    */
   public AnnotationStrategy() {
      this(new TreeStrategy());
   }
   
   /**
    * Constructor for the <code>AnnotationStrategy</code> object. 
    * This creates a strategy that intercepts serialization on any
    * annotated method or field. If no annotation exists then this
    * will delegate to the <code>Strategy</code> provided.
    * 
    * @param strategy the internal strategy to delegate to
    */
   public AnnotationStrategy(Strategy strategy) {
      this.scanner = new ConverterScanner();
      this.strategy = strategy;
   }
   
   /**
    * This is used to read the <code>Value</code> which will be used 
    * to represent the deserialized object. If there is an annotation
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
    * to represent the deserialized object. If there is an annotation
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
      Converter converter = scanner.getConverter(type, value);
      InputNode parent = node.getNode();
      
      if(converter != null) {
         Object data = converter.read(parent);
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
    * provided. If there is a <code>Convert</code> annotation present
    * on the provided type then this will use the converter specified
    * to serialize a representation of the object. If however there
    * is no annotation then this will delegate to the internal 
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
    * provided. If there is a <code>Convert</code> annotation present
    * on the provided type then this will use the converter specified
    * to serialize a representation of the object. If however there
    * is no annotation then this will delegate to the internal 
    * strategy. This returns true if the serialization has completed.
    * 
    * @param type this is the type that represents the field or method
    * @param value this is the object instance to be serialized
    * @param node this is the XML element to be serialized to
    * 
    * @return this returns true if it was serialized, false otherwise
    */
   private boolean write(Type type, Object value, NodeMap<OutputNode> node) throws Exception {
      Converter converter = scanner.getConverter(type, value);
      OutputNode parent = node.getNode();
      
      if(converter != null) {
         converter.write(parent, value);
         return true;
      }
      return false;
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