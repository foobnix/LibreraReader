/*
 * TreeModel.java November 2010
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The <code>TreeModel</code> object is used to build a tree like
 * structure to represent the XML schema for an annotated class. The
 * model is responsible  for building, ordering, and validating all
 * criteria used to represent the class schema. This is immutable
 * to ensure it can be reused many time, in a concurrent environment.
 * Each time attribute and element definitions are requested they
 * are build as new <code>LabelMap</code> objects using a provided
 * context. This ensures the mappings can be styled as required.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Context
 */
class TreeModel implements Model {
  
   /**
    * This is the XPath expression representing the location.
    */
   private Expression expression;
  
   /**
    * This holds the mappings for elements within the model.
    */
   private LabelMap attributes;
   
   /**
    * This holds the mappings for elements within the model.
    */
   private LabelMap elements;
   
   /**
    * This holds the mappings for the models within this instance.
    */
   private ModelMap models;
   
   /**
    * This is used to provide the order of the model elements.
    */
   private OrderList order;
   
   /**
    * This is the serialization policy enforced on this model.
    */
   private Policy policy;
   
   /**
    * This is the type used for reporting validation errors.
    */
   private Detail detail;
   
   /**
    * This must be a valid XML element representing the name.
    */
   private String name;
   
   /**
    * This is used to represent the prefix for this model.
    */
   private String prefix;
   
   /**
    * This is an optional text label used for this model.
    */
   private Label text;
   
   /**
    * This is an optional text label used for this model.
    */
   private Label list;
   
   /**
    * This is the index used to sort similarly named models.
    */
   private int index;
   
   /**
    * Constructor for the <code>TreeModel</code> object. This can be
    * used to register the attributes and elements associated with
    * an annotated class. Also, if there are any path references, 
    * this can contain a tree of models mirroring the XML structure.
    * 
    * @param policy this is the serialization policy enforced
    * @param detail this is the detail associated with this model
    */
   public TreeModel(Policy policy, Detail detail) {
      this(policy, detail, null, null, 1);
   }
   
   /**
    * Constructor for the <code>TreeModel</code> object. This can be
    * used to register the attributes and elements associated with
    * an annotated class. Also, if there are any path references, 
    * this can contain a tree of models mirroring the XML structure.
    * 
    * @param policy this is the serialization policy enforced
    * @param detail this is the detail associated with this model
    * @param name this is the XML element name for this model
    * @param prefix this is the prefix used for this model object
    * @param index this is the index used to order the model
    */
   public TreeModel(Policy policy, Detail detail, String name, String prefix, int index) {
      this.attributes = new LabelMap(policy);
      this.elements = new LabelMap(policy);
      this.models = new ModelMap(detail);
      this.order = new OrderList();
      this.detail = detail;
      this.policy = policy;
      this.prefix = prefix;
      this.index = index;
      this.name = name;
   }
   
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
   public Model lookup(Expression path) {
      String name = path.getFirst();
      int index = path.getIndex();
      Model model = lookup(name, index);
      
      if(path.isPath()) {
         path = path.getPath(1, 0);
         
         if(model != null) {     
            return model.lookup(path);
         }
      }
      return model;   
   }   
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param name this is the name of the element to register
    */   
   public void registerElement(String name) throws Exception {
      if(!order.contains(name)) {
         order.add(name);
      }
      elements.put(name, null);
   }   
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param name this is the name of the element to register
    */
   public void registerAttribute(String name) throws Exception {
      attributes.put(name, null);
   }
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */   
   public void registerText(Label label) throws Exception {
      if(text != null) {
         throw new TextException("Duplicate text annotation on %s", label);
      }
      text = label;
   }
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */   
   public void registerAttribute(Label label) throws Exception {
      String name = label.getName();
      
      if(attributes.get(name) != null) {
         throw new AttributeException("Duplicate annotation of name '%s' on %s", name, label);
      }
      attributes.put(name, label);
   }   
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */   
   public void registerElement(Label label) throws Exception {
      String name = label.getName();
      
      if(elements.get(name) != null) {
         throw new ElementException("Duplicate annotation of name '%s' on %s", name, label);
      }
      if(!order.contains(name)) {
         order.add(name);
      }
      if(label.isTextList()) {
         list = label;
      }
      elements.put(name, label);
   }
   
