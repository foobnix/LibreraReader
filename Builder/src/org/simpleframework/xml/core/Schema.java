/*
 * Schema.java July 2006
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

import org.simpleframework.xml.Version;

/**
 * The <code>Schema</code> object is used to track which fields within
 * an object have been visited by a converter. This object is necessary
 * for processing <code>Composite</code> objects. In particular it is
 * necessary to keep track of which required nodes have been visited 
 * and which have not, if a required not has not been visited then the
 * XML source does not match the XML class schema and serialization
 * must fail before processing any further. 
 * 
 * @author Niall Gallagher
 */ 
interface Schema {
   
   /**
    * This is used to determine whether the scanned class represents
    * a primitive type. A primitive type is a type that contains no
    * XML annotations and so cannot be serialized with an XML form.
    * Instead primitives a serialized using transformations.
    * 
    * @return this returns true if no XML annotations were found
    */
   boolean isPrimitive();
   
   /**
    * This returns the <code>Label</code> that represents the version
    * annotation for the scanned class. Only a single version can
    * exist within the class if more than one exists an exception is
    * thrown. This will read only floating point types such as double.
    * 
    * @return this returns the label used for reading the version
    */
   Label getVersion();
   
   /**
    * This is the <code>Version</code> for the scanned class. It 
    * allows the deserialization process to be configured such that
    * if the version is different from the schema class none of
    * the fields and methods are required and unmatched elements
    * and attributes will be ignored.
    * 
    * @return this returns the version of the class that is scanned
    */
   Version getRevision();
   
   /**
    * This is used to acquire the <code>Decorator</code> for this.
    * A decorator is an object that adds various details to the
    * node without changing the overall structure of the node. For
    * example comments and namespaces can be added to the node with
    * a decorator as they do not affect the deserialization.
    * 
    * @return this returns the decorator associated with this
    */
   Decorator getDecorator();
   
   /**
    * This is used to acquire the instantiator for the type. This is
    * used to create object instances based on the constructors that
    * have been annotated. If no constructors have been annotated
    * then this can be used to create default no argument instances.
    * 
    * @return this instantiator responsible for creating instances
    */
   Instantiator getInstantiator();
   
   /**
    * This is used to acquire the <code>Caller</code> object. This
    * is used to call the callback methods within the object. If the
    * object contains no callback methods then this will return an
    * object that does not invoke any methods that are invoked. 
    * 
    * @return this returns the caller for the specified type
    */
   Caller getCaller();
   
   /**
    * This is used to acquire the <code>Section</code> that defines
    * the XML structure for this class schema. A section, is the 
    * section of XML that the class is represented within. A
    * section contains all the elements and attributes defined for
    * the class in a tree like structure.
    * 
    * @return this returns the section defined for the schama
    */
   Section getSection();
   
   /**
    * This returns the <code>Label</code> that represents the text
    * annotation for the scanned class. Only a single text annotation
    * can be used per class, so this returns only a single label
    * rather than a <code>LabelMap</code> object. Also if this is
    * not null then the elements label map will be empty.
    * 
    * @return this returns the text label for the scanned class
    */
   Label getText();
}
