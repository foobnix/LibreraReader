/*
 * Traverser.java July 2006
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

package org.simpleframework.xml.core;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>Traverser</code> object is used to traverse the XML class
 * schema and either serialize or deserialize an object. This is the
 * root of all serialization and deserialization operations. It uses
 * the <code>Root</code> annotation to ensure that the XML schema
 * matches the provided XML element. If no root element is defined the
 * serialization and deserialization cannot be performed.
 *
 * @author Niall Gallagher
 */ 
class Traverser {

   /**
    * This is the context object used for the traversal performed.
    */
   private final Context context;
   
   /**
    * This is the style that is used to style the XML roots.
    */
   private final Style style;
        
   /**
    * Constructor for the <code>Traverser</code> object. This creates
    * a traverser that can be used to perform serialization or
    * or deserialization of an object. This requires a source object.
    * 
    * @param context the context object used for the traversal
    */
   public Traverser(Context context) {
      this.style = context.getStyle();
      this.context = context;           
   }
   
   /**
    * This will acquire the <code>Decorator</code> for the type.
    * A decorator is an object that adds various details to the
    * node without changing the overall structure of the node. For
    * example comments and namespaces can be added to the node with
    * a decorator as they do not affect the deserialization.
    * 
    * @param type this is the type to acquire the decorator for 
    *
    * @return this returns the decorator associated with this
    */
   private Decorator getDecorator(Class type) throws Exception {
      return context.getDecorator(type);
   }
   
   /**
    * This <code>read</code> method is used to deserialize an object 
    * from the provided XML element. The class provided acts as the
    * XML schema definition used to control the deserialization. If
    * the XML schema does not have a <code>Root</code> annotation 
    * this throws an exception. Also if the root annotation name is
    * not the same as the XML element name an exception is thrown.  
    * 
    * @param node this is the node that is to be deserialized
    * @param type this is the XML schema class to be used
    * 
    * @return an object deserialized from the XML element 
    * 
    * @throws Exception if the XML schema does not match the node
    */
   public Object read(InputNode node, Class type) throws Exception {
      Composite factory = getComposite(type);           
      Object value = factory.read(node);
      
      if(value != null) {
         Class real = value.getClass();

         return read(node, real, value);
      }
      return null;
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown. 
    * 
    * @param node this is the node that is to be deserialized
    * @param value this is the value that is to be deserialized
    * 
    * @return an object deserialized from the XML element 
    * 
    * @throws Exception if the XML schema does not match the node
    */
   public Object read(InputNode node, Object value) throws Exception {
      Class type = value.getClass();
      Composite factory = getComposite(type);        
      Object real = factory.read(node, value);
      
      return read(node, type, real);
   }
   
   /**
    * This <code>read</code> method is used to deserialize an object 
    * from the provided XML element. The class provided acts as the
    * XML schema definition used to control the deserialization. If
    * the XML schema does not have a <code>Root</code> annotation 
    * this throws an exception. Also if the root annotation name is
    * not the same as the XML element name an exception is thrown.  
    * 
    * @param node this is the node that is to be deserialized
    * @param value this is the XML schema object to be used
    * 
    * @return an object deserialized from the XML element 
    * 
    * @throws Exception if the XML schema does not match the XML
    */ 
   private Object read(InputNode node, Class type, Object value) throws Exception {
      String root = getName(type);
     
      if(root == null) {
         throw new RootException("Root annotation required for %s", type);
      }
      return value;
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param node this provides the source of the XML document
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(InputNode node, Class type) throws Exception {
      Composite factory = getComposite(type);
      String root = getName(type);
      
      if(root == null) {
         throw new RootException("Root annotation required for %s", type);
      }
      return factory.validate(node);
   }
   
   /**
    * This <code>write</code> method is used to convert the provided
    * object to an XML element. This creates a child node from the
    * given <code>OutputNode</code> object. Once this child element 
    * is created it is populated with the fields of the source object
    * in accordance with the XML schema class.  
    * 
    * @param source this is the object to be serialized to XML
    * 
    * @throws Exception thrown if there is a problem serializing
    */
   public void write(OutputNode node, Object source) throws Exception {
      write(node, source, source.getClass());
   }

   /**
    * This <code>write</code> method is used to convert the provided
    * object to an XML element. This creates a child node from the
    * given <code>OutputNode</code> object. Once this child element 
    * is created it is populated with the fields of the source object
    * in accordance with the XML schema class.  
    * 
    * @param source this is the object to be serialized to XML
    * @param expect this is the class that is expected to be written
    * 
    * @throws Exception thrown if there is a problem serializing
    */
   public void write(OutputNode node, Object source, Class expect) throws Exception {
      Class type = source.getClass();      
      String root = getName(type);

      if(root == null) {
         throw new RootException("Root annotation required for %s", type);
      }
      write(node, source, expect, root);
   }
   
   /**
    * This <code>write</code> method is used to convert the provided
    * object to an XML element. This creates a child node from the
    * given <code>OutputNode</code> object. Once this child element 
    * is created it is populated with the fields of the source object
    * in accordance with the XML schema class.  
    * 
    * @param source this is the object to be serialized to XML
    * @param expect this is the class that is expected to be written
    * @param name this is the name of the root annotation used 
    * 
    * @throws Exception thrown if there is a problem serializing
    */
   public void write(OutputNode node, Object source, Class expect, String name) throws Exception {
      OutputNode child = node.getChild(name);
      Type type = getType(expect);
      
      if(source != null) {
         Class actual = source.getClass();
         Decorator decorator = getDecorator(actual);
         
         if(decorator != null) {
            decorator.decorate(child);
         }
         if(!context.setOverride(type, source, child)) {
            getComposite(actual).write(child, source);         
         }
      }         
      child.commit();      
   }
   
   /**
    * This will create a <code>Composite</code> object using the XML 
    * schema class provided. This makes use of the source object that
    * this traverser has been given to create a composite converter. 
    * 
    * @param expect this is the XML schema class to be used
    * 
    * @return a converter for the specified XML schema class
    */
   private Composite getComposite(Class expect) throws Exception {
      Type type = getType(expect);
      
      if(expect == null) {
         throw new RootException("Can not instantiate null class");
      }
      return new Composite(context, type);
   }
   
   /**
    * This is used to acquire a type for the provided class. This will
    * wrap the class in a <code>Type</code> wrapper object. Wrapping
    * the class allows it to be used within the framework.
    * 
    * @param type this is the type that is to be wrapped for use
    * 
    * @return this returns the type that wraps the specified class
    */
   private Type getType(Class type) {
      return new ClassType(type);
   }
   
   /**
    * Extracts the <code>Root</code> annotation from the provided XML
    * schema class. If no annotation exists in the provided class the
    * super class is checked and so on until the <code>Object</code>
    * is encountered, if no annotation is found this returns null.
    *  
    * @param type this is the XML schema class to use
    * 
    * @return this returns the root annotation for the XML schema
    */   
   protected String getName(Class type) throws Exception {
      String root = context.getName(type);
      String name = style.getElement(root);
      
      return name;
   }
}
