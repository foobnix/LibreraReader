/*
 * Converter.java January 2010
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

import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * The <code>Converter</code> object is used to convert an object
 * to XML by intercepting the normal serialization process. When
 * serializing an object the <code>write</code> method is invoked.
 * This is provided with the object instance to be serialized and
 * the <code>OutputNode</code> to use to write the XML. Values
 * can be taken from the instance and transferred to the node.
 * <p>
 * For deserialization the <code>read</code> method is invoked.
 * This is provided with the <code>InputNode</code>, which can be
 * used to read the elements and attributes representing the 
 * member data of the object being deserialized. Once the object
 * has been instantiated it must be returned. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.convert.AnnotationStrategy
 * @see org.simpleframework.xml.convert.RegistryStrategy
 */
public interface Converter<T> {

   /**
    * This <code>read</code> method is used to deserialize an object
    * from the source XML. The deserialization is performed using 
    * the XML node provided. This node can be used to read the XML
    * elements and attributes in any format required. Once all of 
    * the data has been extracted an instance must be returned.
    * 
    * @param node this is the node to deserialize the object from
    * 
    * @return the object instance resulting from the deserialization
    */
   T read(InputNode node) throws Exception; 

   /**
    * This <code>write</code> method is used to serialize an object
    * to XML. The serialization should be performed in such a way
    * that all of the objects values are represented by an element
    * or attribute of the provided node. This ensures that it can
    * be fully deserialized at a later time.
    * 
    * @param node this is the node to serialized to object to
    * @param value this is the value that is to be serialized
    */
   void write(OutputNode node, T value) throws Exception;
}