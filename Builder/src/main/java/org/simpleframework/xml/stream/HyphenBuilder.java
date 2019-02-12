/*
 * HyphenBuilder.java July 2008
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
 * The <code>HyphenBuilder</code> is used to represent an XML style
 * that can be applied to a serialized object. A style can be used to
 * modify the element and attribute names for the generated document.
 * This styles can be used to generate hyphenated XML.
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
class HyphenBuilder implements Style {

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
         return new Parser(name).process();
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
         return new Parser(name).process();
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
   private class Parser extends Splitter {
      
      /**
       * Constructor for the <code>Parser</code> object. This will
       * take the original string and parse it such that all of the
       * words are emitted and used to build the styled token.
       * 
       * @param source this is the original string to be parsed
       */
      private Parser(String source) {
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
         text[off] = toLower(text[off]);
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
         
         if(off + len < count) {
            builder.append('-');
         }
      }
   }

}
