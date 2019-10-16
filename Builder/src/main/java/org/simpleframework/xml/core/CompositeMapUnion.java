/*
 * CompositeMapUnion.java March 2011
 *
 * Copyright (C) 2011, Niall Gallagher <niallg@users.sf.net>
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

import java.util.Collections;
import java.util.Map;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>CompositeMapUnion</code> object is used to act as a 
 * mediator for multiple converters associated with a particular union 
 * group. This will basically determine which <code>Converter</code> 
 * should be delegated to based on either the XML element name being read 
 * or the type of the instance object being written. Selection of the 
 * converter is done by consulting the <code>Group</code> of labels 
 * representing the union declaration.
 * 
 * @author Niall Gallagher
 */
class CompositeMapUnion implements Repeater {
   
   /**
    * This contains the labels in the union group keyed by name.
    */
   private final LabelMap elements;
   
   /**
    * This is the path expression used to represent this union.
    */
   private final Expression path;
   
   /**
    * This is the current context used for the serialization.
    */
   private final Context context;
   
   /**
    * This contains the group of labels associated with the union.
    */
   private final Group group;
   
   /**
    * This is this style associated with the serialization context.
    */
   private final Style style;
   
   /**
    * This is the type field or method annotated as a union.
    */
   private final Type type;

   /**
    * Constructor for the <code>CompositeMapUnion</code> object. This
    * is used to create a converter that delegates to other associated
    * converters within the union group depending on the XML element
    * name being read or the instance type that is being written.
    * 
    * @param context this is the context used for the serialization
    * @param group this is the union group used for delegation
    * @param path this is the path expression representing this union
    * @param type this is the annotated field or method to be used
    */
   public CompositeMapUnion(Context context, Group group, Expression path, Type type) throws Exception {
      this.elements = group.getElements();
      this.style = context.getStyle();
      this.context = context;
      this.group = group;
      this.type = type;
      this.path = path;
   }

   /**
    * The <code>read</code> method uses the name of the XML element to
    * select a converter to be used to read the instance. Selection of
    * the converter is done by looking up the associated label from
    * the union group using the element name. Once the converter has
    * been selected it is used to read the instance.
    * 
    * @param node this is the XML element used to read the instance
    * 
    * @return this is the instance that has been read by this
    */
   public Object read(InputNode node) throws Exception {
      String name = node.getName();
      String element = path.getElement(name);
      Label label = elements.get(element);
      Converter converter = label.getConverter(context);
      
      return converter.read(node);
   }

   /**
    * The <code>read</code> method uses the name of the XML element to
    * select a converter to be used to read the instance. Selection of
    * the converter is done by looking up the associated label from
    * the union group using the element name. Once the converter has
    * been selected it is used to read the instance.
    * 
    * @param node this is the XML element used to read the instance
    * @param value this is the value that is to be repeated
    * 
    * @return this is the instance that has been read by this
    */
   public Object read(InputNode node, Object value) throws Exception {
      String name = node.getName();
      String element = path.getElement(name);
      Label label = elements.get(element);
      Converter converter = label.getConverter(context);
      
      return converter.read(node, value);
   }
   
   /**
    * The <code>validate</code> method is used to validate the XML
    * element provided using an associated class schema. The schema
    * is selected using the name of the XML element to acquire
    * the associated converter. Once the converter has been acquired
    * it is delegated to and validated against it.
    * 
    * @param node this is the input XML element to be validated
    * 
    * @return this returns true if the node validates 
    */
   public boolean validate(InputNode node) throws Exception {
      String name = node.getName();
      String element = path.getElement(name);
      Label label = elements.get(element);
      Converter converter = label.getConverter(context);
      
      return converter.validate(node);
   }
   
   /**
    * The <code>write</code> method uses the name of the XML element to
    * select a converter to be used to write the instance. Selection of
    * the converter is done by looking up the associated label from
    * the union group using the instance type. Once the converter has
    * been selected it is used to write the instance.
    * 
    * @param node this is the XML element used to write the instance
    * @param source this is the value that is to be written
    */
   public void write(OutputNode node, Object source) throws Exception {               
      Map map = (Map) source;

      if(group.isInline()) {
         if(!map.isEmpty()) {
            write(node, map);
         } else if(!node.isCommitted()){
            node.remove();
         }
      } else {
         write(node, map);
      }
   }

   /**
    * The <code>write</code> method uses the name of the XML element to
    * select a converter to be used to write the instance. Selection of
    * the converter is done by looking up the associated label from
    * the union group using the instance type. Once the converter has
    * been selected it is used to write the instance.
    * 
    * @param node this is the XML element used to write the instance
    * @param map this is the value that is to be written
    */
   private void write(OutputNode node, Map map) throws Exception {
      for(Object key : map.keySet()) {
         Object item = map.get(key);
         
         if(item != null) {
            Class real = item.getClass();
            Label label = group.getLabel(real);
            
            if(label == null) {               
               throw new UnionException("Value of %s not declared in %s with annotation %s", real, type, group);
            }
            write(node, key, item, label);
         }
      }
   }
   
   /**
    * The <code>write</code> method uses the name of the XML element to
    * select a converter to be used to write the instance. Selection of
    * the converter is done by looking up the associated label from
    * the union group using the instance type. Once the converter has
    * been selected it is used to write the instance.
    * 
    * @param node this is the XML element used to write the instance
    * @param key this is the key associated with the item to write
    * @param item this is the value associated with the item to write
    * @param label this is the label to used to acquire the converter     
    */
   private void write(OutputNode node, Object key, Object item, Label label) throws Exception {  
      Converter converter = label.getConverter(context);
      Map map = Collections.singletonMap(key, item);
      
      if(!label.isInline()) {
         String name = label.getName();
         String root = style.getElement(name);
        
         if(!node.isCommitted()) {
            node.setName(root);
         }
      }
      converter.write(node, map);   
   }
}
