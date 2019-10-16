/*
 * CamelCaseBuilder.java July 2008
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

/**
 * The <code>CamelCaseBuilder</code> is used to represent an XML style
 * that can be applied to a serialized object. A style can be used to
 * modify the element and attribute names for the generated document.
 * This styles can be used to generate camel case XML.
 * <pre>
 * 
 *    &lt;ExampleElement&gt;
 *        &lt;ChildElement exampleAttribute='example'&gt;
 *           &lt;InnerElement&gt;example&lt;/InnerElement&gt;
 *        &lt;/ChildElement&gt;
 *     &lt;/ExampleElement&gt;
 *     
 * </pre>
 * Above the camel case XML elements and attributes can be generated
 * from a style implementation. Styles enable the same objects to be
 * serialized in different ways, generating different styles of XML
 * without having to modify the class schema for that object.    
 * 
 * @author Niall Gallagher
 */
class CamelCaseBuilder implements Style {
   
   /**
    * If true then the attribute will start with upper case.
    */
   protected final boolean attribute;
   
   /**
    * If true then the element will start with upper case.
    */
   protected final boolean element;
   
   /**
    * Constructor for the <code>CamelCaseBuilder</code> object. This 
    * is used to create a style that will create camel case XML 
    * attributes and elements allowing a consistent format for 
    * generated XML. Both the attribute an elements are configurable.
    * 
    * @param element if true the element will start as upper case
    * @param attribute if true the attribute starts as upper case
    */
   public CamelCaseBuilder(boolean element, boolean attribute) {
      this.attribute = attribute;
      this.element = element;
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
      if(name != null) {
         return new Attribute(name).process();
      }
      return null;
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
      if(name != null) {
         return new Element(name).process();
      }
      return null;
   }
   
   /**
    * This is used to parse the style for this builder. This takes 
    * all of the words split from the original string and builds all
    * of the processed tokens for the styles elements and attributes.
    * 
    * @author Niall Gallagher
    */
   private class Attribute extends Splitter {
      
      /**
       * This determines whether to capitalise a split token
       */
      private boolean capital;
      
      /**
       * Constructor for the <code>Attribute</code> object. This will
       * take the original string and parse it such that all of the
       * words are emitted and used to build the styled token.
       * 
       * @param source this is the original string to be parsed
       */
      private Attribute(String source) {
         super(source);
      }
      
      /**
       * This is used to parse the provided text in to the style that
       * is required. Manipulation of the text before committing it
       * ensures that the text adheres to the required style.
       * 
       * @param text this is the text buffer to acquire the token from
       * @param off this is the offset in the buffer token starts at
       * @param len this is the length of the token to be parsed
       */
      @Override
      protected void parse(char[] text, int off, int len) {
         if(attribute || capital) {
            text[off] = toUpper(text[off]);
         }
         capital = true;
      }
      
      /**
       * This is used to commit the provided text in to the style that
       * is required. Committing the text to the buffer assembles the
       * tokens resulting in a complete token.
       * 
       * @param text this is the text buffer to acquire the token from
       * @param off this is the offset in the buffer token starts at
       * @param len this is the length of the token to be committed
       */
      @Override
      protected void commit(char[] text, int off, int len) {
         builder.append(text, off, len);
      }
   }
   
   /**
    * This is used to parse the style for this builder. This takes 
    * all of the words split from the original string and builds all
    * of the processed tokens for the styles elements and attributes.
    * 
    * @author Niall Gallagher
    */
   private class Element extends Attribute {
      
      /**
       * This determines whether to capitalise a split token
       */
      private boolean capital;
      
      /**
       * Constructor for the <code>Element</code> object. This will
       * take the original string and parse it such that all of the
       * words are emitted and used to build the styled token.
       * 
       * @param source this is the original string to be parsed
       */
      private Element(String source) {
         super(source);
      }
      
      /**
       * This is used to parse the provided text in to the style that
       * is required. Manipulation of the text before committing it
       * ensures that the text adheres to the required style.
       * 
       * @param text this is the text buffer to acquire the token from
       * @param off this is the offset in the buffer token starts at
       * @param len this is the length of the token to be parsed
       */
      @Override
      protected void parse(char[] text, int off, int len) {
         if(element || capital) {
            text[off] = toUpper(text[off]);
         }
         capital = true;
      }
   }

}
