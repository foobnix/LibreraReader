/*
 * VisitorStrategy.java January 2010
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

package org.simpleframework.xml.strategy;

import java.util.Map;

import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;

/**
 * The <code>VisitorStrategy</code> object is a simplification of a
 * strategy, which allows manipulation of the serialization process.
 * Typically implementing a <code>Strategy</code> is impractical as
 * it requires the implementation to determine the type a node
 * represents. Instead it is often easier to visit each node that
 * is being serialized or deserialized and manipulate it so that 
 * the resulting XML can be customized. 
 * <p>
 * To perform customization in this way a <code>Visitor</code> can
 * be implemented. This can be passed to this strategy which will 
 * ensure the visitor is given each XML element as it is either 
 * being serialized or deserialized. Such an inversion of control
 * allows the nodes to be manipulated with little effort. By 
 * default this used <code>TreeStrategy</code> object as a default
 * strategy to delegate to. However, any strategy can be used.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.strategy.Visitor
 */
public class VisitorStrategy implements Strategy {
   
   /**
    * This is the strategy that is delegated to by this strategy.
    */
   private final Strategy strategy;
   
   /**
    * This is the visitor that is used to intercept serialization.
    */
   private final Visitor visitor;
   
   /**
    * Constructor for the <code>VisitorStrategy</code> object. This
    * strategy requires a visitor implementation that can be used
    * to intercept the serialization and deserialization process.
    * 
    * @param visitor this is the visitor used for interception
    */
   public VisitorStrategy(Visitor visitor) {
      this(visitor, new TreeStrategy());
   }
   
   /**
    * Constructor for the <code>VisitorStrategy</code> object. This
    * strategy requires a visitor implementation that can be used
    * to intercept the serialization and deserialization process.
    * 
    * @param visitor this is the visitor used for interception
    * @param strategy this is the strategy to be delegated to
    */
   public VisitorStrategy(Visitor visitor, Strategy strategy) {
      this.strategy = strategy;
      this.visitor = visitor;
   }

   /**
    * This method will read with  an internal strategy after it has
    * been intercepted by the visitor. Interception of the XML node
    * before it is delegated to the internal strategy allows the 
    * visitor to change some attributes or details before the node
    * is interpreted by the strategy.
    * 
    * @param type this is the type of the root element expected
    * @param node this is the node map used to resolve an override
    * @param map this is used to maintain contextual information
    * 
    * @return the value that should be used to describe the instance
    */
   public Value read(Type type, NodeMap<InputNode> node, Map map) throws Exception {
      if(visitor != null) {
         visitor.read(type, node);
      }
      return strategy.read(type, node, map);
   }

   /**
    * This method will write with an internal strategy before it has
    * been intercepted by the visitor. Interception of the XML node
    * before it is delegated to the internal strategy allows the 
    * visitor to change some attributes or details before the node
    * is interpreted by the strategy.
    * 
    * @param type this is the type of the root element expected
    * @param node this is the node map used to resolve an override
    * @param map this is used to maintain contextual information
    * 
    * @return the value that should be used to describe the instance
    */
   public boolean write(Type type, Object value, NodeMap<OutputNode> node, Map map) throws Exception {
      boolean result = strategy.write(type, value, node, map); 
      
      if(visitor != null) {
         visitor.write(type, node);
      }
      return result;
   }
}