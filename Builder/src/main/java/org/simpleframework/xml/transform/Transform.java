/*
 * Transform.java May 2007
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

package org.simpleframework.xml.transform;

/**
 * A <code>Transform</code> represents a an object used to transform
 * an object to and from a string value. This is typically used when
 * either an <code>Attribute</code> or <code>Element</code> annotation
 * is used to mark the field of a type that does not contain any of
 * the XML annotations, and so does not represent an XML structure.
 * For example take the following annotation.
 * <pre>
 * 
 *    &#64;Text
 *    private Date date;
 *    
 * </pre> 
 * The above annotation marks an object from the Java class libraries
 * which does not contain any XML annotations. During serialization
 * and deserialization of such types a transform is used to process
 * the object such that it can be written and read to and from XML.
 * 
 * @author Niall Gallagher
 */
public interface Transform<T> {
   
   /**
    * This method is used to convert the string value given to an
    * appropriate representation. This is used when an object is
    * being deserialized from the XML document and the value for
    * the string representation is required.
    * 
    * @param value this is the string representation of the value
    * 
    * @return this returns an appropriate instanced to be used
    */
    T read(String value) throws Exception;
    
    /**
     * This method is used to convert the provided value into an XML
     * usable format. This is used in the serialization process when
     * there is a need to convert a field value in to a string so 
     * that that value can be written as a valid XML entity.
     * 
     * @param value this is the value to be converted to a string
     * 
     * @return this is the string representation of the given value
     */
    String write(T value) throws Exception;
}