   /**
    * This is used to build a map from a <code>Context</code> object.
    * Building a map in this way ensures that any style specified by
    * the context can be used to create the XML element and attribute
    * names in the styled format. It also ensures that the model
    * remains immutable as it only provides copies of its data.
    * 
    * @return this returns a map built from the specified context
    */   
   public ModelMap getModels() throws Exception {
      return models.getModels();
   }

   /**
    * This is used to build a map from a <code>Context</code> object.
    * Building a map in this way ensures that any style specified by
    * the context can be used to create the XML element and attribute
    * names in the styled format. It also ensures that the model
    * remains immutable as it only provides copies of its data.
    * 
    * @return this returns a map built from the specified context
    */   
   public LabelMap getAttributes() throws Exception {
      return attributes.getLabels();
   }

   /**
    * This is used to build a map from a <code>Context</code> object.
    * Building a map in this way ensures that any style specified by
    * the context can be used to create the XML element and attribute
    * names in the styled format. It also ensures that the model
    * remains immutable as it only provides copies of its data.
    * 
    * @return this returns a map built from the specified context
    */
   public LabelMap getElements() throws Exception{
      return elements.getLabels();
   }
   
   /**
    * This is used to determine if the provided name represents
    * a model. This is useful when validating the model as it
    * allows determination of a named model, which is an element. 
    * 
    * @param name this is the name of the section to determine
    * 
    * @return this returns true if the model is registered
    */
   public boolean isModel(String name) {
      return models.containsKey(name);
   }

   /**
    * This is used to determine if the provided name represents
    * an element. This is useful when validating the model as 
    * it allows determination of a named XML element. 
    * 
    * @param name this is the name of the section to determine
    * 
    * @return this returns true if the element is registered
    */
   public boolean isElement(String name) {
      return elements.containsKey(name);
   }
   
   /**
    * This is used to determine if the provided name represents
    * an attribute. This is useful when validating the model as 
    * it allows determination of a named XML attribute
    * 
    * @param name this is the name of the attribute to determine
    * 
    * @return this returns true if the attribute is registered
    */
   public boolean isAttribute(String name) {
      return attributes.containsKey(name);
   }
   
   /**
    * This will return the names of all elements contained within
    * the model. This includes the names of all XML elements that
    * have been registered as well as any other models that have
    * been added. Iteration is done in an ordered manner, according
    * to the registration of elements and models.
    * 
    * @return an order list of the elements and models registered
    */
   public Iterator<String> iterator() {
      List<String> list = new ArrayList<String>();
      
      for(String name : order) {
         list.add(name);        
      }
      return list.iterator();      
   }
   
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
   public void validate(Class type) throws Exception {
      validateExpressions(type);
      validateAttributes(type);
      validateElements(type);
      validateModels(type);
      validateText(type);
   }
   
   /**
    * This method is used to validate the model based on whether it 
    * has a text annotation. If this model has a text annotation then
    * it is checked to see if it is a composite model or has any
    * elements. If it has either then the model is considered invalid.
    *
    * @param type this is the object type representing the schema
    */
   private void validateText(Class type) throws Exception {
      if(text != null) {
         if(!elements.isEmpty()) {
            throw new TextException("Text annotation %s used with elements in %s", text, type);
         }
         if(isComposite()) {
            throw new TextException("Text annotation %s can not be used with paths in %s", text, type);
         }
      }
   }
   
