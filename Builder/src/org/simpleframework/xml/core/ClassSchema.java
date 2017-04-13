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
class ClassSchema implements Schema {
   
   /**
    * This is the instantiator used to create all object instances.
    */
   private final Instantiator factory;
   
   /**
    * This is the decorator associated with this schema object.
    */
   private final Decorator decorator;

   /**
    * This represents the XML section defined for the class schema.
    */
   private final Section section;
   
   /**
    * This is the version annotation for the XML class schema.
    */
   private final Version revision;
   
   /**
    * This is the pointer to the schema class replace method.
    */
   private final Caller caller;
   
   /**
    * This is the version label used to read the version attribute.
    */
   private final Label version;
   
   /**
    * This is used to represent a text value within the schema.
    */
   private final Label text;
   
   /**
    * This is the type that this class schema is representing.
    */
   private final Class type;
   
   /**
    * This is used to specify whether the type is a primitive class.
    */
   private final boolean primitive;

   /**
    * Constructor for the <code>Schema</code> object. This is used 
    * to wrap the element and attribute XML annotations scanned from
    * a class schema. The schema tracks all fields visited so that
    * a converter can determine if all fields have been serialized.
    * 
    * @param schema this contains all labels scanned from the class
    * @param context this is the context object for serialization
    */
   public ClassSchema(Scanner schema, Context context) throws Exception {  
      this.caller = schema.getCaller(context);
      this.factory = schema.getInstantiator();
      this.revision = schema.getRevision();
      this.decorator = schema.getDecorator();
      this.primitive = schema.isPrimitive();
      this.version = schema.getVersion();
      this.section = schema.getSection();
      this.text = schema.getText();
      this.type = schema.getType();
   }
   
   /**
    * This is used to determine whether the scanned class represents
    * a primitive type. A primitive type is a type that contains no
    * XML annotations and so cannot be serialized with an XML form.
    * Instead primitives a serialized using transformations.
    * 
    * @return this returns true if no XML annotations were found
    */
   public boolean isPrimitive() {
      return primitive;
   }
   
   /**
    * This is used to acquire the instantiator for the type. This is
    * used to create object instances based on the constructors that
    * have been annotated. If no constructors have been annotated
    * then this can be used to create default no argument instances.
    * 
    * @return this instantiator responsible for creating instances
    */
   public Instantiator getInstantiator() {
      return factory;
   }
   
   /**
    * This returns the <code>Label</code> that represents the version
    * annotation for the scanned class. Only a single version can
    * exist within the class if more than one exists an exception is
    * thrown. This will read only floating point types such as double.
    * 
    * @return this returns the label used for reading the version
    */
   public Label getVersion() {
      return version;
   }
   
   /**
    * This is the <code>Version</code> for the scanned class. It 
    * allows the deserialization process to be configured such that
    * if the version is different from the schema class none of
    * the fields and methods are required and unmatched elements
    * and attributes will be ignored.
    * 
    * @return this returns the version of the class that is scanned
    */
   public Version getRevision() {
      return revision;
   }
   
   /**
    * This is used to acquire the <code>Decorator</code> for this.
    * A decorator is an object that adds various details to the
    * node without changing the overall structure of the node. For
    * example comments and namespaces can be added to the node with
    * a decorator as they do not affect the deserialization.
    * 
    * @return this returns the decorator associated with this
    */
   public Decorator getDecorator() {
      return decorator;
   }
   
   /**
    * This is used to acquire the <code>Caller</code> object. This
    * is used to call the callback methods within the object. If the
    * object contains no callback methods then this will return an
    * object that does not invoke any methods that are invoked. 
    * 
    * @return this returns the caller for the specified type
    */
   public Caller getCaller() {
      return caller;
   }
   
   /**
    * This is used to acquire the <code>Section</code> that defines
    * the XML structure for this class schema. A section, is the 
    * section of XML that the class is represented within. A
    * section contains all the elements and attributes defined for
    * the class in a tree like structure.
    * 
    * @return this returns the section defined for the schama
    */
   public Section getSection() {
      return section;
   }
   
   /**
    * This returns the <code>Label</code> that represents the text
    * annotation for the scanned class. Only a single text annotation
    * can be used per class, so this returns only a single label
    * rather than a <code>LabelMap</code> object. Also if this is
    * not null then the elements label map will be empty.
    * 
    * @return this returns the text label for the scanned class
    */
   public Label getText() {
      return text;
   }
   
   /**
    * This is used to acquire a description of the schema. This is
    * useful when debugging an issue as it allows a representation
    * of the instance to be viewed with the class it represents.
    * 
    * @return this returns a visible description of the schema
    */
   public String toString() {
      return String.format("schema for %s", type);
   }
}
