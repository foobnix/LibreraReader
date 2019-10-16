/*
 * StringArrayTransform.java May 2007
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

import org.simpleframework.xml.transform.Transform;
import java.util.regex.Pattern;

/**
 * The <code>StringArrayTransform</code>  is used to transform string
 * arrays to and from string representations, which will be inserted
 * in the generated XML document as the value place holder. The
 * value must be readable and writable in the same format. Fields
 * and methods annotated with the XML attribute annotation will use
 * this to persist and retrieve the value to and from the XML source.
 * <pre>
 * 
 *    &#64;Attribute
 *    private String[] array;
 *    
 * </pre>
 * As well as the XML attribute values using transforms, fields and
 * methods annotated with the XML element annotation will use this.
 * Aside from the obvious difference, the element annotation has an
 * advantage over the attribute annotation in that it can maintain
 * any references using the <code>CycleStrategy</code> object. 
 * 
 * @author Niall Gallagher
 */
class StringArrayTransform implements Transform<String[]> {

   /**
    * Represents the pattern used to split the string values.
    */
   private final Pattern pattern;        

   /**
    * This is the token used to split the string into an array.
    */
   private final String token;        

   /**
    * Constructor for the <code>StringArrayTransform</code> object.
    * This will create a transform that will split an array using a
    * comma as the delimeter. In order to perform the split in a
    * reasonably performant manner the pattern used is compiled.
    */
   public StringArrayTransform() {
      this(",");           
   }        
  
   /**
    * Constructor for the <code>StringArrayTransform</code> object.
    * This will create a transform that will split an array using a
    * specified regular expression pattern. To keep the performance
    * of the transform reasonable the pattern used is compiled.
    * 
    * @param token the pattern used to split the string values
    */
   public StringArrayTransform(String token) {
      this.pattern = Pattern.compile(token);           
      this.token = token;           
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
   public String[] read(String value) {
      return read(value, token);           
   }
   
   /**
    * This method is used to convert the string value given to an
    * appropriate representation. This is used when an object is
    * being deserialized from the XML document and the value for
    * the string representation is required.
    * 
    * @param value this is the string representation of the value
    * @param token this is the token used to split the string
    * 
    * @return this returns an appropriate instanced to be used
    */
   private String[] read(String value, String token) {
      String[] list = pattern.split(value);

      for(int i = 0; i < list.length; i++) {
         String text = list[i];

         if(text != null) {              
            list[i] = text.trim();
         }
      }
      return list;
   }
   
   /**
    * This method is used to convert the provided value into an XML
    * usable format. This is used in the serialization process when
    * there is a need to convert a field value in to a string so 
    * that that value can be written as a valid XML entity.
    * 
    * @param list this is the value to be converted to a string
    * 
    * @return this is the string representation of the given value
    */
   public String write(String[] list) {
      return write(list, token);
   }
   
   /**
    * This method is used to convert the provided value into an XML
    * usable format. This is used in the serialization process when
    * there is a need to convert a field value in to a string so 
    * that that value can be written as a valid XML entity.
    * 
    * @param list this is the value to be converted to a string
    * @param token this is the token used to join the strings
    * 
    * @return this is the string representation of the given value
    */
   private String write(String[] list, String token) {                 
      StringBuilder text = new StringBuilder();           

      for(int i = 0; i < list.length; i++) {
         String item = list[i];

         if(item != null) { 
            if(text.length() > 0) {
               text.append(token);
               text.append(' ');
            }
            text.append(item);
         }
      }   
      return text.toString();
   }
}
