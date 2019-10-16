/*
 * Strategy.java July 2006
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

package org.simpleframework.xml.strategy;

import java.util.Map;

import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;

/**
 * The <code>Strategy</code> interface represents a strategy that can be 
 * used to resolve and load the <code>Class</code> objects that compose 
 * a serializable object. A strategy implementation will make use of the
 * provided attribute node map to extract details that can be used to
 * determine what type of object must be used. 
 * <pre>
 * 
 *    &lt;xml version="1.0"&gt;
 *    &lt;example class="some.example.Demo"&gt;
 *       &lt;integer&gt;2&lt;/integer&gt;
 *    &lt;/example&gt;
 *    
 * </pre> 
 * The above example shows how the default strategy augments elements
 * with "class" attributes that describe the type that should be used
 * to instantiate a field when an object is deserialized. So looking at
 * the above example the root element would be a "some.example.Demo".
 * <p>
 * Custom <code>Strategy</code> implementations give the persister a
 * chance to intercept the class loading and type resolution for XML
 * documents. It also opens up the possibility for class versioning.
 * To establish contextual information a <code>Map</code> object can be
 * used. The map object is a transient object that is created and used
 * for the duration of a single operation of the persister.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.core.Persister
 */
public interface Strategy {

   /**
    * This is used to resolve and load a class for the given element.
    * The class should be of the same type or a subclass of the class
    * specified. It can be resolved using the details within the
    * provided XML node map, if the details used do not represent any
    * serializable values they should be removed so as not to disrupt
    * the deserialization process. For example the default strategy
    * removes all "class" attributes from the given node map.
    * 
    * @param type this is the type of the root element expected
    * @param node this is the node map used to resolve an override
    * @param map this is used to maintain contextual information
    * 
    * @return the value that should be used to describe the instance
    * 
    * @throws Exception thrown if the class cannot be resolved
    */
   Value read(Type type, NodeMap<InputNode> node, Map map) throws Exception;

   /**
    * This is used to attach attribute values to the given node
    * map during the serialization process. This method allows
    * the strategy to augment the XML document so that it can be
    * deserialized using a similar strategy. For example the 
    * default strategy adds a "class" attribute to the node map.
    *  
    * @param type this is the declared class for the field used
    * @param value this is the instance variable being serialized
    * @param node this is the node map used to represent the value
    * @param map this is used to maintain contextual information
    * 
    * @return this returns true if serialization is complete    
    * 
    * @throws Exception thrown if the details cannot be set
    */
   boolean write(Type type, Object value, NodeMap<OutputNode> node, Map map) throws Exception;

}
