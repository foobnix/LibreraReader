/*
 * StructureBuilder.java November 2010
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

import java.lang.annotation.Annotation;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.ElementMapUnion;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Order;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.Version;
import org.simpleframework.xml.strategy.Type;

/**
 * The <code>StructureBuilder</code> object is used to build the XML
 * structure of an annotated class. Once all the information scanned
 * from the class has been collected a <code>Structure</code> object
 * can be built using this object. The structure instance will 
 * contain relevant information regarding the class schema.
 * <p>
 * This builder exposes several methods, which are invoked in a
 * sequence by the <code>Scanner</code> object. In particular there
 * is a <code>process</code> method which is used to consume the
 * annotated fields and methods. With the annotations it then builds
 * the underlying structure representing the class schema.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Scanner
 */
class StructureBuilder {

   /**
    * This is used to build an instantiator for creating objects.
    */
   private InstantiatorBuilder resolver;

   /**
    * This is used to build XPath expressions from annotations.
    */
   private ExpressionBuilder builder;
   
   /**
    * This is used to perform the initial ordered registrations. 
    */
   private ModelAssembler assembler;
   
   /**
    * This is the instantiator that is used to create instances.
    */
   private Instantiator factory;
   
   /**
    * For validation all attributes must be stored in the builder.
    */
   private LabelMap attributes;
   
   /**
    * For validation all elements must be stored in the builder.
    */
   private LabelMap elements;
 
   /**
    * This is used to maintain the text labels for the class.
    */
   private LabelMap texts;
   
   /**
    * This is the source scanner that is used to scan the class.
    */
   private Scanner scanner;
   
   /**
    * This object contains various support functions for the class.
    */
   private Support support;
   
   /**
    * This is the version annotation extracted from the class.
    */
   private Label version;
   
   /**
    * This represents a text annotation extracted from the class.
    */
   private Label text;
   
   /**
    * This the core model used to represent the XML structure.
    */
   private Model root;
   
   /**
    * This is used to determine if the scanned class is primitive.
    */
   private boolean primitive;

   /**
    * Constructor for the <code>StructureBuilder</code> object. This
    * is used to process all the annotations for a class schema and
    * build a hierarchical model representing the required structure.
    * Once the structure has been built then it is validated to
    * ensure that all elements and attributes exist.
    * 
    * @param scanner this is the scanner used to scan annotations
    * @param type this is the type that is being scanned
    * @param format this is the format used to style the XML
    */
   public StructureBuilder(Scanner scanner, Detail detail, Support support) throws Exception {
      this.builder = new ExpressionBuilder(detail, support);
      this.assembler = new ModelAssembler(builder, detail, support);
      this.resolver = new InstantiatorBuilder(scanner, detail);
      this.root = new TreeModel(scanner, detail);
      this.attributes = new LabelMap(scanner);
      this.elements = new LabelMap(scanner);
      this.texts = new LabelMap(scanner);
      this.scanner = scanner;
      this.support = support;
   }   
   
   /**
    * This is used to acquire the optional order annotation to provide
    * order to the elements and attributes for the generated XML. This
    * acts as an override to the order provided by the declaration of
    * the types within the object.  
    * 
    * @param type this is the type to be scanned for the order
    */
   public void assemble(Class type) throws Exception {
      Order order = scanner.getOrder();
      
      if(order != null) {
         assembler.assemble(root, order);
      }
   }
   
   /**
    * This reflectively checks the annotation to determine the type 
    * of annotation it represents. If it represents an XML schema
    * annotation it is used to create a <code>Label</code> which can
    * be used to represent the field within the context object.
    * 
    * @param field the field that the annotation comes from
    * @param label the annotation used to model the XML schema
    * 
    * @throws Exception if there is more than one text annotation
    */   
   public void process(Contact field, Annotation label) throws Exception {
      if(label instanceof Attribute) {
         process(field, label, attributes);
      }
      if(label instanceof ElementUnion) {
         union(field, label, elements);
      }
      if(label instanceof ElementListUnion) {
         union(field, label, elements);
      }
      if(label instanceof ElementMapUnion) {
         union(field, label, elements);
      }
      if(label instanceof ElementList) {
         process(field, label, elements);
      }
      if(label instanceof ElementArray) {
         process(field, label, elements);
      }
      if(label instanceof ElementMap) {
         process(field, label, elements);
      }
      if(label instanceof Element) {
         process(field, label, elements);
      }    
      if(label instanceof Version) {
         version(field, label);
      }
      if(label instanceof Text) {
         text(field, label);
      }
   }   
   
