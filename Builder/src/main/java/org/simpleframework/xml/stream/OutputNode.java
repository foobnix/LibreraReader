/*
 * OutputNode.java July 2006
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
 * The <code>OutputNode</code> object is used to represent a cursor
 * which can be used to write XML elements and attributes. Each of
 * the output node objects represents a element, and can be used 
 * to add attributes to that element as well as child elements.
 *
 * @author Niall Gallagher
 */ 
public interface OutputNode extends Node {
   
   /**
    * This method is used to determine if this node is the root 
    * node for the XML document. The root node is the first node
    * in the document and has no sibling nodes. This is false
    * if the node has a parent node or a sibling node.
    * 
    * @return true if this is the root node within the document
    */
   boolean isRoot();  
   
   /**
    * This returns a <code>NodeMap</code> which can be used to add
    * nodes to the element before that element has been committed. 
    * Nodes can be removed or added to the map and will appear as
    * attributes on the written element when it is committed.
    *
    * @return returns the node map used to manipulate attributes
    */ 
   NodeMap<OutputNode> getAttributes();
   
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
   Mode getMode();
   
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
   void setMode(Mode mode);
   
   /**
    * This is used to set the output mode of this node to either
    * be CDATA or escaped. If this is set to true the any value
    * specified will be written in a CDATA block, if this is set
    * to false the values is escaped. If however this method is
    * never invoked then the mode is inherited from the parent.
    * 
    * @param data if true the value is written as a CDATA block
    */
   void setData(boolean data);
  
   /**
    * This is used to acquire the prefix for this output node. If
    * the output node is an element then this will search its parent
    * nodes until the prefix that is currently in scope is found. 
    * If however this node is an attribute then the hierarchy of 
    * nodes is not searched as attributes to not inherit namespaces.
    *
    * @return this returns the prefix associated with this node
    */  
   String getPrefix();
   
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
   String getPrefix(boolean inherit);
   
   /**
    * This is used to acquire the namespace URI reference associated
    * with this node. Although it is recommended that the namespace
    * reference is a URI it does not have to be, it can be any unique
    * identifier that can be used to distinguish the qualified names.
    *
    * @return this returns the namespace URI reference for this
    */
   String getReference();
  
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
   void setReference(String reference);
  
   /**
    * This returns the <code>NamespaceMap</code> for this node. Only
    * an element can have namespaces, so if this node represents an
    * attribute the elements namespaces will be provided when this is
    * requested. By adding a namespace it becomes in scope for the
    * current element all all child elements of that element.
    *
    * @return this returns the namespaces associated with the node
    */  
   NamespaceMap getNamespaces();
   
   /**
    * This is used to get the text comment for the element. This can
    * be null if no comment has been set. If no comment is set on 
    * the node then no comment will be written to the resulting XML.
    * 
    * @return this is the comment associated with this element
    */
   String getComment();
   
   /**
    * This is used to set a text comment to the element. This will
    * be written just before the actual element is written. Only a
    * single comment can be set for each output node written. 
    * 
    * @param comment this is the comment to set on the node
    */
   void setComment(String comment);
   
   /**
    * This is used to set a text value to the element. This should
    * be added to the element if the element contains no child
    * elements. If the value cannot be added an exception is thrown.
    * 
    * @param value this is the text value to add to this element
    *
    * @throws Exception thrown if the text value cannot be added
    */ 
   void setValue(String value);
   
   /**
    * This is used to change the name of an output node. This will
    * only affect the name of the node if the node has not yet been
    * committed. If the node is committed then this will not be
    * reflected in the resulting XML generated.
    * 
    * @param name this is the name to change the node to
    */
   void setName(String name);
   
   /**
    * This method is used for convenience to add an attribute node 
    * to the attribute <code>NodeMap</code>. The attribute added
    * can be removed from the element by using the node map.
    * 
    * @param name this is the name of the attribute to be added
    * @param value this is the value of the node to be added
    * 
    * @return this returns the node that has just been added
    */ 
   OutputNode setAttribute(String name, String value);
   
   /**
    * This is used to acquire the <code>Node</code> that is the
    * parent of this node. This will return the node that is
    * the direct parent of this node and allows for siblings to
    * make use of nodes with their parents if required.  
    *   
    * @return this returns the parent node for this node
    */
   OutputNode getParent();
   
   /**
    * This is used to create a child element within the element that
    * this object represents. When a new child is created with this
    * method then the previous child is committed to the document.
    * The created <code>OutputNode</code> object can be used to add
    * attributes to the child element as well as other elements.
    *
    * @param name this is the name of the child element to create
    */ 
   OutputNode getChild(String name) throws Exception;        

   /**
    * This is used to remove any uncommitted changes. Removal of an
    * output node can only be done if it has no siblings and has
    * not yet been committed. If the node is committed then this 
    * will throw an exception to indicate that it cannot be removed. 
    * 
    * @throws Exception thrown if the node cannot be removed
    */
   void remove() throws Exception;
   
   /**
    * The <code>commit</code> method is used flush and commit any 
    * child nodes that have been created by this node. This allows
    * the output to be completed when building of the XML document
    * has been completed. If output fails an exception is thrown.
    * 
    * @throws Exception thrown if the node cannot be committed
    */ 
   void commit() throws Exception;

   /**
    * This is used to determine whether the node has been committed.
    * If the node has been committed, then this will return true.
    * When committed the node can no longer produce chile nodes.
    * 
    * @return true if this node has already been committed
    */
   boolean isCommitted();
}
