/*
 * Instantiator.java April 2009
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

import static org.simpleframework.xml.core.Support.isAssignable;

import java.util.List;

/**
 * The <code>Instantiator</code> object is used to represent an single
 * constructor within an object. It contains the actual constructor
 * as well as the list of parameters. Each instantiator will score its
 * weight when given a <code>Criteria</code> object. This allows
 * the deserialization process to find the most suitable one to
 * use when instantiating an object.
 * 
 * @author Niall Gallagher
 */
class SignatureCreator implements Creator {   
   
   /**
    * This is the list of parameters in the order of declaration. 
    */
   private final List<Parameter> list;
   
   /**
    * This is the map that contains the parameters to be used.
    */
   private final Signature signature;
   
   /**
    * This is the type represented by the creator instance.
    */
   private final Class type;
   
   /**
    * Constructor for the <code>Instantiator</code> object. This is used
    * to create a factory like object used for instantiating objects.
    * Each instantiator will score its suitability using the parameters
    * it is provided.
    * 
    * @param signature this is the signature that contains parameters
    */
   public SignatureCreator(Signature signature) {
      this.type = signature.getType();
      this.list = signature.getAll();
      this.signature = signature;
   } 
   
   /**
    * This is the type associated with the <code>Creator</code> object.
    * All instances returned from this creator will be of this type.
    * 
    * @return this returns the type associated with this creator
    */
   public Class getType() {
      return type;
   }
   
   /**
    * This is the signature associated with the creator. The signature
    * contains all the parameters associated with the creator as well
    * as the constructor that this represents. Exposing the signature
    * allows the creator to be validated.
    * 
    * @return this is the signature associated with the creator
    */
   public Signature getSignature() {
      return signature;
   }
   
   /**
    * This is used to instantiate the object using the default no
    * argument constructor. If for some reason the object can not be
    * instantiated then this will throw an exception with the reason.    
    * 
    * @return this returns the object that has been instantiated
    */
   public Object getInstance() throws Exception {
      return signature.create();
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
      Object[] values = list.toArray();
      
      for(int i = 0; i < list.size(); i++) {
         values[i] = getVariable(criteria, i);
      }
      return signature.create(values);
   }
   
   /**
    * This is used to acquire a variable from the criteria provided. 
    * In order to match the constructor correctly this will check to
    * see if the if the parameter is required. If it is required then
    * there must be a non null value or an exception is thrown.
    * 
    * @param criteria this is used to acquire the parameter value
    * @param index this is the index to acquire the value for
    * 
    * @return the value associated with the specified parameter
    */
   private Object getVariable(Criteria criteria, int index) throws Exception {
      Parameter parameter = list.get(index);
      Object key = parameter.getKey();
      Variable variable = criteria.remove(key);
      
      if(variable != null) {
         return variable.getValue();
      }
      return null;
   }
  
   /**
    * This is used to score this <code>Instantiator</code> object so that
    * it can be weighed amongst other constructors. The instantiator that
    * scores the highest is the one that is used for instantiation.
    * <p>
    * If any read only element or attribute is not a parameter in
    * the constructor then the constructor is discounted. This is
    * because there is no way to set the read only entity without a
    * constructor injection in to the instantiated object.
    * 
    * @param criteria this contains the criteria to be used
    * 
    * @return this returns the score based on the criteria provided
    */
   public double getScore(Criteria criteria) throws Exception {
      Signature match = signature.copy();
      
      for(Object key : criteria) {
         Parameter parameter = match.get(key);
         Variable label = criteria.get(key);
         Contact contact = label.getContact();

         if(parameter != null) {
            Object value = label.getValue();
            Class expect = value.getClass();
            Class actual = parameter.getType();
            
            if(!isAssignable(expect, actual)) {
               return -1.0;
            }
         }
         if(contact.isReadOnly()) {
            if(parameter == null) {
               return -1.0;
            }               
         }
      }
      return getPercentage(criteria);
   }
   
   /**
    * This is used to determine what percentage of available values
    * can be injected in to a constructor. Calculating the percentage
    * in this manner ensures that the best possible fit will be used
    * to construct the object. This also allows the object to define
    * what defaults it wishes to set for the values.
    * <p>
    * This will use a slight adjustment to ensure that if there are
    * many constructors with a 100% match on parameters, the one 
    * with the most values to be injected wins. This ensures the
    * most desirable constructor is chosen each time.
    *     
    * @param criteria this is the criteria object containing values
    * 
    * @return this returns the percentage match for the values
    */
   private double getPercentage(Criteria criteria) throws Exception {
      double score = 0.0;
      
      for(Parameter value : list) {
         Object key = value.getKey();
         Label label = criteria.get(key);

         if(label == null) {
            if(value.isRequired()) {
               return -1;
            }  
            if(value.isPrimitive()) {
               return -1;
            }
         } else {
            score++;
         }
      }
      return getAdjustment(score);
   }
   
   /**
    * This will use a slight adjustment to ensure that if there are
    * many constructors with a 100% match on parameters, the one 
    * with the most values to be injected wins. This ensures the
    * most desirable constructor is chosen each time.
    *     
    * @param score this is the score from the parameter matching
    * 
    * @return an adjusted score to account for the signature size
    */
   private double getAdjustment(double score) {
      double adjustment = list.size() / 1000.0;
      
      if(score > 0) {
         return adjustment + score / list.size();
      }
      return score / list.size();
   }   
   
   /**
    * This is used to acquire a descriptive name for the instantiator.
    * Providing a name is useful in debugging and when exceptions are
    * thrown as it describes the constructor the instantiator represents.
    * 
    * @return this returns the name of the constructor to be used
    */
   public String toString() {
      return signature.toString();
   }
}