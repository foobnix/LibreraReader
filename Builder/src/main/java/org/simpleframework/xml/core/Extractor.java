/*
 * Extractor.java March 2011
 *
 * Copyright (C) 2011, Niall Gallagher <niallg@users.sf.net>
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

import java.lang.annotation.Annotation;

/**
 * The <code>Extractor</code> interface is used to represent an object
 * that can be used to extract constituent parts from a union. Using
 * this allows a uniform interface to be used to interface with various
 * different union annotations. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.ExtractorFactory
 */
interface Extractor<T extends Annotation>{
   
   /**
    * This is used to acquire each annotation that forms part of the
    * union group. Extracting the annotations in this way allows
    * the extractor to build a <code>Label</code> which can be used
    * to represent the annotation. Each label can then provide a
    * converter implementation to serialize objects.
    * 
    * @return this returns each annotation within the union group
    */
   T[] getAnnotations() throws Exception;
   
   /**
    * Each annotation can provide a class which is used to determine
    * which label is used to serialize an object. This ensures that
    * the correct label is selected whenever serialization occurs.
    * 
    * @param label this is the annotation to extract the type for
    * 
    * @return this returns the class associated with the annotation
    */
   Class getType(T label) throws Exception;
   
   /**
    * This creates a <code>Label</code> object used to represent the
    * annotation provided. Creating the label in this way ensures
    * that each union has access to the serialization methods 
    * defined for each type an XML element name.
    * 
    * @param label this is the annotation to create the label for
    * 
    * @return this is the label created for the annotation
    */
   Label getLabel(T label) throws Exception;
}