/*
 * Primitive.java July 2006
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

/**
 * The <code>Primitive</code> object is used to provide serialization
 * for primitive objects. This can serialize and deserialize any
 * primitive object and enumerations. Primitive values are converted
 * to text using the <code>String.valueOf</code> method. Enumerated
 * types are converted using the <code>Enum.valueOf</code> method.
 * <p>
 * Text within attributes and elements can contain template variables
 * similar to those found in Apache <cite>Ant</cite>. This allows
 * values such as system properties, environment variables, and user
 * specified mappings to be inserted into the text in place of the
 * template reference variables.
 * <pre>
 * 
 *    &lt;example attribute="${value}&gt;
 *       &lt;text&gt;Text with a ${variable}&lt;/text&gt;
 *    &lt;/example&gt;
 * 
 * </pre>
 * In the above XML element the template variable references will be
 * checked against the <code>Filter</code> object used by the context
 * serialization object. If they corrospond to a filtered value then
 * they are replaced, if not the text remains unchanged.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.filter.Filter
 */ 
class Primitive implements Converter {

   /**
    * This is used to convert the string values to primitives.
    */         
   private final PrimitiveFactory factory;
        
   /**
    * The context object is used to perform text value filtering.
    */ 
   private final Context context;
   
   /**
    * This the value used to represent a null primitive value.
    */
   private final String empty;
   
   /**
    * This is the type that this primitive expects to represent.
    */
   private final Class expect;
   
   /**
    * This is the actual method or field that has been annotated.
    */
   private final Type type;
   
   /**
    * Constructor for the <code>Primitive</code> object. This is used
    * to convert an XML node to a primitive object and vice versa. To
    * perform deserialization the primitive object requires the context
    * object used for the instance of serialization to performed.
    *
    * @param context the context object used for the serialization
    * @param type this is the type of primitive this represents
    */ 
   public Primitive(Context context, Type type) {
      this(context, type, null);          
   }
   
   /**
    * Constructor for the <code>Primitive</code> object. This is used
    * to convert an XML node to a primitive object and vice versa. To
    * perform deserialization the primitive object requires the context
    * object used for the instance of serialization to performed.
    *
    * @param context the context object used for the serialization
    * @param type this is the type of primitive this represents
    * @param empty this is the value used to represent a null value
    */ 
   public Primitive(Context context, Type type, String empty) {
      this.factory = new PrimitiveFactory(context, type);  
      this.expect = type.getType();
      this.context = context; 
      this.empty = empty;     
      this.type = type;     
   }

   /**
    * This <code>read</code> method will extract the text value from
    * the node and replace any template variables before converting
    * it to a primitive value. This uses the <code>Context</code>
    * object used for this instance of serialization to replace all
    * template variables with values from the context filter.
    *
    * @param node this is the node to be converted to a primitive
    *
    * @return this returns the primitive that has been deserialized
    */ 
   public Object read(InputNode node) throws Exception{
      if(node.isElement()) {
         return readElement(node);
      }
      return read(node, expect);
   }  
   
   /**
    * This <code>read</code> method will extract the text value from
    * the node and replace any template variables before converting
    * it to a primitive value. This uses the <code>Context</code>
    * object used for this instance of serialization to replace all
    * template variables with values from the context filter.
    *
    * @param node this is the node to be converted to a primitive
    * @param value this is the original primitive value used
    *
    * @return this returns the primitive that has been deserialized
    * 
    * @throws Exception if value is not null an exception is thrown
    */ 
   public Object read(InputNode node, Object value) throws Exception{
      if(value != null) {
         throw new PersistenceException("Can not read existing %s for %s", expect, type);
      }
      return read(node);
   }
   
   /**
    * This <code>read</code> method will extract the text value from
    * the node and replace any template variables before converting
    * it to a primitive value. This uses the <code>Context</code>
    * object used for this instance of serialization to replace all
    * template variables with values from the context filter.
    *
    * @param node this is the node to be converted to a primitive
    * @param type this is the type to read the primitive with
    *
    * @return this returns the primitive that has been deserialized
    */ 
   public Object read(InputNode node, Class type) throws Exception{
      String value = node.getValue();

      if(value == null) {
         return null;
      }
      if(empty != null && value.equals(empty)) {
         return empty;         
      }
      return readTemplate(value, type);
   }
   
   /**
    * This <code>read</code> method will extract the text value from
    * the node and replace any template variables before converting
    * it to a primitive value. This uses the <code>Context</code>
    * object used for this instance of serialization to replace all
    * template variables with values from the context filter.
    *
    * @param node this is the node to be converted to a primitive
    *
    * @return this returns the primitive that has been deserialized
    */ 
   private Object readElement(InputNode node) throws Exception {
      Instance value = factory.getInstance(node);
      
      if(!value.isReference()) {
         return readElement(node, value);
      }
      return value.getInstance();
   }
   
   /**
    * This <code>read</code> method will extract the text value from
    * the node and replace any template variables before converting
    * it to a primitive value. This uses the <code>Context</code>
    * object used for this instance of serialization to replace all
    * template variables with values from the context filter.
    *
    * @param node this is the node to be converted to a primitive
    * @param value this is the instance to set the result to
    *
    * @return this returns the primitive that has been deserialized
    */ 
   private Object readElement(InputNode node, Instance value) throws Exception {
      Object result = read(node, expect);
      
      if(value != null) {
         value.setInstance(result);
      }
      return result;
   }
   
   /**
    * This <code>read</code> method will extract the text value from
    * the node and replace any template variables before converting
    * it to a primitive value. This uses the <code>Context</code>
    * object used for this instance of serialization to replace all
    * template variables with values from the context filter.
    *
    * @param value this is the value to be processed as a template
    * @param type this is the type that that the primitive is
    *
    * @return this returns the primitive that has been deserialized
    */ 
   private Object readTemplate(String value, Class type) throws Exception {
      String text = context.getProperty(value);
      
      if(text != null) {
         return factory.getInstance(text, type);
      }
      return null;
   }  
   
   /**
    * This <code>validate</code> method will validate the primitive 
    * by checking the node text. If the value is a reference then 
    * this will not extract any value from the node. Transformation
    * of the extracted value is not done as it can not account for
    * template variables. Thus any text extracted is valid.
    *
    * @param node this is the node to be validated as a primitive
    *
    * @return this returns the primitive that has been validated
    */ 
   public boolean validate(InputNode node) throws Exception {
      if(node.isElement()) {
         validateElement(node);
      } else {
         node.getValue();
      }
      return true;
   }
   
   /**
    * This <code>validateElement</code> method validates a primitive 
    * by checking the node text. If the value is a reference then 
    * this will not extract any value from the node. Transformation
    * of the extracted value is not done as it can not account for
    * template variables. Thus any text extracted is valid.
    *
    * @param node this is the node to be validated as a primitive
    *
    * @return this returns the primitive that has been validated
    */ 
   private boolean validateElement(InputNode node) throws Exception {
      Instance type = factory.getInstance(node);
      
      if(!type.isReference()) {         
         type.setInstance(null);
      }
      return true;
   }
   
   /**
    * This <code>write</code> method will serialize the contents of
    * the provided object to the given XML element. This will use
    * the <code>String.valueOf</code> method to convert the object to
    * a string if the object represents a primitive, if however the
    * object represents an enumerated type then the text value is
    * created using <code>Enum.name</code>.
    *
    * @param source this is the object to be serialized
    * @param node this is the XML element to have its text set
    */  
   public void write(OutputNode node, Object source) throws Exception {
      String text = factory.getText(source);
    
      if(text != null) {
         node.setValue(text);
      }  
   }
}