   /**
    * This is used when all details from a field have been gathered 
    * and a <code>Label</code> implementation needs to be created. 
    * This will build a label instance based on the field annotation.
    * If a label with the same name was already inserted then it is
    * ignored and the value for that field will not be serialized. 
    * 
    * @param field the field the annotation was extracted from
    * @param type the annotation extracted from the field
    * @param map this is used to collect the label instance created
    * 
    * @throws Exception thrown if the label can not be created
    */   
   private void union(Contact field, Annotation type, LabelMap map) throws Exception {
      List<Label> list = support.getLabels(field, type);
      
      for(Label label : list) {
         String path = label.getPath();
         String name = label.getName();
         
         if(map.get(path) != null) {
            throw new PersistenceException("Duplicate annotation of name '%s' on %s", name, label);
         }
         process(field, label, map);
      }
   }
   
   /**
    * This is used when all details from a field have been gathered 
    * and a <code>Label</code> implementation needs to be created. 
    * This will build a label instance based on the field annotation.
    * If a label with the same name was already inserted then it is
    * ignored and the value for that field will not be serialized. 
    * 
    * @param field the field the annotation was extracted from
    * @param type the annotation extracted from the field
    * @param map this is used to collect the label instance created
    * 
    * @throws Exception thrown if the label can not be created
    */   
   private void process(Contact field, Annotation type, LabelMap map) throws Exception {
      Label label = support.getLabel(field, type);
      String path = label.getPath();
      String name = label.getName();
      
      if(map.get(path) != null) {
         throw new PersistenceException("Duplicate annotation of name '%s' on %s", name, field);
      }
      process(field, label, map);
   }
   
   /**
    * This is used when all details from a field have been gathered 
    * and a <code>Label</code> implementation needs to be created. 
    * This will build a label instance based on the field annotation.
    * If a label with the same name was already inserted then it is
    * ignored and the value for that field will not be serialized. 
    * 
    * @param field the field the annotation was extracted from
    * @param label this is the label representing a field or method
    * @param map this is used to collect the label instance created
    * 
    * @throws Exception thrown if the label can not be created
    */
   private void process(Contact field, Label label, LabelMap map) throws Exception {
      Expression expression = label.getExpression();
      String path = label.getPath();
      Model model = root;
      
      if(!expression.isEmpty()) {
         model = register(expression);
      }
      resolver.register(label);
      model.register(label);      
      map.put(path, label);
   }   
   
   /**
    * This is used to process the <code>Text</code> annotations that
    * are present in the scanned class. This will set the text label
    * for the class and an ensure that if there is more than one
    * text label within the class an exception is thrown.
    * 
    * @param field the field the annotation was extracted from
    * @param type the annotation extracted from the field
    * 
    * @throws Exception if there is more than one text annotation
    */   
   private void text(Contact field, Annotation type) throws Exception {
      Label label = support.getLabel(field, type);
      Expression expression = label.getExpression();
      String path = label.getPath();
      Model model = root;
      
      if(!expression.isEmpty()) {
         model = register(expression);
      }
      if(texts.get(path) != null) {
         throw new TextException("Multiple text annotations in %s", type);
      }
      resolver.register(label);
      model.register(label);
      texts.put(path, label);
   }
   
   /**
    * This is used to process the <code>Text</code> annotations that
    * are present in the scanned class. This will set the text label
    * for the class and an ensure that if there is more than one
    * text label within the class an exception is thrown.
    * 
    * @param field the field the annotation was extracted from
    * @param type the annotation extracted from the field
    * 
    * @throws Exception if there is more than one text annotation
    */   
   private void version(Contact field, Annotation type) throws Exception {
      Label label = support.getLabel(field, type);
      
      if(version != null) {
         throw new AttributeException("Multiple version annotations in %s", type);
      }
      version = label;
   }

