/*
 * EnumTransform.java May 2007
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
 * The <code>EnumTransform</code> represents a transform that is 
 * used to transform enumerations to strings and back again. This
 * is used when enumerations are used in comma separated arrays.
 * This may be created multiple times for different types.
 * 
 * @author Niall Gallagher
 */
class EnumTransform implements Transform<Enum> {
   
   /**
    * This is the specific enumeration that this transforms.
    */
   private final Class type;
   
   /**
    * Constructor for the <code>EnumTransform</code> object. This
    * is used to create enumerations from strings and convert them
    * back again. This allows enumerations to be used in arrays.
    * 
    * @param type this is the enumeration type to be transformed
    */
   public EnumTransform(Class type) {
      this.type = type;
   }

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
   public Enum read(String value) throws Exception {
      return Enum.valueOf(type, value);
   }

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
   public String write(Enum value) throws Exception {
      return value.name();
   }
}