   /**
    * This is used to validate the expressions used for each label that
    * this model represents. Each label within a model must have an
    * XPath expression, if the expressions do not match then this will
    * throw an exception. If the model contains no labels then it is
    * considered empty and does not need validation.
    * 
    * @param type this is the object type representing the schema
    */
   private void validateExpressions(Class type) throws Exception {
      for(Label label : elements) {
         if(label != null) {
            validateExpression(label);
         }
      }
      for(Label label : attributes) {
         if(label != null) {
            validateExpression(label);
         }
      }
      if(text != null) {
         validateExpression(text);
      } 
   }
   
   /**
    * This is used to validate the expressions used for a label that
    * this model represents. Each label within a model must have an
    * XPath expression, if the expressions do not match then this will
    * throw an exception. If the model contains no labels then it is
    * considered empty and does not need validation.
    * 
    * @param label this is the object type representing the schema
    */
   private void validateExpression(Label label) throws Exception {
      Expression location = label.getExpression();
      
      if(expression != null) {
         String path = expression.getPath();
         String expect = location.getPath();
         
         if(!path.equals(expect)) {
            throw new PathException("Path '%s' does not match '%s' in %s", path, expect, detail);
         }
      } else {
         expression = location;
      }
   }
   
   /**
    * This is used to validate the models within the instance. This
    * will basically result in validation of the entire tree. Once
    * finished all models contained within the tree will be valid.
    * If any model is invalid an exception will be thrown.
    * <p>
    * To ensure that all ordering and registration of the models
    * is consistent this will check to ensure the indexes of each
    * registered model are in sequence. If they are out of sequence
    * then this will throw an exception.
    * 
    * @param type this is the type this model is created for
    */
   private void validateModels(Class type) throws Exception {
      for(ModelList list : models) {
         int count = 1;
         
         for(Model model : list) {
            if(model != null) {        
               String name = model.getName();
               int index = model.getIndex();
            
               if(index != count++) {
                  throw new ElementException("Path section '%s[%s]' is out of sequence in %s", name, index, type);
               }
               model.validate(type);
            }
         }
      }
   }
   
   /**
    * This is used to validate the individual attributes within the
    * model. Validation is done be acquiring all the attributes and
    * determining if they are null. If they are null this means that
    * an ordering has been imposed on a non-existing attribute.
    * 
    * @param type this is the type this model is created for   
    */
   private void validateAttributes(Class type) throws Exception {
      Set<String> keys = attributes.keySet();
      
      for(String name : keys) {
         Label label = attributes.get(name);
         
         if(label == null) {
            throw new AttributeException("Ordered attribute '%s' does not exist in %s", name, type);
         }
         if(expression != null) {
            expression.getAttribute(name); // prime cache
         }
      }
   }
   
   /**
    * This is used to validate the individual elements within the
    * model. Validation is done be acquiring all the elements and
    * determining if they are null. If they are null this means that
    * an ordering has been imposed on a non-existing element.
    * 
    * @param type this is the type this model is created for   
    */
   private void validateElements(Class type) throws Exception {
      Set<String> keys = elements.keySet();
      
      for(String name : keys) {
         ModelList list = models.get(name);
         Label label = elements.get(name);
         
         if(list == null && label == null) {
            throw new ElementException("Ordered element '%s' does not exist in %s", name, type);
         }
         if(list != null && label != null) {
            if(!list.isEmpty()) {
               throw new ElementException("Element '%s' is also a path name in %s", name, type);
            }
         }
         if(expression != null) {
            expression.getElement(name); // prime cache
         }
      }
   }
   
   /**
    * This is used to register an XML entity within the model. The
    * registration process has the affect of telling the model that
    * it will contain a specific, named, XML entity. It also has 
    * the affect of ordering them within the model, such that the
    * first registered entity is the first iterated over.
    * 
    * @param label this is the label to register with the model
    */
   public void register(Label label) throws Exception {
      if(label.isAttribute()) {
         registerAttribute(label);
      } else if(label.isText()) {
         registerText(label);
      } else {
         registerElement(label);
      }
   }   

