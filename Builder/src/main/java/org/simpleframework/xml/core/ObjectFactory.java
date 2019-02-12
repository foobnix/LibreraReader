/*
 * ObjectFactory.java July 2006
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

package org.simpleframework.xml.core;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Value;
import org.simpleframework.xml.stream.InputNode;

/**
 * The <code>ObjectFactory</code> is the most basic factory. This will
 * basically check to see if there is an override type within the XML
 * node provided, if there is then that is instantiated, otherwise the
 * field type is instantiated. Any type created must have a default
 * no argument constructor. If the override type is an abstract class
 * or an interface then this factory throws an exception.
 *  
 * @author Niall Gallagher
 */ 
class ObjectFactory extends PrimitiveFactory {
   
   /**
    * Constructor for the <code>ObjectFactory</code> class. This is
    * given the field class that this should create object instances
    * of. If the field type is abstract then the XML node must have
    * sufficient information for the <code>Strategy</code> object to
    * resolve the implementation class to be instantiated.
    *
    * @param context the contextual object used by the persister 
    * @param type this is the object type to use for this factory 
    */
   public ObjectFactory(Context context, Type type, Class override) {
      super(context, type, override);
   }        

   /**
    * This method will instantiate an object of the field type, or if
    * the <code>Strategy</code> object can resolve a class from the
    * XML element then this is used instead. If the resulting type is
    * abstract or an interface then this method throws an exception.
    * 
    * @param node this is the node to check for the override
    * 
    * @return this returns an instance of the resulting type
    */       
   @Override
   public Instance getInstance(InputNode node) throws Exception {
      Value value = getOverride(node);
      Class expect = getType();
    
      if(value == null) { 
         if(!isInstantiable(expect)) {
            throw new InstantiationException("Cannot instantiate %s for %s", expect, type);              
         }
         return context.getInstance(expect);         
      }
      return new ObjectInstance(context, value);      
   }     
}
