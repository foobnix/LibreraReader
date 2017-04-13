/*
 * Repeater.java July 2007
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

/**
 * The <code>Repeater</code> interface is used to for converters that
 * can repeat a read on a given element. This is typically used for
 * inline lists and maps so that the elements can be mixed within the
 * containing element. This ensures a more liberal means of writing
 * the XML such that elements not grouped in a containing XML element
 * can be declared throughout the document.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.CompositeInlineMap
 */
interface Repeater extends Converter {
   
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
    * 
    * @throws Exception if a deserialized type cannot be instantiated
    */
   Object read(InputNode node, Object value) throws Exception;

}
