/*
 * OutputNodeMap.java July 2006
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
 * The <code>OutputNodeMap</code> is used to collect attribute nodes
 * for an output node. This will create a generic node to add to the
 * map. The nodes created will be used by the output node to write
 * attributes for an element.
 * 
 * @author Niall Gallagher
 */ 
class OutputNodeMap extends LinkedHashMap<String, OutputNode> implements NodeMap<OutputNode> {

   /**
    * This is the source node that this node map belongs to.
    */         
   private final OutputNode source;
        
   /**
    * Constructor for the <code>OutputNodeMap</code> object. This is
    * used to create a node map that is used to create and collect
    * nodes, which will be used as attributes for an output element.
    */         
   public OutputNodeMap(OutputNode source) {
      this.source = source;           
   }   
   
   /**
    * This is used to acquire the actual node this map represents.
    * The source node provides further details on the context of
    * the node, such as the parent name, the namespace, and even
    * the value in the node. Care should be taken when using this. 
    * 
    * @return this returns the node that this map represents
    */
   public OutputNode getNode() {
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
    * This is used to add a new <code>Node</code> to the map. The
    * node that is created is a simple name value pair. Once the
    * node is created it can be retrieved by its given name.
    *
    * @param name this is the name of the node to be created
    * @param value this is the value to be given to the node
    * 
    * @return this is the node that has been added to the map
    */    
   public OutputNode put(String name, String value) {
      OutputNode node = new OutputAttribute(source, name, value);
      
      if(source != null) {
         put(name, node);
      }
      return node;
   }
   
   /**
    * This is used to remove the <code>Node</code> mapped to the
    * given name.  This returns a name value pair that represents
    * an attribute. If no node is mapped to the specified name 
    * then this method will a return null value.
    *
    * @param name this is the name of the node to remove
    * 
    * @return this will return the node mapped to the given name
    */    
   public OutputNode remove(String name) {
      return super.remove(name);
   }

   /**
    * This is used to acquire the <code>Node</code> mapped to the
    * given name. This returns a name value pair that represents
    * an element. If no node is mapped to the specified name then 
    * this method will return a null value.
    *
    * @param name this is the name of the node to retrieve
    * 
    * @return this will return the node mapped to the given name
    */   
   public OutputNode get(String name) {
      return super.get(name);
   }

   /**
    * This returns an iterator for the names of all the nodes in
    * this <code>OutputNodeMap</code>. This allows the names to be 
    * iterated within a for each loop in order to extract nodes.
    *
    * @return this returns the names of the nodes in the map
    */    
   public Iterator<String> iterator() {
      return keySet().iterator();           
   }
}