   /**
    * This is used to build the <code>Structure</code> that has been
    * built. The structure will contain all the details required to
    * serialize and deserialize the type. Once created the structure
    * is immutable, and can be used to create <code>Section</code>
    * objects, which contains the element and attribute details.
    * 
    * @param type this is the type that represents the schema class
    * 
    * @return this returns the structure that has been built
    */
   public Structure build(Class type) throws Exception {
      return new Structure(factory, root, version, text, primitive);
   }
   
   /**
    * This is used to determine if the specified XPath expression
    * represents an element within the root model. This will return
    * true if the specified path exists as either an element or
    * as a valid path to an existing model.
    * <p>
    * If the path references a <code>Model</code> then that is an
    * element only if it is not empty. If the model is empty this
    * means that it was used in the <code>Order</code> annotation
    * only and this does not refer to a value XML element.
    * 
    * @param path this is the path to search for the element
    * 
    * @return this returns true if an element or model exists
    */
   private boolean isElement(String path)throws Exception {
      Expression target = builder.build(path);
      Model model = lookup(target);
      
      if(model != null) {
         String name = target.getLast();
         int index = target.getIndex();
         
         if(model.isElement(name)) {
            return true;
         }
         if(model.isModel(name)) {
            Model element = model.lookup(name, index);
            
            if(element.isEmpty()) {
               return false;
            }
            return true;
         }
      }
      return false;
   }
   
