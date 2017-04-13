/*
 * Scanner.java July 2006
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

import java.util.List;

import org.simpleframework.xml.Order;
import org.simpleframework.xml.Version;

/**
 * The <code>Scanner</code> object performs the reflective inspection
 * of a class and builds a map of attributes and elements for each
 * annotated field. This acts as a cachable container for reflection
 * actions performed on a specific type. When scanning the provided
 * class this inserts the scanned field as a <code>Label</code> in to
 * a map so that it can be retrieved by name. Annotations classified
 * as attributes have the <code>Attribute</code> annotation, all other
 * annotated fields are stored as elements.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Schema
 */ 
interface Scanner extends Policy {
   
   /**
    * This is used to acquire the default signature for the class. 
    * The default signature is the signature for the no argument
    * constructor for the type. If there is no default constructor
    * for the type then this will return null.
    * 
    * @return this returns the default signature if it exists
    */
   Signature getSignature();
   
   /**
    * This returns the signatures for the type. All constructors are
    * represented as a signature and returned. More signatures than
    * constructors will be returned if a constructor is annotated 
    * with a union annotation.
    *
    * @return this returns the list of signatures for the type
    */
   List<Signature> getSignatures();
   
   /**
    * This returns a map of all parameters that exist. This is used
    * to validate all the parameters against the field and method
    * annotations that exist within the class. 
    * 
    * @return this returns a map of all parameters within the type
    */
   ParameterMap getParameters();
   
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
    * This is used to acquire the type that this scanner scans for
    * annotations to be used in a schema. Exposing the class that
    * this represents allows the schema it creates to be known.
    * 
    * @return this is the type that this creator will represent
    */
   Class getType();
   
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
    * This method is used to return the <code>Caller</code> for this
    * class. The caller is a means to deliver invocations to the
    * object for the persister callback methods. It aggregates all of
    * the persister callback methods in to a single object.
    * 
    * @return this returns a caller used for delivering callbacks
    */
   Caller getCaller(Context context);

   /**
    * This is used to create a <code>Section</code> given the context
    * used for serialization. A section is an XML structure that 
    * contains all the elements and attributes defined for the class.
    * Each section is a tree like structure defining exactly where
    * each attribute an element is located within the source XML.
    * 
    * @return this will return a section for serialization
    */
   Section getSection();
   
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
    * This is used to acquire the <code>Order</code> annotation for
    * the class schema. The order annotation defines the order that
    * the elements and attributes should appear within the document.
    * Providing order in this manner makes the resulting XML more
    * predictable. If no order is provided, appearance is random.
    * 
    * @return this returns the order, if any, defined for the class
    */
   Order getOrder();
   
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
    * This returns the <code>Label</code> that represents the text
    * annotation for the scanned class. Only a single text annotation
    * can be used per class, so this returns only a single label
    * rather than a <code>LabelMap</code> object. Also if this is
    * not null then the elements label map must be empty.
    * 
    * @return this returns the text label for the scanned class
    */
   Label getText();
   
   /**
    * This returns the name of the class processed by this scanner.
    * The name is either the name as specified in the last found
    * <code>Root</code> annotation, or if a name was not specified
    * within the discovered root then the Java Bean class name of
    * the last class annotated with a root annotation.
    * 
    * @return this returns the name of the object being scanned
    */
   String getName();

   /**
    * This method is used to retrieve the schema class commit method
    * during the deserialization process. The commit method must be
    * marked with the <code>Commit</code> annotation so that when the
    * object is deserialized the persister has a chance to invoke the
    * method so that the object can build further data structures.
    * 
    * @return this returns the commit method for the schema class
    */
   Function getCommit();

   /**
    * This method is used to retrieve the schema class validation
    * method during the deserialization process. The validation method
    * must be marked with the <code>Validate</code> annotation so that
    * when the object is deserialized the persister has a chance to 
    * invoke that method so that object can validate its field values.
    * 
    * @return this returns the validate method for the schema class
    */   
   Function getValidate();
   
   /**
    * This method is used to retrieve the schema class persistence
    * method. This is invoked during the serialization process to
    * get the object a chance to perform an nessecary preparation
    * before the serialization of the object proceeds. The persist
    * method must be marked with the <code>Persist</code> annotation.
    * 
    * @return this returns the persist method for the schema class
    */
   Function getPersist();

   /**
    * This method is used to retrieve the schema class completion
    * method. This is invoked after the serialization process has
    * completed and gives the object a chance to restore its state
    * if the persist method required some alteration or locking.
    * This is marked with the <code>Complete</code> annotation.
    * 
    * @return returns the complete method for the schema class
    */   
   Function getComplete();
   
   /**
    * This method is used to retrieve the schema class replacement
    * method. The replacement method is used to substitute an object
    * that has been deserialized with another object. This allows
    * a seamless delegation mechanism to be implemented. This is
    * marked with the <code>Replace</code> annotation. 
    * 
    * @return returns the replace method for the schema class
    */
   Function getReplace();
   
   /**
    * This method is used to retrieve the schema class replacement
    * method. The replacement method is used to substitute an object
    * that has been deserialized with another object. This allows
    * a seamless delegation mechanism to be implemented. This is
    * marked with the <code>Replace</code> annotation. 
    * 
    * @return returns the replace method for the schema class
    */
   Function getResolve();

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
    * This is used to determine whether the scanned class represents
    * a primitive type. A primitive type is a type that contains no
    * XML annotations and so cannot be serialized with an XML form.
    * Instead primitives a serialized using transformations.
    * 
    * @return this returns true if no XML annotations were found
    */
   boolean isEmpty();
   
   /**
    * This method is used to determine whether strict mappings are
    * required. Strict mapping means that all labels in the class
    * schema must match the XML elements and attributes in the
    * source XML document. When strict mapping is disabled, then
    * XML elements and attributes that do not exist in the schema
    * class will be ignored without breaking the parser.
    *
    * @return true if strict parsing is enabled, false otherwise
    */ 
   boolean isStrict();
}

