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
interface Creator {

   /**
    * This is used to instantiate the object using the default no
    * argument constructor. If for some reason the object can not be
    * instantiated then this will throw an exception with the reason.
    * 
    * @return this returns the object that has been instantiated
    */
   Object getInstance() throws Exception; 
   
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
   Object getInstance(Criteria criteria) throws Exception;

   /**
    * This is used to score this <code>Instantiator</code> object so that
    * it can be weighed amongst other constructors. The instantiator that
    * scores the highest is the one that is used for insIntantiation.
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
   double getScore(Criteria criteria) throws Exception;
   
   /**
    * This is the signature associated with the creator. The signature
    * contains all the parameters associated with the creator as well
    * as the constructor that this represents. Exposing the signature
    * allows the creator to be validated.
    * 
    * @return this is the signature associated with the creator
    */
   Signature getSignature() throws Exception;
   
   /**
    * This is the type associated with the <code>Creator</code> object.
    * All instances returned from this creator will be of this type.
    * 
    * @return this returns the type associated with this creator
    */
   Class getType() throws Exception;
}
