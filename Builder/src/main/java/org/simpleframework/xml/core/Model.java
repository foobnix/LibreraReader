/*
 * Model.java November 2010
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

/**
 * The <code>Model</code> interface represents the core data structure
 * used for representing an XML schema. This is effectively a tree
 * like structure in that it can contain other models as well as XML
 * attributes and elements. Each model represents a context within an
 * XML document, each context is navigated to with an XPath expression.
 * <p>
 * The model is responsible for building the element and attribute
 * labels used to read and write and also to ensure the correct order
 * of the XML elements and attributes is enforced. Once the model has
 * been completed it can then be validated to ensure its contents 
 * represent a valid XML structure.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Section
 */
interface Model extends Iterable<String> {
   
   /**
    * Used to determine if a model is empty. A model is considered
    * empty if that model does not contain any registered elements
    * or attributes. However, if the model contains other models
    * that have registered elements or attributes it is not empty.
    * 
    * @return true if the model does not contain registrations
    */
   boolean isEmpty();
   
   /**
    * This is used to determine if the provided name represents
    * a model. This is useful when validating the model as it
    * allows determination of a named model, which is an element. 
    * 
    * @param name this is the name of the section to determine
    * 
    * @return this returns true if the model is registered
    */
   boolean isModel(String name);
   
   /**
    * This is used to determine if the provided name represents
    * an element. This is useful when validating the model as 
    * it allows determination of a named XML element. 
    * 
    * @param name this is the name of the section to determine
    * 
    * @return this returns true if the element is registered
    */
   boolean isElement(String name);
   
   /**
    * This is used to determine if the provided name represents
    * an attribute. This is useful when validating the model as 
    * it allows determination of a named XML attribute
    * 
    * @param name this is the name of the attribute to determine
    * 
    * @return this returns true if the attribute is registered
    */
   boolean isAttribute(String name);
   
   /**
    * This is used to perform a recursive search of the models that
    * have been registered, if a model has elements or attributes
    * then this returns true. If however no other model contains 
    * any attributes or elements then this will return false.
    * 
    * @return true if any model has elements or attributes
    */
   boolean isComposite();
   
   /**
    * This is used to validate the model to ensure all elements and
    * attributes are valid. Validation also ensures that any order
    * specified by an annotated class did not contain invalid XPath
    * values, or redundant elements and attributes.
    * 
    * @param type this is the object type representing the schema
    * 
    * @throws Exception if text and element annotations are present
    */
   void validate(Class type) throws Exception;
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */
   void register(Label label) throws Exception;
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */
   void registerText(Label label) throws Exception;
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */
   void registerElement(Label label) throws Exception;
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */
   void registerAttribute(Label label) throws Exception;
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param name this is the name of the element to register
    */
   void registerElement(String name) throws Exception;
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param name this is the name of the element to register
    */
   void registerAttribute(String name) throws Exception;
   
   /**
    * This is used to register a <code>Model</code> within this
    * model. Registration of a model creates a tree of models that
    * can be used to represent an XML structure. Each model can
    * contain elements and attributes associated with a type.
    * 
    * @param name this is the name of the model to be registered
    * @param prefix this is the prefix used for this model
    * @param index this is the index used to order the model
    * 
    * @return this returns the model that was registered
    */
   Model register(String name, String prefix, int index) throws Exception;   
   
   /**
    * This method is used to look for a <code>Model</code> that
    * matches the specified element name. If no such model exists
    * then this will return null. This is used as an alternative
    * to providing an XPath expression to navigate the tree.
    * 
    * @param name this is the name of the model to be acquired
    * @param index this is the index used to order the model
    * 
    * @return this returns the model located by the expression
    */
   Model lookup(String name, int index);
   
   /**
    * This method is used to look for a <code>Model</code> that
    * matches the specified expression. If no such model exists
    * then this will return null. Using an XPath expression allows
    * a tree like structure to be navigated with ease.
    * 
    * @param path an XPath expression used to locate a model
    * 
    * @return this returns the model located by the expression
    */
   Model lookup(Expression path);
   
   /**
    * This is used to build a map from a <code>Context</code> object.
    * Building a map in this way ensures that any style specified by
    * the context can be used to create the XML element and attribute
    * names in the styled format. It also ensures that the model
    * remains immutable as it only provides copies of its data.
    * 
    * @return this returns a map built from the specified context
    */
   LabelMap getElements() throws Exception;
   
   /**
    * This is used to build a map from a <code>Context</code> object.
    * Building a map in this way ensures that any style specified by
    * the context can be used to create the XML element and attribute
    * names in the styled format. It also ensures that the model
    * remains immutable as it only provides copies of its data.
    * 
    * @return this returns a map built from the specified context
    */
   LabelMap getAttributes() throws Exception;
   
   /**
    * This is used to build a map from a <code>Context</code> object.
    * Building a map in this way ensures that any style specified by
    * the context can be used to create the XML element and attribute
    * names in the styled format. It also ensures that the model
    * remains immutable as it only provides copies of its data.
    * 
    * @return this returns a map built from the specified context
    */
   ModelMap getModels() throws Exception;
   
   /**
    * This returns a text label if one is associated with the model.
    * If the model does not contain a text label then this method
    * will return null. Any model with a text label should not be
    * composite and should not contain any elements.
    * 
    * @return this is the optional text label for this model
    */
   Label getText();
   
   /**
    * This returns an <code>Expression</code> representing the path
    * this model exists at within the class schema. This should 
    * never be null for any model that is not empty.
    * 
    * @return this returns the expression associated with this
    */
   Expression getExpression();
   
   /**
    * This is used to acquire the path prefix for the model. The
    * path prefix is used when the model is transformed in to an
    * XML structure. This ensures that the XML element created to
    * represent the model contains the optional prefix.
    * 
    * @return this returns the prefix for this model
    */
   String getPrefix();
   
   /**
    * This is used to return the name of the model. The name is 
    * must be a valid XML element name. It is used when a style
    * is applied to a section as the model name must be styled.
    * 
    * @return this returns the name of this model instance
    */
   String getName();   

   /**
    * This method is used to return the index of the model. The
    * index is the order that this model appears within the XML
    * document. Having an index allows multiple models of the
    * same name to be inserted in to a sorted collection.
    * 
    * @return this is the index of this model instance
    */
   int getIndex();
}