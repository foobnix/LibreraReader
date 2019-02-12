/*
 * ArrayFactory.java July 2006
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

import java.lang.reflect.Array;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Value;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.Position;

/**
 * The <code>ArrayFactory</code> is used to create object array
 * types that are compatible with the field type. This simply
 * requires the type of the array in order to instantiate that
 * array. However, this also performs a check on the field type 
 * to ensure that the array component types are compatible.
 * 
 * @author Niall Gallagher
 */ 
class ArrayFactory extends Factory { 
        
   /**
    * Constructor for the <code>ArrayFactory</code> object. This is
    * given the array component type as taken from the field type 
    * of the source object. Each request for an array will return 
    * an array which uses a compatible component type.
    * 
    * @param context this is the context object for serialization
    * @param type the array component type for the field object
    */
   public ArrayFactory(Context context, Type type) {
      super(context, type);                
   }    
   
   /**
    * This is used to create a default instance of the field type. It
    * is up to the subclass to determine how to best instantiate an
    * object of the field type that best suits. This is used when the
    * empty value is required or to create the default type instance.
    * 
    * @return a type which is used to instantiate the collection     
    */
   @Override
   public Object getInstance() throws Exception {
      Class type = getComponentType();
      
      if(type != null) {
         return Array.newInstance(type, 0);
      }
      return null;
   }

   /**
    * Creates the array type to use. This will use the provided
    * XML element to determine the array type and provide a means
    * for creating an array with the <code>Value</code> object. If
    * the array size cannot be determined an exception is thrown.
    * 
    * @param node this is the input node for the array element
    * 
    * @return the object array type used for the instantiation
    */         
   public Instance getInstance(InputNode node) throws Exception {
      Position line = node.getPosition();
      Value value = getOverride(node);    
      
      if(value == null) {
         throw new ElementException("Array length required for %s at %s", type, line);         
      }      
      Class type = value.getType();
      
      return getInstance(value, type);
   }

   /**
    * Creates the array type to use. This will use the provided
    * XML element to determine the array type and provide a means
    * for creating an array with the <code>Value</code> object. If
    * the array types are not compatible an exception is thrown.
    * 
    * @param value this is the type object with the array details
    * @param entry this is the entry type for the array instance    
    * 
    * @return this object array type used for the instantiation  
    */
   private Instance getInstance(Value value, Class entry) throws Exception {
      Class expect = getComponentType();

      if(!expect.isAssignableFrom(entry)) {
         throw new InstantiationException("Array of type %s cannot hold %s for %s", expect, entry, type);
      }
      return new ArrayInstance(value);   
   }   
   
   /**
    * This is used to extract the component type for the array class
    * this factory represents. This is used when an array is to be
    * instantiated. If the class provided to the factory is not an
    * array then this will throw an exception.
    * 
    * @return this returns the component type for the array
    */
   private Class getComponentType() throws Exception {
      Class expect = getType();
      
      if(!expect.isArray()) {
         throw new InstantiationException("The %s not an array for %s", expect, type);
      }
      return expect.getComponentType();
   }
}