/*
 * InputNodeMap.java July 2006
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

import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * The <code>InputNodeMap</code> object represents a map to contain
 * attributes used by an input node. This can be used as an empty
 * node map, it can be used to extract its values from a start
 * element. This creates <code>InputAttribute</code> objects for 
 * each node added to the map, these can then be used by an element
 * input node to represent attributes as input nodes.
 *
 * @author Niall Gallagher
 */ 
class InputNodeMap extends LinkedHashMap<String, InputNode> implements NodeMap<InputNode> {

   /**
    * This is the source node that this node map belongs to.
    */          
   private final InputNode source;        
   
   /**
    * Constructor for the <code>InputNodeMap</code> object. This
    * is used to create an empty input node map, which will create
    * <code>InputAttribute</code> object for each inserted node.
    *
    * @param source this is the node this node map belongs to
    */ 
   protected InputNodeMap(InputNode source) {
      this.source = source;            
   }        

   /**
    * Constructor for the <code>InputNodeMap</code> object. This
    * is used to create an input node map, which will be populated
    * with the attributes from the <code>StartElement</code> that
    * is specified.
    *
    * @param source this is the node this node map belongs to
    * @param element the element to populate the node map with
    */ 
   public InputNodeMap(InputNode source, EventNode element) {
      this.source = source;           
      this.build(element);   
   }
   
   /**
    * This is used to insert all attributes belonging to the start
    * element to the map. All attributes acquired from the element
    * are converted into <code>InputAttribute</code> objects so 
    * that they can be used as input nodes by an input node.
    *
    * @param element the element to acquire attributes from
    */ 
   private void build(EventNode element) {
      for(Attribute entry : element) {
         InputAttribute value = new InputAttribute(source, entry);
         
         if(!entry.isReserved()) {
            put(value.getName(), value);
         }
      }
   }
   
   /**
    * This is used to acquire the actual node this map represents.
    * The source node provides further details on the context of
    * the node, such as the parent name, the namespace, and even
    * the value in the node. Care should be taken when using this. 
    * 
    * @return this returns the node that this map represents
    */
   public InputNode getNode() {
       return source;
   }

   /**
    * This is used to get the name of the element that owns the
    * nodes for the specified map. This can be used to determine
    * which element the node map belongs to.
    * 
    * @return this returns the name of the owning element
    */         
   public String getName() {
      return source.getName();           
   }   
   
   /**
    * This is used to add a new <code>InputAttribute</code> node to
    * the map. The created node can be used by an input node to
    * to represent the attribute as another input node. Once the 
    * node is created it can be acquired using the specified name.
    *
    * @param name this is the name of the node to be created
    * @param value this is the value to be given to the node
    * 
    * @return this returns the node that has just been added
    */    
   public InputNode put(String name, String value) {
      InputNode node = new InputAttribute(source, name, value);
      
      if(name != null) {
         put(name, node);
      }
      return node;
   }
   
   /**
    * This is used to remove the <code>Node</code> mapped to the
    * given name.  This returns a name value pair that represents
    * an attribute. If no node is mapped to the specified name 
    * then this method will return a null value.
    *
    * @param name this is the name of the node to remove
    * 
    * @return this will return the node mapped to the given name
    */    
   public InputNode remove(String name) {
      return super.remove(name);
   }
   
   /**
    * This is used to acquire the <code>Node</code> mapped to the
    * given name. This returns a name value pair that represents
    * an attribute. If no node is mapped to the specified name 
    * then this method will return a null value.
    *
    * @param name this is the name of the node to retrieve
    * 
    * @return this will return the node mapped to the given name
    */       
   public InputNode get(String name) {
      return super.get(name);
   }

   /**
    * This returns an iterator for the names of all the nodes in
    * this <code>NodeMap</code>. This allows the names to be 
    * iterated within a for each loop in order to extract nodes.
    *
    * @return this returns the names of the nodes in the map
    */    
   public Iterator<String> iterator() {
      return keySet().iterator();
   }
}
