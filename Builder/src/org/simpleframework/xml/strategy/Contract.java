/*
 * Contract.java April 2007
 *
 * Copyright (C) 2007, Niall Gallagher <niallg@users.sf.net>
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

/**
 * The <code>Contract</code> object is used to expose the attribute
 * names used by the cycle strategy. This ensures that reading and
 * writing of the XML document is done in a consistent manner. Each
 * attribute is used to mark special meta-data for the object graph. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.strategy.CycleStrategy
 */
class Contract {
             
   /**
    * This is used to specify the length of array instances.
    */
   private String length;
   
   /**
    * This is the label used to mark the type of an object.
    */
   private String label;
   
   /**
    * This is the attribute used to mark the identity of an object.
    */
   private String mark;
   
   /**
    * This is the attribute used to refer to an existing instance.
    */
   private String refer;
   
   /**
    * Constructor for the <code>Syntax</code> object. This is used
    * to expose the attribute names used by the strategy. All the
    * names can be acquired and shared by the read and write graph
    * objects, which ensures consistency between the two objects.
    * 
    * @param mark this is used to mark the identity of an object
    * @param refer this is used to refer to an existing object
    * @param label this is used to specify the class for the field
    * @param length this is the length attribute used for arrays
    */   
   public Contract(String mark, String refer, String label, String length){  
      this.length = length;
      this.label = label;
      this.refer = refer;
      this.mark = mark;
   }
   
   /**
    * This is returns the attribute used to store information about
    * the type to the XML document. This attribute name is used to 
    * add data to XML elements to enable the deserialization process
    * to know the exact instance to use when creating a type.
    * 
    * @return the name of the attribute used to store the type
    */
   public String getLabel() {
      return label;
   }
   
   /**
    * This returns the attribute used to store references within the
    * serialized XML document. The reference attribute is added to
    * the serialized XML element so that cycles in the object graph 
    * can be recreated. This is an optional attribute.
    * 
    * @return this returns the name of the reference attribute
    */
   public String getReference() {
      return refer;
   }
   
   /**
    * This returns the attribute used to store the identities of all
    * objects serialized to the XML document. The identity attribute
    * stores a unique identifiers, which enables this strategy to
    * determine an objects identity within the serialized XML.
    * 
    * @return this returns the name of the identity attribute used
    */
   public String getIdentity() {
      return mark;
   }
   
   /**
    * This returns the attribute used to store the array length in
    * the serialized XML document. The array length is required so
    * that the deserialization process knows how to construct the
    * array before any of the array elements are deserialized.
    * 
    * @return this returns the name of the array length attribute
    */
   public String getLength() {
      return length;
   } 
} 
