/*
 * ModelAssembler.java November 2010
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

import org.simpleframework.xml.Order;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>ModelAssembler</code> is used to assemble the model
 * using registrations based on the specified order. The order of
 * elements and attributes is specified by an <code>Order</code>
 * annotation. For order, all attributes within an XPath expression
 * must be valid attribute references, for example
 * <pre>
 * 
 *    some[1]/path/@attribute
 *    path/to/@attribute
 *    attribute    
 * 
 * </pre>
 * The above expressions are all legal references. The final 
 * reference specifies an attribute that is not within an XPath
 * expression. If the '@' character is missing from attribute
 * orderings an exception is thrown to indicate this.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.Order
 */
class ModelAssembler {
  
   /**
    * This is used to parse the XPath expressions in the order
    */
   private final ExpressionBuilder builder;  
   
   /**
    * This is the format that is used to style the order values. 
    */
   private final Format format;
   
   /**
    * This is the type this this is assembling the model for.
    */
   private final Detail detail;

   /**
    * Constructor for the <code>ModelAssembler</code> object. If
    * no order has been specified for the schema class then this
    * will perform no registrations on the specified model.   
    * 
    * @param builder this is the builder for XPath expressions
    * @param detail this contains the details for the assembler
    * @param support this contains various support functions
    */
   public ModelAssembler(ExpressionBuilder builder, Detail detail, Support support) throws Exception {
      this.format = support.getFormat();
      this.builder = builder;     
      this.detail = detail;
   }
   
   /**
    * This is used to assemble the model by perform registrations
    * based on the <code>Order</code> annotation. The initial
    * registrations performed by this establish the element and
    * attribute order for serialization of the schema class.
    * 
    * @param model the model to perform registrations on
    * @param order this is the order specified by the class   
    */
   public void assemble(Model model, Order order) throws Exception { 
      assembleElements(model, order);
      assembleAttributes(model, order);
   }
   
   /**
    * This is used to assemble the model by perform registrations
    * based on the <code>Order</code> annotation. The initial
    * registrations performed by this establish the element and
    * attribute order for serialization of the schema class.
    * 
    * @param model the model to perform registrations on
    * @param order this is the order specified by the class   
    */
   private void assembleElements(Model model, Order order) throws Exception {
      for(String value : order.elements()) {
         Expression path = builder.build(value);
         
         if(path.isAttribute()) {
            throw new PathException("Ordered element '%s' references an attribute in %s", path, detail);
         }         
         registerElements(model, path);         
      }
   }
   
   /**
    * This is used to assemble the model by perform registrations
    * based on the <code>Order</code> annotation. The initial
    * registrations performed by this establish the element and
    * attribute order for serialization of the schema class.
    * 
    * @param model the model to perform registrations on
    * @param order this is the order specified by the class   
    */
   private void assembleAttributes(Model model, Order order) throws Exception {
      for(String value : order.attributes()) {
         Expression path = builder.build(value);
         
         if(!path.isAttribute() && path.isPath()) {
            throw new PathException("Ordered attribute '%s' references an element in %s", path, detail);
         }
         if(!path.isPath()) {
            Style style = format.getStyle();
            String name = style.getAttribute(value);
            
            model.registerAttribute(name);
         } else {
         registerAttributes(model, path);         
         }
      }
   }
   
   /**
    * This is used to perform registrations using an expression.
    * Each segment in the expression will create a new model and
    * the final segment of the expression is the attribute.
    * 
    * @param model the model to register the attribute with
    * @param path this is the expression to be evaluated
    */
   private void registerAttributes(Model model, Expression path) throws Exception {
      String prefix = path.getPrefix();
      String name = path.getFirst();   
      int index = path.getIndex();
  
      if(path.isPath()) {
         Model next = model.register(name, prefix, index);
         Expression child = path.getPath(1);
         
         if(next == null) {
            throw new PathException("Element '%s' does not exist in %s", name, detail);
         }
         registerAttributes(next, child);
      } else {         
         registerAttribute(model, path);
      }
   }
   
   /**
    * This will register the attribute specified in the path within
    * the provided model. Registration here will ensure that the
    * attribute is ordered so that it is placed within the document
    * in a required position.
    * 
    * @param model this is the model to register the attribute in
    * @param path this is the path referencing the attribute
    */
   private void registerAttribute(Model model, Expression path) throws Exception {
      String name = path.getFirst(); 
      
      if(name != null) {
         model.registerAttribute(name);
      }
   }
   
   /**
    * This is used to perform registrations using an expression.
    * Each segment in the expression will create a new model and
    * the final segment of the expression is the element.
    * 
    * @param model the model to register the element with
    * @param path this is the expression to be evaluated
    */
   private void registerElements(Model model, Expression path) throws Exception {
      String prefix = path.getPrefix();
      String name = path.getFirst();  
      int index = path.getIndex();
      
      if(name != null) {
         Model next = model.register(name, prefix, index);
         Expression child = path.getPath(1);
      
         if(path.isPath()) {            
            registerElements(next, child);
         }
      }
      registerElement(model, path);      
   }   
   
   /**
    * This is used to register the element within the specified
    * model. To ensure the order does not conflict with expressions
    * the index of the ordered path is checked. If the order comes
    * before an expected order then an exception is thrown. 
    * For example, take the following expressions.
    * <pre>
    *    
    *    path[1]/element
    *    path[3]/element
    *    path[2]/element
    *    
    * </pre>
    * In the above the order of appearance of the expressions does
    * not match the indexes of the paths. This causes a conflict.
    * To ensure such a situation does not arise this is checked.
    * 
    * @param model this is the model to register the element in
    * @param path this is the expression referencing the element
    */
   private void registerElement(Model model, Expression path) throws Exception {
      String prefix = path.getPrefix();
      String name = path.getFirst();  
      int index = path.getIndex();
      
      if(index > 1) {
         Model previous = model.lookup(name, index -1);
         
         if(previous == null) {
            throw new PathException("Ordered element '%s' in path '%s' is out of sequence for %s", name, path, detail);
         }
      }
      model.register(name, prefix, index);
   }
}