   /**
    * This is used to determine if the specified XPath expression
    * represents an attribute within the root model. This returns
    * true if the specified path exists as either an attribute.
    * 
    * @param path this is the path to search for the attribute
    * 
    * @return this returns true if the attribute exists
    */
   private boolean isAttribute(String path) throws Exception {
      Expression target = builder.build(path);
      Model model = lookup(target);
      
      if(model != null) { 
         String name = target.getLast();
         
         if(!target.isPath()) {
            return model.isAttribute(path);
         }
         return model.isAttribute(name);
      }
      return false;
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
   private Model lookup(Expression path) throws Exception {
      Expression target = path.getPath(0, 1);
      
      if(path.isPath()) {
         return root.lookup(target);
      }
      return root;
   }   

   /**
    * This is used to register a <code>Model</code> for this builder.
    * Registration of a model creates a tree of models that is used 
    * to represent an XML structure. Each model can contain elements 
    * and attributes associated with a type.
    * 
    * @param path this is the path of the model to be resolved
    * 
    * @return this returns the model that was registered
    */
   private Model register(Expression path) throws Exception {   
      Model model = root.lookup(path);
      
      if (model != null) {
         return model;
      }      
      return create(path);
   }
   
   /**
    * This is used to register a <code>Model</code> for this builder.
    * Registration of a model creates a tree of models that is used 
    * to represent an XML structure. Each model can contain elements 
    * and attributes associated with a type.
    * 
    * @param path this is the path of the model to be resolved
    * 
    * @return this returns the model that was registered
    */
   private Model create(Expression path) throws Exception {
      Model model = root;
   
      while(model != null) {
         String prefix = path.getPrefix();
         String name = path.getFirst();
         int index = path.getIndex();

         if(name != null) {
            model = model.register(name, prefix, index);
         }
         if(!path.isPath()) {
            break;
         }
         path = path.getPath(1);
      }
      return model;
   }
   
   /**
    * This is used to commit the structure of the type. This will
    * build an <code>Instantiator</code> that can be used to create
    * instances using annotated constructors. If no constructors have
    * be annotated then instances can use the default constructor.
    * 
    * @param type this is the type that this builder creates
    */
   public void commit(Class type) throws Exception {
      if(factory == null) {
         factory = resolver.build();
      }
   }
   
   /**
    * This is used to validate the configuration of the scanned class.
    * If a <code>Text</code> annotation has been used with elements
    * then validation will fail and an exception will be thrown. 
    * 
    * @param type this is the object type that is being scanned
    */
   public void validate(Class type) throws Exception {
      Order order = scanner.getOrder();
      
      validateUnions(type);
      validateElements(type, order);
      validateAttributes(type, order);
      validateModel(type);
      validateText(type);  
      validateTextList(type);
   }
   
   /**
    * This is used to validate the model to ensure all elements and
    * attributes are valid. Validation also ensures that any order
    * specified by an annotated class did not contain invalid XPath
    * values, or redundant elements and attributes.
    * 
    * @param type this is the object type representing the schema
    */
   private void validateModel(Class type) throws Exception {
      if(!root.isEmpty()) {
         root.validate(type);
      }
   }

   /**
    * This is used to validate the configuration of the scanned class.
    * If a <code>Text</code> annotation has been used with elements
    * then validation will fail and an exception will be thrown. 
    * 
    * @param type this is the object type that is being scanned
    */
   private void validateText(Class type) throws Exception {
   	Label label = root.getText();
   	
      if(label != null) {
         if(!label.isTextList()) {
            if(!elements.isEmpty()) {
               throw new TextException("Elements used with %s in %s", label, type);
            }
            if(root.isComposite()) {
               throw new TextException("Paths used with %s in %s", label, type);
            }
         }
      }  else {
         if(scanner.isEmpty()) {
            primitive = isEmpty();
         }
      }
   }
   
   /**
    * This is used to validate the configuration of the scanned class.
    * If an <code>ElementListUnion</code> annotation has been used with 
    * a <code>Text</code> annotation this validates to ensure there are
    * no other elements declared and no <code>Path</code> annotations 
    * have been used, which ensures free text can be processed.
    * 
    * @param type this is the object type that is being scanned
    */
   private void validateTextList(Class type) throws Exception {
      Label label = root.getText();
      
      if(label != null) {
         if(label.isTextList()) {
            Object key = label.getKey();
            
            for(Label element : elements) {
               Object identity = element.getKey();
               
               if(!identity.equals(key)) {
                  throw new TextException("Elements used with %s in %s", label, type);
               }
               Type dependent = element.getDependent();
               Class actual = dependent.getType();
               
               if(actual == String.class) {
                  throw new TextException("Illegal entry of %s with text annotations on %s in %s", actual, label, type);
               }
            }
            if(root.isComposite()) {
               throw new TextException("Paths used with %s in %s", label, type);
            }
         }
      } 
   }
   
   /**
    * This is used to validate the unions that have been defined
    * within the type. Union validation is done by determining if 
    * the union has consistent inline values. If one annotation in
    * the union declaration is inline, then all must be inline.
    * 
    * @param type this is the type to validate the unions for
    */
   private void validateUnions(Class type) throws Exception {
      for(Label label : elements) {
         String[] options = label.getPaths();
         Contact contact = label.getContact();
         
         for(String option : options) {
            Annotation union = contact.getAnnotation();
            Label other = elements.get(option);
            
            if(label.isInline() != other.isInline()) {
               throw new UnionException("Inline must be consistent in %s for %s", union, contact);
            }
            if(label.isRequired() != other.isRequired()) {
               throw new UnionException("Required must be consistent in %s for %s", union, contact);
            }
         }    
      }
   }
   
   /**
    * This is used to validate the configuration of the scanned class.
    * If an ordered element is specified but does not refer to an
    * existing element then this will throw an exception.
    * 
    * @param type this is the object type that is being scanned
    * @param order this is the order that is to be validated
    */
   private void validateElements(Class type, Order order) throws Exception {
      if(order != null) {
         for(String name : order.elements()) {
            if(!isElement(name)) {
               throw new ElementException("Ordered element '%s' missing for %s", name, type);
            }
         }
      }
   }
   
   /**
    * This is used to validate the configuration of the scanned class.
    * If an ordered attribute is specified but does not refer to an
    * existing attribute then this will throw an exception.
    * 
    * @param type this is the object type that is being scanned
    * @param order this is the order that is to be validated
    */
   private void validateAttributes(Class type, Order order) throws Exception {
      if(order != null) {
         for(String name : order.attributes()) {
            if(!isAttribute(name)) {
               throw new AttributeException("Ordered attribute '%s' missing in %s", name, type);
            }
         }
      }
   } 
   
   /**
    * This is used to determine if the structure is empty. To check
    * to see if the structure is empty all models within the tree
    * must be examined. Also, if there is a text annotation then it
    * is not considered to be empty.
    * 
    * @return true if the structure represents an empty schema
    */
   private boolean isEmpty() {
      if(text != null) {
         return false;
      }
      return root.isEmpty();
   } 
}