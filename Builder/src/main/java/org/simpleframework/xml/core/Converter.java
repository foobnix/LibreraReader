/*
 * Converter.java July 2006
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

import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * The <code>Converter</code> object serializes and deserializes XML
 * elements. Serialization of lists, primitives, and compound types 
 * are performed using a converter. Any object read from a converter
 * will produce a fully deserialized object will all its fields. 
 * The objects written to an XML element populate that element with 
 * attributes an elements according to the objects annotations.
 * 
 * @author Niall Gallagher
 */
interface Converter {

   /**
    * The <code>read</code> method reads an object to a specific type
    * from the provided node. If the node provided is an attribute
    * then the object must be a primitive such as a string, integer,
    * boolean, or any of the other Java primitive types.  
    * 
    * @param node contains the details used to deserialize the object
    * 
    * @return a fully deserialized object will all its fields 
    * 
    * @throws Exception if a deserialized type cannot be instantiated
    */
   Object read(InputNode node) throws Exception; 
   
   /**
    * The <code>read</code> method reads an object to a specific type
    * from the provided node. If the node provided is an attribute
    * then the object must be a primitive such as a string, integer,
    * boolean, or any of the other Java primitive types.  
    * 
    * @param node contains the details used to deserialize the object
    * @param value this is an existing value to deserialize in to
    * 
    * @return a fully deserialized object will all its fields 
    * 
    * @throws Exception if a deserialized type cannot be instantiated
    */
   Object read(InputNode node, Object value) throws Exception; 
   
   /**
    * The <code>validate</code> method is used to validate the class
    * XML schema against an input source. This will traverse the class
    * fields and methods ensuring that the input XML document contains
    * a valid structure when compared against the class XML schema.
    * 
    * @param node contains the details used to validate the object
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   boolean validate(InputNode node) throws Exception;
   
   /**
    * The <code>write</code> method writes the fields from the given 
    * object to the XML element. After this has finished the element
    * contains all attributes and sub-elements from the object.
    * 
    * @param object this is the object to be written to the element
    * @param node this is the element that is to be populated
    * 
    * @throws Exception throw if the object cannot be serialized
    */
   void write(OutputNode node, Object object) throws Exception;
}
