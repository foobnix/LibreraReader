/*
 * TextList.java December 2012
 *
 * Copyright (C) 2012, Niall Gallagher <niallg@users.sf.net>
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

import java.util.Collection;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * This <code>TextList</code> object is a converter that is used
 * to read free text and insert that text in to a list. Collecting
 * free text in this way allows unstructured XML to be processed
 * and stored in an ordered way, which will allow it to be written
 * as identical XML during the serialization process.
 * 
 * @author Niall Gallagher
 */
class TextList implements Repeater {
   
   /**
    * This is the factory that is used to create the containing list.
    */
   private final CollectionFactory factory;
   
   /**
    * This is the primitive object used to read the free text.
    */
   private final Primitive primitive;
   
   /**
    * This is a string type which is used for all free text data.
    */
   private final Type type;
   
   /**
    * Constructor for the <code>TextList</code> object. This is used
    * to create a converter that can read free text from between
    * elements in an <code>ElementListUnion</code>. Converting the
    * free text in this way allows unstructured XML to be processed.
    * 
    * @param context this is the context that is used for this
    * @param list this is the list type this will be used with
    * @param label this is the label this the text is declared with
    */
   public TextList(Context context, Type list, Label label) {
      this.type = new ClassType(String.class);
      this.factory = new CollectionFactory(context, list);
      this.primitive = new Primitive(context, type);
   }

   /**
    * The <code>read</code> method reads an object to a specific type
    * from the provided node. If the node provided is an attribute
    * then the object must be a primitive such as a string, integer,
    * boolean, or any of the other Java primitive types.  
    * 
    * @param node contains the details used to deserialize the object
    * 
    * @return a fully deserialized object will all its fields 
    */
   public Object read(InputNode node) throws Exception {
      Instance value = factory.getInstance(node); 
      Object data = value.getInstance();
      
      if(value.isReference()) {      
         return value.getInstance(); 
      }
      return read(node, data);
   }

   /**
    * The <code>read</code> method reads an object to a specific type
    * from the provided node. If the node provided is an attribute
    * then the object must be a primitive such as a string, integer,
    * boolean, or any of the other Java primitive types.  
    * 
    * @param node contains the details used to deserialize the object
    * @param value this is the value to read the objects in to
    * 
    * @return a fully deserialized object will all its fields 
    */
   public Object read(InputNode node, Object result) throws Exception {
      Collection list = (Collection) result;                 
      Object value = primitive.read(node);
      
      if(value != null) {
         list.add(value);
      }
      return result;
   } 
   
   /**
    * The <code>validate</code> method is used to validate the class
    * XML schema against an input source. This will traverse the class
    * fields and methods ensuring that the input XML document contains
    * a valid structure when compared against the class XML schema.
    * 
    * @param node contains the details used to validate the object
    * 
    * @return true if the document matches the class XML schema 
    */
   public boolean validate(InputNode node) throws Exception {
      return true;
   }
   
   /**
    * The <code>write</code> method writes the fields from the given 
    * object to the XML element. After this has finished the element
    * contains all attributes and sub-elements from the object.
    * 
    * @param object this is the object to be written to the element
    * @param node this is the element that is to be populated
    */
   public void write(OutputNode node, Object object) throws Exception {
      Collection list = (Collection) object;
      OutputNode parent = node.getParent();
      
      for(Object item : list) {
         primitive.write(parent, item);
      }
   }
}