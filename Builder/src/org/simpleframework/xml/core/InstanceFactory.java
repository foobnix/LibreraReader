/*
 * Instantiator.java July 2006
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

import java.lang.reflect.Constructor;

import org.simpleframework.xml.strategy.Value;
import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>Instantiator</code> is used to instantiate types that 
 * will leverage a constructor cache to quickly create the objects.
 * This is used by the various object factories to return type 
 * instances that can be used by converters to create the objects 
 * that will later be deserialized.
 *
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Instance
 */
class InstanceFactory {

   /**
    * This is used to cache the constructors for the given types.
    */
   private final Cache<Constructor> cache;
   
   /**
    * Constructor for the <code>Instantiator</code> object. This will
    * create a constructor cache that can be used to cache all of 
    * the constructors instantiated for the required types. 
    */
   public InstanceFactory() {
      this.cache = new ConcurrentCache<Constructor>();
   }
   
   /**
    * This will create an <code>Instance</code> that can be used
    * to instantiate objects of the specified class. This leverages
    * an internal constructor cache to ensure creation is quicker.
    * 
    * @param value this contains information on the object instance
    * 
    * @return this will return an object for instantiating objects
    */
   public Instance getInstance(Value value) {
      return new ValueInstance(value);
   }

   /**
    * This will create an <code>Instance</code> that can be used
    * to instantiate objects of the specified class. This leverages
    * an internal constructor cache to ensure creation is quicker.
    * 
    * @param type this is the type that is to be instantiated
    * 
    * @return this will return an object for instantiating objects
    */
   public Instance getInstance(Class type) {
      return new ClassInstance(type);
   }
   
   /**
    * This method will instantiate an object of the provided type. If
    * the object or constructor does not have public access then this
    * will ensure the constructor is accessible and can be used.
    * 
    * @param type this is used to ensure the object is accessible
    *
    * @return this returns an instance of the specific class type
    */ 
   protected Object getObject(Class type) throws Exception {
      Constructor method = cache.fetch(type);
      
      if(method == null) {
         method = type.getDeclaredConstructor();      

         if(!method.isAccessible()) {
            method.setAccessible(true);              
         }
         cache.cache(type, method);
      }
      return method.newInstance();   
   }
   
   /**
    * The <code>ValueInstance</code> object is used to create an object
    * by using a <code>Value</code> instance to determine the type. If
    * the provided value instance represents a reference then this will
    * simply provide the value of the reference, otherwise it will
    * instantiate a new object and return that.
    */
   private class ValueInstance implements Instance {
      
      /**
       * This is the internal value that contains the criteria.
       */
      private final Value value;
      
      /**
       * This is the type that is to be instantiated by this.
       */
      private final Class type;
      
      /**
       * Constructor for the <code>ValueInstance</code> object. This 
       * is used to represent an instance that delegates to the given
       * value object in order to acquire the object. 
       * 
       * @param value this is the value object that contains the data
       */
      public ValueInstance(Value value) {
         this.type = value.getType();
         this.value = value;
      }
      
      /**
       * This method is used to acquire an instance of the type that
       * is defined by this object. If for some reason the type can
       * not be instantiated an exception is thrown from this.
       * 
       * @return an instance of the type this object represents
       */
      public Object getInstance() throws Exception {
         if(value.isReference()) {
            return value.getValue();
         }
         Object object = getObject(type);
         
         if(value != null) {
            value.setValue(object);
         }
         return object;
      }
      
      /**
       * This method is used acquire the value from the type and if
       * possible replace the value for the type. If the value can
       * not be replaced then an exception should be thrown. This 
       * is used to allow primitives to be inserted into a graph.
       * 
       * @param object this is the object to insert as the value
       * 
       * @return an instance of the type this object represents
       */
      public Object setInstance(Object object) {
         if(value != null) {
            value.setValue(object);
         }
         return object;
      }

      /**
       * This is used to determine if the type is a reference type.
       * A reference type is a type that does not require any XML
       * deserialization based on its annotations. Values that are
       * references could be substitutes objects or existing ones. 
       * 
       * @return this returns true if the object is a reference
       */
      public boolean isReference() {
         return value.isReference();
      }

      /**
       * This is the type of the object instance that will be created
       * by the <code>getInstance</code> method. This allows the 
       * deserialization process to perform checks against the field.
       * 
       * @return the type of the object that will be instantiated
       */
      public Class getType() {
         return type;
      }
   }
   
   /**
    * The <code>ClassInstance</code> object is used to create an object
    * by using a <code>Class</code> to determine the type. If the given
    * class can not be instantiated then this throws an exception when
    * the instance is requested. For performance an instantiator is
    * given as it contains a reflection cache for constructors.
    */
   private class ClassInstance implements Instance {
      
      /**
       * This represents the value of the instance if it is set.
       */
      private Object value;
      
      /**
       * This is the type of the instance that is to be created.
       */
      private Class type;
      
      /**
       * Constructor for the <code>ClassInstance</code> object. This is
       * used to create an instance of the specified type. If the given
       * type can not be instantiated then an exception is thrown.
       * 
       * @param type this is the type that is to be instantiated
       */
      public ClassInstance(Class type) {
         this.type = type;
      }

      /**
       * This method is used to acquire an instance of the type that
       * is defined by this object. If for some reason the type can
       * not be instantiated an exception is thrown from this.
       * 
       * @return an instance of the type this object represents
       */
      public Object getInstance() throws Exception {
         if(value == null) {
            value = getObject(type);
         }
         return value;
      }
      
      /**
       * This method is used acquire the value from the type and if
       * possible replace the value for the type. If the value can
       * not be replaced then an exception should be thrown. This 
       * is used to allow primitives to be inserted into a graph.
       * 
       * @param value this is the value to insert as the type
       * 
       * @return an instance of the type this object represents
       */
      public Object setInstance(Object value) throws Exception {
         return this.value = value;
      }

      /**
       * This is the type of the object instance that will be created
       * by the <code>getInstance</code> method. This allows the 
       * deserialization process to perform checks against the field.
       * 
       * @return the type of the object that will be instantiated
       */
      public Class getType() {
         return type;
      }

      /**
       * This is used to determine if the type is a reference type.
       * A reference type is a type that does not require any XML
       * deserialization based on its annotations. Values that are
       * references could be substitutes objects or existing ones. 
       * 
       * @return this returns true if the object is a reference
       */
      public boolean isReference() {
         return false;
      }
   }
}
