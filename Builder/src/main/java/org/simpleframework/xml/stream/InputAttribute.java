/*
 * InputAttribute.java July 2006
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

package org.simpleframework.xml.stream;

/**
 * The <code>InputAttribute</code> is used to represent an attribute
 * within an element. Rather than representing an attribute as a
 * name value pair of strings, an attribute is instead represented
 * as an input node, in the same manner as an element. The reason
 * for representing an attribute in this way is such that a uniform
 * means of extracting and parsing values can be used for inputs.
 *
 * @author Niall Gallagher
 */ 
class InputAttribute implements InputNode {

   /**
    * This is the parent node to this attribute instance.
    */
   private InputNode parent;
   
   /**
    * This is the reference associated with this attribute node.
    */
   private String reference;
   
   /**
    * This is the prefix associated with this attribute node.
    */
   private String prefix;
        
   /**
    * Represents the name of this input attribute instance.
    */         
   private String name;

   /**
    * Represents the value for this input attribute instance.
    */ 
   private String value;    
   
   /**
    * This is the source associated with this input attribute.
    */
   private Object source;
   
   /**
    * Constructor for the <code>InputAttribute</code> object. This
    * is used to create an input attribute using the provided name
    * and value, all other values for this input node will be null.
    * 
    * @param parent this is the parent node to this attribute
    * @param name this is the name for this attribute object
    * @param value this is the value for this attribute object
    */     
   public InputAttribute(InputNode parent, String name, String value) {
      this.parent = parent;
      this.value = value;
      this.name = name;           
   }
   
   /**
    * Constructor for the <code>InputAttribute</code> object. This
    * is used to create an input attribute using the provided name
    * and value, all other values for this input node will be null.
    * 
    * @param parent this is the parent node to this attribute
    * @param attribute this is the attribute containing the details
    */     
   public InputAttribute(InputNode parent, Attribute attribute) {
      this.reference = attribute.getReference();
      this.prefix = attribute.getPrefix();
      this.source = attribute.getSource();
      this.value = attribute.getValue();
      this.name = attribute.getName();
      this.parent = parent;           
   }
   
   /**
    * This is used to return the source object for this node. This
    * is used primarily as a means to determine which XML provider
    * is parsing the source document and producing the nodes. It
    * is useful to be able to determine the XML provider like this.
    * 
    * @return this returns the source of this input node
    */
   public Object getSource() {
      return source;
   }
   
   /**
    * This is used to acquire the <code>Node</code> that is the
    * parent of this node. This will return the node that is
    * the direct parent of this node and allows for siblings to
    * make use of nodes with their parents if required.  
    *   
    * @return this returns the parent node for this node
    */
   public InputNode getParent() {
      return parent;
   }
   
   /**
    * This provides the position of this node within the document.
    * This allows the user of this node to report problems with
    * the location within the document, allowing the XML to be
    * debugged if it does not match the class schema.
    *
    * @return this returns the position of the XML read cursor
    */  
   public Position getPosition() {
      return parent.getPosition();           
   }

   /**
    * Returns the name of the node that this represents. This is
    * an immutable property and will not change for this node. 
    *  
    * @return returns the name of the node that this represents
    */   
   public String getName() {
      return name;
   }
   
   /**
    * This is used to acquire the namespace prefix for the node.
    * If there is no namespace prefix for the node then this will
    * return null. Acquiring the prefix enables the qualification
    * of the node to be determined. It also allows nodes to be
    * grouped by its prefix and allows group operations.
    * 
    * @return this returns the prefix associated with this node
    */
   public String getPrefix() {
      return prefix;
   }
   
   /**
    * This allows the namespace reference URI to be determined.
    * A reference is a globally unique string that allows the
    * node to be identified. Typically the reference will be a URI
    * but it can be any unique string used to identify the node.
    * This allows the node to be identified within the namespace.
    * 
    * @return this returns the associated namespace reference URI 
    */
   public String getReference() {
      return reference;
   }

   /**
    * Returns the value for the node that this represents. This 
    * is an immutable value for the node and cannot be changed.
    * 
    * @return the name of the value for this node instance
    */   
   public String getValue() {
      return value;
   }
   
   /**
    * This method is used to determine if this node is the root 
    * node for the XML document. This will return false as this 
    * node can never be the root node because it is an attribute.
    * 
    * @return this will always return false for attribute nodes
    */
   public boolean isRoot() {
      return false;
   }

   /**
    * This is used to determine if this node is an element. This
    * node instance can not be an element so this method returns
    * false. Returning null tells the users of this node that any
    * attributes added to the node map will be permenantly lost.
    *
    * @return this returns false as this is an attribute node
    */ 
   public boolean isElement() {
      return false;           
   } 

   /**
    * Because the <code>InputAttribute</code> object represents an
    * attribute this method will return null. If nodes are added 
    * to the node map the values will not be available here.
    *
    * @return this always returns null for a requested attribute
    */ 
   public InputNode getAttribute(String name) {
      return null;
   }

   /**
    * Because the <code>InputAttribute</code> object represents an
    * attribute this method will return an empty map. If nodes are
    * added to the node map the values will not be maintained.
    *
    * @return this always returns an empty node map of attributes
    */
   public NodeMap<InputNode> getAttributes() {
      return new InputNodeMap(this);
   }
   
   /**
    * Because the <code>InputAttribute</code> object represents an
    * attribute this method will return null. An attribute is a
    * simple name value pair an so can not contain any child nodes.
    *
    * @return this always returns null for a requested child node
    */
   public InputNode getNext() {
      return null;           
   }
   
   /**
    * Because the <code>InputAttribute</code> object represents an
    * attribute this method will return null. An attribute is a
    * simple name value pair an so can not contain any child nodes.
    *
    * @param name this is the name of the next expected element
    *
    * @return this always returns null for a requested child node
    */
   public InputNode getNext(String name) {
      return null;           
   }
   
   /**
    * This method is used to skip all child elements from this
    * element. This allows elements to be effectively skipped such
    * that when parsing a document if an element is not required
    * then that element can be completely removed from the XML.
    */ 
   public void skip() {
      return;           
   }
   
   /**
    * This is used to determine if this input node is empty. An
    * empty node is one with no attributes or children. This can
    * be used to determine if a given node represents an empty
    * entity, with which no extra data can be extracted.
    * 
    * @return this will always return false as it has a value
    */
   public boolean isEmpty() {
      return false;
   }
   
   /**
    * This is the string representation of the attribute. It is
    * used for debugging purposes. When evaluating the attribute
    * the to string can be used to print out the attribute name.
    * 
    * @return this returns a text description of the attribute
    */
   public String toString() {
      return String.format("attribute %s='%s'", name, value);
   }
}
