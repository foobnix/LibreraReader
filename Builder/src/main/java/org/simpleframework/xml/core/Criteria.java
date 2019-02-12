/*
 * Criteria.java December 2009
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
 * The <code>Criteria</code> object represents the criteria used to
 * create an object and populate its methods and fields. This allows
 * all deserialized information for a single object to be stored in
 * a single location. All deserialized variables are accessible from
 * the <code>get</code> method.
 * 
 * @author Niall Gallagher
 */
interface Criteria extends Iterable<Object> {
   
   /**
    * This is used to get the <code>Variable</code> that represents
    * a deserialized object. The variable contains all the meta
    * data for the field or method and the value that is to be set
    * on the method or field.
    * 
    * @param key this is the key of the variable to be acquired
    * 
    * @return this returns the keyed variable if it exists
    */
   Variable get(Object key) throws Exception;
   
   /**
    * This is used to get the <code>Variable</code> that represents
    * a deserialized object. The variable contains all the meta
    * data for the field or method and the value that is to be set
    * on the method or field.
    * 
    * @param label this is the label to acquire the variable for
    * 
    * @return this returns the variable associated with the label
    */
   Variable get(Label label) throws Exception;
   
   /**
    * This is used to resolve the <code>Variable</code> by using 
    * the union names of a label. This will also acquire variables
    * based on the actual name of the variable.
    * 
    * @param path this is the path of the variable to be acquired
    * 
    * @return this returns the variable mapped to the path
    */
   Variable resolve(String path) throws Exception;
   
   /**
    * This is used to remove the <code>Variable</code> from this
    * criteria object. When removed, the variable will no longer be
    * used to set the method or field when the <code>commit</code>
    * method is invoked.
    * 
    * @param key this is the key associated with the variable
    * 
    * @return this returns the keyed variable if it exists
    */
   Variable remove(Object key) throws Exception;
   
   /**
    * This is used to create a <code>Variable</code> and set it for
    * this criteria. The variable can be retrieved at a later stage
    * using the name of the label. This allows for repeat reads as
    * the variable can be used to acquire the labels converter.
    * 
    * @param label this is the label used to create the pointer
    * @param value this is the value of the object to be read
    */
   void set(Label label, Object value) throws Exception;
   
   /**
    * This is used to set the values for the methods and fields of
    * the specified object. Invoking this performs the population
    * of an object being deserialized. It ensures that each value 
    * is set after the XML element has been fully read.
    * 
    * @param source this is the object that is to be populated
    */
   void commit(Object source) throws Exception;
}