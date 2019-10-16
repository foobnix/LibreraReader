/*
 * OutputElement.java July 2006
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
 * The <code>OutputElement</code> object represents an XML element.  
 * Attributes can be added to this before ant child element has been
 * acquired from it. Once a child element has been acquired the
 * attributes will be written an can no longer be manipulated, the
 * same applies to any text value set for the element.
 * 
 * @author Niall Gallagher
 */ 
class OutputElement implements OutputNode {
   
   /**
    * Represents the attributes that have been set for the element.
    */         
   private OutputNodeMap table;

   /**
    * This is the namespace map that contains the namespaces.
    */ 
   private NamespaceMap scope;
   
   /**
    * Used to write the start tag and attributes for the document.
    */ 
   private NodeWriter writer;
   
   /**
    * This is the parent XML element to this output node.
    */
   private OutputNode parent;
   
   /**
    * This is the namespace reference URI associated with this.
    */ 
   private String reference;
   
   /**
    * This is the comment that has been set on this element.
    */
   private String comment;
   
   /**
    * Represents the value that has been set for the element.
    */ 
   private String value;

   /**
    * Represents the name of the element for this output node.
    */ 
   private String name;
   
   /**
    * This is the output mode that this element object is using.
    */
   private Mode mode;
   
   /**
    * Constructor for the <code>OutputElement</code> object. This is
    * used to create an output element that can create elements for
    * an XML document. This requires the writer that is used to 
    * generate the actual document and the name of this node.
    *
    * @param parent this is the parent node to this output node
    * @param writer this is the writer used to generate the file
    * @param name this is the name of the element this represents
    */ 
   public OutputElement(OutputNode parent, NodeWriter writer, String name) {
      this.scope = new PrefixResolver(parent);
      this.table = new OutputNodeMap(this);
      this.mode = Mode.INHERIT;
      this.writer = writer;           
      this.parent = parent;
      this.name = name;
   }     

   /**
    * This is used to acquire the prefix for this output node. This 
    * will search its parent nodes until the prefix that is currently
    * in scope is found.  If a prefix is not found in the parent 
    * nodes then a prefix in the current nodes namespace mappings is
    * searched, failing that the prefix returned is null.
    *
    * @return this returns the prefix associated with this node
    */  
   public String getPrefix() {
      return getPrefix(true);
   }
   
   /**
    * This is used to acquire the prefix for this output node. If
    * the output node is an element then this will search its parent
    * nodes until the prefix that is currently in scope is found. 
    * If however this node is an attribute then the hierarchy of 
    * nodes is not searched as attributes to not inherit namespaces.
    *
    * @param inherit if there is no explicit prefix then inherit
    *
    * @return this returns the prefix associated with this node
    */  
   public String getPrefix(boolean inherit) {
      String prefix = scope.getPrefix(reference);

      if(inherit) {
         if(prefix == null) {
            return parent.getPrefix();
         }
      }
      return prefix;
   }
   
   /**
    * This is used to acquire the namespace URI reference associated
    * with this node. Although it is recommended that the namespace
    * reference is a URI it does not have to be, it can be any unique
    * identifier that can be used to distinguish the qualified names.
    *
    * @return this returns the namespace URI reference for this
    */
   public String getReference() {
      return reference;
   }
  
   /**
    * This is used to set the reference for the node. Setting the
    * reference implies that the node is a qualified node within the
    * XML document. Both elements and attributes can be qualified.
    * Depending on the prefix set on this node or, failing that, any
    * parent node for the reference, the element will appear in the
    * XML document with that string prefixed to the node name.
    *
    * @param reference this is used to set the reference for the node
    */  
   public void setReference(String reference) {
      this.reference = reference;
   }
  
   /**
    * This returns the <code>NamespaceMap</code> for this node. Only
    * an element can have namespaces, so if this node represents an
    * attribute the elements namespaces will be provided when this is
    * requested. By adding a namespace it becomes in scope for the
    * current element all all child elements of that element.
    *
    * @return this returns the namespaces associated with the node
    */ 
   public NamespaceMap getNamespaces() {
      return scope;
   }
   
   /**
    * This is used to acquire the <code>Node</code> that is the
    * parent of this node. This will return the node that is
    * the direct parent of this node and allows for siblings to
    * make use of nodes with their parents if required.  
    *   
    * @return this returns the parent node for this node
    */
   public OutputNode getParent() {
      return parent;
   }
   
   /**
    * Returns the name of the node that this represents. This is
    * an immutable property and cannot be changed. This will be
    * written as the tag name when this has been committed.
    *  
    * @return returns the name of the node that this represents
    */   
   public String getName() {
      return name;           
   }
  
   /**
    * Returns the value for the node that this represents. This 
    * is a modifiable property for the node and can be changed,
    * however once committed any change will be irrelevant.
    * 
    * @return the name of the value for this node instance
    */   
   public String getValue() {
      return value;
   }
   
