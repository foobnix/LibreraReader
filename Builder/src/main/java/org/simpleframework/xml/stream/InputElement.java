/*
 * InputElement.java July 2006
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
 * The <code>InputElement</code> represents a self contained element
 * that will allow access to its child elements. If the next element
 * read from the <code>NodeReader</code> is not a child then this
 * will return null. The input element node also allows the attribute
 * values associated with the node to be accessed.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.stream.NodeReader
 */ 
class InputElement implements InputNode {
   
   /**
    * This contains all the attributes associated with the element.
    */ 
   private final InputNodeMap map;

   /**
    * This is the node reader that reads from the XML document.
    */ 
   private final NodeReader reader;
   
   /**
    * This is the parent node for this XML input element node.
    */
   private final InputNode parent;
   
   /**
    * This is the XML element that this node provides access to.
    */         
   private final EventNode node;
 
   /**
    * Constructor for the <code>InputElement</code> object. This 
    * is used to create an input node that will provide access to 
    * an XML element. All attributes associated with the element 
    * given are extracted and exposed via the attribute node map.
    *
    * @param parent this is the parent XML element for this 
    * @param reader this is the reader used to read XML elements
    * @param node this is the XML element wrapped by this node
    */ 
   public InputElement(InputNode parent, NodeReader reader, EventNode node) {
      this.map = new InputNodeMap(this, node);      
      this.reader = reader;           
      this.parent = parent;
      this.node = node;
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
      return node.getSource();
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
      return new InputPosition(node);           
   }   

   /**
    * Returns the name of the node that this represents. This is
    * an immutable property and should not change for any node.  
    * This provides the name without the name space part.
    *  
    * @return returns the name of the node that this represents
    */   
   public String getName() {
      return node.getName();           
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
      return node.getPrefix();
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
      return node.getReference();
   }
   
   /**
    * This method is used to determine if this node is the root 
    * node for the XML document. The root node is the first node
    * in the document and has no sibling nodes. This is false
    * if the node has a parent node or a sibling node.
    * 
    * @return true if this is the root node within the document
    */
   public boolean isRoot() {
      return reader.isRoot(this);
   }

   /**
    * This is used to determine if this node is an element. This
    * allows users of the framework to make a distinction between
    * nodes that represent attributes and nodes that represent
    * elements. This is particularly useful given that attribute
    * nodes do not maintain a node map of attributes.
    *
    * @return this returns true as this instance is an element
    */ 
   public boolean isElement() {
      return true;           
   } 

   /**
    * Provides an attribute from the element represented. If an
    * attribute for the specified name does not exist within the
    * element represented then this method will return null.
    *
    * @param name this is the name of the attribute to retrieve
    *
    * @return this returns the value for the named attribute
    */    
   public InputNode getAttribute(String name) {
      return map.get(name);
   }

   /**
    * This returns a map of the attributes contained within the
    * element. If no elements exist within the element then this
    * returns an empty map. 
    * 
    * @return this returns a map of attributes for the element
    */    
   public NodeMap<InputNode> getAttributes() {
      return map;
   }

   /**
    * Returns the value for the node that this represents. This 
    * is an immutable value for the node and cannot be changed.
    * If there is a problem reading an exception is thrown.
    * 
    * @return the name of the value for this node instance
    */     
   public String getValue() throws Exception {
      return reader.readValue(this);           
   }
  
   /**
    * The method is used to acquire the next child attribute of this 
    * element. If the next element from the <code>NodeReader</code> 
    * is not a child node to the element that this represents then
    * this will return null, which ensures each element represents
    * a self contained collection of child nodes.
    *
    * @return this returns the next child element of this node
    *
    * @exception Exception thrown if there is a problem reading
    */  
   public InputNode getNext() throws Exception {
      return reader.readElement(this);
   }
   
   /**
    * The method is used to acquire the next child attribute of this 
    * element. If the next element from the <code>NodeReader</code> 
    * is not a child node to the element that this represents then
    * this will return null, also if the next element does not match
    * the specified name then this will return null.
    *
    * @param name this is the name expected fromt he next element
    *
    * @return this returns the next child element of this node
    *
    * @exception Exception thrown if there is a problem reading
    */  
   public InputNode getNext(String name) throws Exception {
      return reader.readElement(this, name);
   }
   
   /**
    * This method is used to skip all child elements from this
    * element. This allows elements to be effectively skipped such
    * that when parsing a document if an element is not required
    * then that element can be completely removed from the XML.
    *
    * @exception Exception thrown if there was a parse error
    */ 
   public void skip() throws Exception {
      reader.skipElement(this);           
   }
   
   /**
    * This is used to determine if this input node is empty. An
    * empty node is one with no attributes or children. This can
    * be used to determine if a given node represents an empty
    * entity, with which no extra data can be extracted.
    * 
    * @return this returns true if the node is an empty element
    * 
    * @throws Exception thrown if there was a parse error
    */
   public boolean isEmpty() throws Exception {
      if(!map.isEmpty()) {
         return false;
      }
      return reader.isEmpty(this);           
   }
   
   /**
    * This is the string representation of the element. It is
    * used for debugging purposes. When evaluating the element
    * the to string can be used to print out the element name.
    * 
    * @return this returns a text description of the element
    */
   public String toString() {
      return String.format("element %s", getName());
   }
}


