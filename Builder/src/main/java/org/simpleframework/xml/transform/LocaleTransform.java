/*
 * LocaleTransform.java May 2007
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

import java.util.regex.Pattern;
import java.util.Locale;

/**
 * The <code>LocaleTransform</code> is used to transform locale
 * values to and from string representations, which will be inserted
 * in the generated XML document as the value place holder. The
 * value must be readable and writable in the same format. Fields
 * and methods annotated with the XML attribute annotation will use
 * this to persist and retrieve the value to and from the XML source.
 * <pre>
 * 
 *    &#64;Attribute
 *    private Locale locale;
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
class LocaleTransform implements Transform<Locale>{

   /**
    * This is the pattern used to split the parts of the locale.
    */
   private final Pattern pattern;
   
   /**
    * Constructor for the <code>LocaleTransform</code> object. This
    * is used to create a transform that will convert locales to and
    * from string representations. The representations use the Java
    * locale representation of language, country, and varient.
    */
   public LocaleTransform() {
      this.pattern = Pattern.compile("_");
   }
   
   /**
    * This method is used to convert the string value given to an
    * appropriate representation. This is used when an object is
    * being deserialized from the XML document and the value for
    * the string representation is required.
    * 
    * @param locale the string representation of the date value 
    * 
    * @return this returns an appropriate instanced to be used
    */
   public Locale read(String locale) throws Exception {
      String[] list = pattern.split(locale);
      
      if(list.length < 1) {
         throw new InvalidFormatException("Invalid locale %s", locale);
      }
      return read(list);
   }
   
   /**
    * This method is used to convert the string value given to an
    * appropriate representation. This is used when an object is
    * being deserialized from the XML document and the value for
    * the string representation is required.
    * 
    * @param locale the string representation of the date value 
    * 
    * @return this returns an appropriate instanced to be used
    */
   private Locale read(String[] locale)  throws Exception {
      String[] list = new String[] {"", "", ""};
      
      for(int i = 0; i < list.length; i++) {
         if(i < locale.length) {         
            list[i] = locale[i];
         }
      }
      return new Locale(list[0], list[1], list[2]);
   }
   
   /**
    * This method is used to convert the provided value into an XML
    * usable format. This is used in the serialization process when
    * there is a need to convert a field value in to a string so 
    * that that value can be written as a valid XML entity.
    * 
    * @param locale this is the value to be converted to a string
    * 
    * @return this is the string representation of the given date
    */
   public String write(Locale locale) {
      return locale.toString();
   }
}