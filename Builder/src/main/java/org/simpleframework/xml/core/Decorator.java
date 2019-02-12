/*
 * Decorator.java July 2008
 *
 * Copyright (C) 2008, Niall Gallagher <niallg@users.sf.net>
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

import org.simpleframework.xml.stream.OutputNode;

/**
 * The <code>Decorator</code> interface is used to describe an object
 * that is used to add decorations to an output node. A decoration is
 * a object that adds information to the output node without any
 * change to the structure of the node. Decorations can include extra
 * information like comments and namespaces.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Label
 */
interface Decorator {
   
   /**
    * This method is used to decorate the provided node. This node 
    * can be either an XML element or an attribute. Decorations that
    * can be applied to the node by invoking this method include
    * things like comments and namespaces.
    * 
    * @param node this is the node that is to be decorated by this
    */
   void decorate(OutputNode node);
   
   /**
    * This method is used to decorate the provided node. This node 
    * can be either an XML element or an attribute. Decorations that
    * can be applied to the node by invoking this method include
    * things like comments and namespaces. This can also be given
    * another <code>Decorator</code> which is applied before this
    * decorator, any common data can then be overwritten.
    * 
    * @param node this is the node that is to be decorated by this
    * @param secondary this is a secondary decorator to be applied
    */
   void decorate(OutputNode node, Decorator secondary);
}