   /**
    * This method is used to look for a <code>Model</code> that
    * matches the specified element name. If no such model exists
    * then this will return null. This is used as an alternative
    * to providing an XPath expression to navigate the tree.
    * 
    * @param name this is the name of the model to be acquired
    * 
    * @return this returns the model located by the expression
    */
   public Model lookup(String name, int index) {
      return models.lookup(name, index);
   }

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
   public Model register(String name, String prefix, int index) throws Exception {
      Model model = models.lookup(name, index);
      
      if (model == null) {
         return create(name, prefix, index);
      }
      return model;
   }
   
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
   private Model create(String name, String prefix, int index) throws Exception {
      Model model = new TreeModel(policy, detail, name, prefix, index);
      
      if(name != null) {
         models.register(name, model);
         order.add(name);
      }
      return model;
   }
   
   /**
    * This is used to perform a recursive search of the models that
    * have been registered, if a model has elements or attributes
    * then this returns true. If however no other model contains 
    * any attributes or elements then this will return false.
    * 
    * @return true if any model has elements or attributes
    */
   public boolean isComposite() {
      for(ModelList list : models) {
         for(Model model : list) {
            if(model != null) {
               if(!model.isEmpty()) {
                  return true;
               }
            }
         }
      }
      return !models.isEmpty();
   }
   
   /**
    * Used to determine if a model is empty. A model is considered
    * empty if that model does not contain any registered elements
    * or attributes. However, if the model contains other models
    * that have registered elements or attributes it is not empty.
    * 
    * @return true if the model does not contain registrations
    */
   public boolean isEmpty() {
      if(text != null) {
         return false;
      }
      if(!elements.isEmpty()) {
         return false;
      }
      if(!attributes.isEmpty()) {
         return false;
      }
      return !isComposite();
   }
   
   /**
    * This returns a text label if one is associated with the model.
    * If the model does not contain a text label then this method
    * will return null. Any model with a text label should not be
    * composite and should not contain any elements.
    * 
    * @return this is the optional text label for this model
    */
   public Label getText() {
      if(list != null) {
         return list;
      }
      return text;
   }
   
   /**
    * This returns an <code>Expression</code> representing the path
    * this model exists at within the class schema. This should 
    * never be null for any model that is not empty.
    * 
    * @return this returns the expression associated with this
    */
   public Expression getExpression() {
      return expression;
   }
   
   /**
    * This is used to acquire the path prefix for the model. The
    * path prefix is used when the model is transformed in to an
    * XML structure. This ensures that the XML element created to
    * represent the model contains the optional prefix.
    * 
    * @return this returns the prefix for this model
    */
   public String getPrefix() {
      return prefix;
   }
   
   /**
    * This is used to return the name of the model. The name is 
    * must be a valid XML element name. It is used when a style
    * is applied to a section as the model name must be styled.
    * 
    * @return this returns the name of this model instance
    */
   public String getName() {
      return name;
   }
   
   /**
    * This method is used to return the index of the model. The
    * index is the order that this model appears within the XML
    * document. Having an index allows multiple models of the
    * same name to be inserted in to a sorted collection.
    * 
    * @return this is the index of this model instance
    */
   public int getIndex() {
      return index;
   }
   
   /**
    * For the purposes of debugging we provide a representation
    * of the model in a string format. This will basically show
    * the name of the model and the index it exists at.
    * 
    * @return this returns some details for the model
    */
   public String toString() {
      return String.format("model '%s[%s]'", name, index);
   }
   
   /**
    * The <code>OrderList</code> object is used to maintain the order
    * of the XML elements within the model. Elements are either 
    * other models or element <code>Label</code> objects that are
    * annotated fields or methods. Maintaining order is important     
    * 
    * @author Niall Gallagher
    */
   private static class OrderList extends ArrayList<String> {
      
      /**
       * Constructor for the <code>OrderList</code> object. This is
       * basically a typedef of sorts that hides the ugly generic
       * details from the class definition.        
       */
      public OrderList() {
         super();
      }
   }
}
