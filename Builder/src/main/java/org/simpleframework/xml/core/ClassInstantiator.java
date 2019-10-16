/*
 * ClassInstantiator.java December 2009
 *
 * Copyright (C) 2009, Niall Gallagher <niallg@users.sf.net>
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

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>ClassInstantiator</code> is used for instantiating 
 * objects using either the default no argument constructor or one
 * that takes deserialized values as parameters. This also exposes 
 * the parameters and constructors used to instantiate the object.
 * 
 * @author Niall Gallagher
 */
class ClassInstantiator implements Instantiator {
   
   /**
    * This contains a list of all the creators for the class.
    */
   private final List<Creator> creators;  
   
   /**
    * This is used to acquire a parameter by the parameter name.
    */
   private final ParameterMap registry;
   
   /**
    * This represents the default no argument constructor used.
    */
   private final Creator primary;

   /**
    * This contains the details for the class to instantiate.
    */
   private final Detail detail;
   
   /**
    * Constructor for the <code>ClassCreator</code> object. This is
    * used to create an object that contains all information that
    * relates to the construction of an instance. 
    * 
    * @param creators contains the list of all constructors available
    * @param primary this is the default no argument constructor
    * @param registry contains all parameters for the constructors
    * @param detail contains the details for the instantiated class
    */
   public ClassInstantiator(List<Creator> creators, Creator primary, ParameterMap registry, Detail detail) {
      this.creators = creators;
      this.registry = registry;
      this.primary = primary;
      this.detail = detail;
   }

   /**
    * This is used to determine if this <code>Creator</code> has a
    * default constructor. If the class does contain a no argument
    * constructor then this will return true.
    * 
    * @return true if the class has a default constructor
    */
   public boolean isDefault() {
      int count = creators.size();
      
      if(count <= 1) {
         return primary != null;
      }
      return false;
   }
   
   /**
    * This is used to instantiate the object using the default no
    * argument constructor. If for some reason the object can not be
    * instantiated then this will throw an exception with the reason.
    * 
    * @return this returns the object that has been instantiated
    */
   public Object getInstance() throws Exception {
      return primary.getInstance();
   }
   
   /**
    * This is used to instantiate the object using a constructor that
    * takes deserialized objects as arguments. The object that have
    * been deserialized can be taken from the <code>Criteria</code>
    * object which contains the deserialized values.
    * 
    * @param criteria this contains the criteria to be used
    * 
    * @return this returns the object that has been instantiated
    */
   public Object getInstance(Criteria criteria) throws Exception {
      Creator creator = getCreator(criteria);
      
      if(creator == null) {
         throw new PersistenceException("Constructor not matched for %s", detail);
      }
      return creator.getInstance(criteria);
   }
   
   /**
    * This is used to acquire an <code>Instantiator</code> which is used
    * to instantiate the object. If there is no match for the instantiator
    * then the default constructor is provided.
    * 
    * @param criteria this contains the criteria to be used for this
    * 
    * @return this returns the instantiator that has been matched
    */
   private Creator getCreator(Criteria criteria) throws Exception {
      Creator result = primary;
      double max = 0.0;
      
      for(Creator instantiator : creators) {
         double score = instantiator.getScore(criteria);
         
         if(score > max) {
            result = instantiator;
            max = score;
         }
      }
      return result;
   }

   /**
    * This is used to acquire the named <code>Parameter</code> from
    * the creator. A parameter is taken from the constructor which
    * contains annotations for each object that is required. These
    * parameters must have a matching field or method.
    * 
    * @param name this is the name of the parameter to be acquired
    * 
    * @return this returns the named parameter for the creator
    */
   public Parameter getParameter(String name) {
      return registry.get(name);
   }
   
   /**
    * This is used to acquire all parameters annotated for the class
    * schema. Providing all parameters ensures that they can be
    * validated against the annotated methods and fields to ensure
    * that each parameter is valid and has a corresponding match.
    * 
    * @return this returns the parameters declared in the schema     
    */
   public List<Parameter> getParameters() {
      return registry.getAll();
   }
   
   /**
    * This is used to acquire all of the <code>Instantiator</code> 
    * objects used to create an instance of the object. Each represents 
    * a constructor and contains the parameters to the constructor. 
    * This is primarily used to validate each constructor against the
    * fields and methods annotated to ensure they are compatible.
    * 
    * @return this returns a list of instantiators for the creator
    */
   public List<Creator> getCreators() {
      return new ArrayList<Creator>(creators);
   }
   
   /**
    * This is used to acquire a description of the creator. This is
    * useful when debugging an issue as it allows a representation
    * of the instance to be viewed with the class it represents.
    * 
    * @return this returns a visible description of the creator
    */
   public String toString() {
      return String.format("creator for %s", detail);
   }
}