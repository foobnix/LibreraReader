/*
 * Structure.java November 2010
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

package org.simpleframework.xml.core;

import org.simpleframework.xml.Version;

/**
 * The <code>Structure</code> object represents the XML structure of
 * an annotated class schema. A structure instance is an immutable
 * object that contains all the criteria used in reading and writing
 * and object. It provides a <code>Section</code> representing a tree
 * structure of elements and attributes. Each section returned by
 * the structure is a styled copy of the structured contents.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.StructureBuilder
 * @see org.simpleframework.xml.core.Section
 */
class Structure {
   
   /**
    * This is the instantiator that is used to create instances.
    */
   private final Instantiator factory;
   
   /**
    * This is the label representing the version of the class.
    */
   private final Label version;
   
   /**
    * This is contains any text field or method in the class.
    */
   private final Label text;
   
   /**
    * This contains the tree of XML elements and attributes used.
    */
   private final Model model;
   
   /**
    * This is used to determine if the structure is a primitive.
    */
   private final boolean primitive;
   
   /**
    * Constructor for the <code>Structure</code> object. A structure
    * is created using all the established criteria for a schema
    * that represents an annotated class. Once created the structure
    * is immutable and is used to build XML sections.
    * 
    * @param factory this is used to create new object instances
    * @param model the model representing the tree of XML elements
    * @param version this is the version associated with the class
    * @param text this represents any text field or method
    * @param primitive used to determine if this is primitive
    */
   public Structure(Instantiator factory, Model model, Label version, Label text, boolean primitive) {
      this.primitive = primitive;
      this.factory = factory;
      this.version = version;
      this.model = model;
      this.text = text;      
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
    * This is used to acquire the <code>Section</code> representing
    * the class schema. A section is a tree like XML structure that
    * contains all the details of the attributes and elements that
    * form a section of the schema class. The context is provided 
    * to that the names can be styled if required. 
    * 
    * @return a section representing an XML section
    */
   public Section getSection() {
      return new ModelSection(model);
   }
   
   /**
    * This is used to determine whether the schema class represents
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
    * This is the <code>Version</code> for the scanned class. It 
    * allows the deserialization process to be configured such that
    * if the version is different from the schema class none of
    * the fields and methods are required and unmatched elements
    * and attributes will be ignored.
    * 
    * @return this returns the version of the schema class
    */
   public Version getRevision() {
      if(version != null) {
         Contact contact = version.getContact();
         return contact.getAnnotation(Version.class);
      }
      return null;
   }
   
   
   /**
    * This returns the <code>Label</code> that represents the version
    * annotation for the schema class. Only a single version can
    * exist within the class if more than one exists an exception is
    * thrown. This will read only floating point types such as double.
    * 
    * @return this returns the label used for reading the version
    */
   public Label getVersion() {
      return version;
   }
   
   /**
    * This returns the <code>Label</code> that represents the text
    * annotation for the schema class. Only a single text annotation
    * can be used per class, so this returns only a single label
    * rather than a <code>LabelMap</code> object. Also if this is
    * not null then the section returned must be empty.
    * 
    * @return this returns the text label for the schema class
    */
   public Label getText() {
      return text;
   }
}
