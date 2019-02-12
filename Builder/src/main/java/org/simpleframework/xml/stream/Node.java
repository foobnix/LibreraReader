/*
 * Node.java July 2006
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
 * The <code>Node</code> is used to represent a name value pair and
 * acts as the base form of data used within the framework. Each of
 * the attributes and elements are represented as nodes.  
 * 
 * @author Niall Gallagher
 */
public interface Node {

   /**
    * Returns the name of the node that this represents. This is
    * an immutable property and should not change for any node.  
    *  
    * @return returns the name of the node that this represents
    */
   String getName();

   /**
    * Returns the value for the node that this represents. This 
    * is a modifiable property for the node and can be changed.
    * 
    * @return the name of the value for this node instance
    * 
    * @throws Exception if there is a problem getting the value
    */
   String getValue() throws Exception;  
   
   /**
    * This is used to acquire the <code>Node</code> that is the
    * parent of this node. This will return the node that is
    * the direct parent of this node and allows for siblings to
    * make use of nodes with their parents if required.  
    *   
    * @return this returns the parent node for this node
    */
   Node getParent();
}
