/*
 * Builder.java July 2008
 *
 * Copyright (C) 2008, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.xml.stream;

import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>Builder</code> class is used to represent an XML style
 * that can be applied to a serialized object. A style can be used to
 * modify the element and attribute names for the generated document.
 * Styles can be used to generate hyphenated or camel case XML.
 * <pre>
 * 
 *    &lt;example-element&gt;
 *        &lt;child-element example-attribute='example'&gt;
 *           &lt;inner-element&gt;example&lt;/inner-element&gt;
 *        &lt;/child-element&gt;
 *     &lt;/example-element&gt;
 *     
 * </pre>
 * Above the hyphenated XML elements and attributes can be generated
 * from a style implementation. Styles enable the same objects to be
 * serialized in different ways, generating different styles of XML
 * without having to modify the class schema for that object.    
 * 
 * @author Niall Gallagher
 */
class Builder implements Style {
   
   /**
    * This is the cache for the constructed attribute values.
    */
   private final Cache<String> attributes;
   
   /**
    * This is the cache for the constructed element values. 
    */
   private final Cache<String> elements;
   
   /**
    * This is the style object used to create the values used.
    */
   private final Style style;
   
   /**
    * Constructor for the <code>Builder</code> object. This will cache
    * values constructed from the inner style object, which allows the
    * results from the style to retrieved quickly the second time.
    * 
    * @param style this is the internal style object to be used
    */
   public Builder(Style style) {
      this.attributes = new ConcurrentCache<String>();
      this.elements = new ConcurrentCache<String>();
      this.style = style;
   }

   /**
    * This is used to generate the XML attribute representation of 
    * the specified name. Attribute names should ensure to keep the
    * uniqueness of the name such that two different names will
    * be styled in to two different strings.
    * 
    * @param name this is the attribute name that is to be styled
    * 
    * @return this returns the styled name of the XML attribute
    */
   public String getAttribute(String name) {
      String value = attributes.fetch(name);
      
      if(value != null) {
         return value;
      }
      value = style.getAttribute(name);
      
      if(value != null) {
         attributes.cache(name, value);
      }
      return value;
   }

   /**
    * This is used to generate the XML element representation of 
    * the specified name. Element names should ensure to keep the
    * uniqueness of the name such that two different names will
    * be styled in to two different strings.
    * 
    * @param name this is the element name that is to be styled
    * 
    * @return this returns the styled name of the XML element
    */
   public String getElement(String name) {
      String value = elements.fetch(name);
      
      if(value != null) {
         return value;
      }
      value = style.getElement(name);
      
      if(value != null) {
         elements.cache(name, value);
      }
      return value;
   }

   /**
    * This is used to set the attribute values within this builder.
    * Overriding the attribute values ensures that the default
    * algorithm does not need to determine each of the values. It
    * allows special behaviour that the user may require for XML.
    * 
    * @param name the name of the XML attribute to be overridden
    * @param value the value that is to be used for that attribute
    */
   public void setAttribute(String name, String value) {
      attributes.cache(name, value);
   }

   /**
    * This is used to set the element values within this builder.
    * Overriding the element values ensures that the default
    * algorithm does not need to determine each of the values. It
    * allows special behaviour that the user may require for XML.
    * 
    * @param name the name of the XML element to be overridden
    * @param value the value that is to be used for that element
    */
   public void setElement(String name, String value) {
      elements.cache(name, value);   
   }
}