   /**
    * This is used to get the text comment for the element. This can
    * be null if no comment has been set. If no comment is set on 
    * the node then no comment will be written to the resulting XML.
    * 
    * @return this is the comment associated with this element
    */
   public String getComment() {
      return comment;
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
      return writer.isRoot(this);
   }
   
   /**
    * The <code>Mode</code> is used to indicate the output mode
    * of this node. Three modes are possible, each determines
    * how a value, if specified, is written to the resulting XML
    * document. This is determined by the <code>setData</code>
    * method which will set the output to be CDATA or escaped, 
    * if neither is specified the mode is inherited.
    * 
    * @return this returns the mode of this output node object
    */
   public Mode getMode() {
      return mode;
   }
   
   /**
    * This is used to set the output mode of this node to either
    * be CDATA, escaped, or inherited. If the mode is set to data
    * then any value specified will be written in a CDATA block, 
    * if this is set to escaped values are escaped. If however 
    * this method is set to inherited then the mode is inherited
    * from the parent node.
    * 
    * @param mode this is the output mode to set the node to 
    */
   public void setMode(Mode mode) {
      this.mode = mode;
   }
   
   /**
    * This returns a <code>NodeMap</code> which can be used to add
    * nodes to the element before that element has been committed. 
    * Nodes can be removed or added to the map and will appear as
    * attributes on the written element when it is committed.
    *
    * @return returns the node map used to manipulate attributes
    */    
   public OutputNodeMap getAttributes() {
      return table;
   }
   
   /**
    * This is used to set a text comment to the element. This will
    * be written just before the actual element is written. Only a
    * single comment can be set for each output node written. 
    * 
    * @param comment this is the comment to set on the node
    */
   public void setComment(String comment) {
      this.comment = comment;
   }

   /**
    * This is used to set a text value to the element. This should
    * be added to the element if the element contains no child
    * elements. If the value cannot be added an exception is thrown.
    * 
    * @param value this is the text value to add to this element
    */    
   public void setValue(String value) {
      this.value = value;
   }
   
   /**
    * This is used to change the name of an output node. This will
    * only affect the name of the node if the node has not yet been
    * committed. If the node is committed then this will not be
    * reflected in the resulting XML generated.
    * 
    * @param name this is the name to change the node to
    */
   public void setName(String name) {
      this.name = name;
   }
   
   /**
    * This is used to set the output mode of this node to either
    * be CDATA or escaped. If this is set to true the any value
    * specified will be written in a CDATA block, if this is set
    * to false the values is escaped. If however this method is
    * never invoked then the mode is inherited from the parent.
    * 
    * @param data if true the value is written as a CDATA block
    */
   public void setData(boolean data) {
      if(data) {
         mode = Mode.DATA;
      } else {
         mode = Mode.ESCAPE;
      }      
   }

   /**
    * This method is used for convinience to add an attribute node 
    * to the attribute <code>NodeMap</code>. The attribute added
    * can be removed from the element by useing the node map.
    * 
    * @param name this is the name of the attribute to be added
    * @param value this is the value of the node to be added
    *
    * @return this will return the attribute that was just set
    */    
   public OutputNode setAttribute(String name, String value) {
      return table.put(name, value);
   }

   /**
    * This is used to create a child element within the element that
    * this object represents. When a new child is created with this
    * method then the previous child is committed to the document.
    * The created <code>OutputNode</code> object can be used to add
    * attributes to the child element as well as other elements.
    *
    * @param name this is the name of the child element to create
    */    
   public OutputNode getChild(String name) throws Exception {
      return writer.writeElement(this, name);
   }
   
   /**
    * This is used to remove any uncommitted changes. Removal of an
    * output node can only be done if it has no siblings and has
    * not yet been committed. If the node is committed then this 
    * will throw an exception to indicate that it cannot be removed. 
    * 
    * @throws Exception thrown if the node cannot be removed
    */
   public void remove() throws Exception {
      writer.remove(this);
   }
   
   /**
    * This will commit this element and any uncommitted elements
    * elements that are decendents of this node. For instance if
    * any child or grand child remains open under this element
    * then those elements will be closed before this is closed.
    *
    * @throws Exception this is thrown if there is an I/O error
    */ 
   public void commit() throws Exception{
      writer.commit(this);
   }
  
   /**
    * This is used to determine whether this node has been committed.
    * If the node is committed then no further child elements can
    * be created from this node instance. A node is considered to
    * be committed if a parent creates another child element or if
    * the <code>commit</code> method is invoked.
    *
    * @return true if the node has been committed
    */  
   public boolean isCommitted() {
      return writer.isCommitted(this);
   }
   
   /**
    * This is the string representation of the element. It is
    * used for debugging purposes. When evaluating the element
    * the to string can be used to print out the element name.
    * 
    * @return this returns a text description of the element
    */
   public String toString() {
      return String.format("element %s", name);
   }
}
