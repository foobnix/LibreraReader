/*
 * Splitter.java July 2008
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
 * The <code>Splitter</code> object is used split up a string in to
 * tokens that can be used to create a camel case or hyphenated text
 * representation of the string. This will preserve acronyms and
 * numbers and splits tokens by case and character type. Examples
 * of how a string would be splitted are as follows.
 * <pre>
 * 
 *    CamelCaseString = "Camel" "Case" "String"
 *    hyphenated-text = "hyphenated" "text"
 *    URLAcronym      = "URL" "acronym"
 *    RFC2616.txt     = "RFC" "2616" "txt"
 * 
 * </pre>
 * By splitting strings in to individual words this allows the
 * splitter to be used to assemble the words in a way that adheres
 * to a specific style. Each style can then be applied to an XML 
 * document to give it a consistent format.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.stream.Style
 */
abstract class Splitter {

   /**
    * This is the string builder used to build the processed text.
    */
   protected StringBuilder builder;
   
   /**
    * This is the original text that is to be split in to words.
    */
   protected char[] text;
   
   /**
    * This is the number of characters to be considered for use.
    */
   protected int count;
   
   /**
    * This is the current read offset of the text string.
    */
   protected int off;
   
   /**
    * Constructor of the <code>Splitter</code> object. This is used
    * to split the provided string in to individual words so that
    * they can be assembled as a styled token, which can represent
    * an XML attribute or element.
    * 
    * @param source this is the source that is to be split 
    */
   public Splitter(String source) {
      this.builder = new StringBuilder();
      this.text = source.toCharArray();
      this.count = text.length;
   }
   
   /**
    * This is used to process the internal string and convert it in
    * to a styled string. The styled string can then be used as an
    * XML attribute or element providing a consistent format to the
    * document that is being generated.
    * 
    * @return the string that has been converted to a styled string
    */
   public String process() {
      while(off < count) {
         while(off < count) {
            char ch = text[off];
            
            if(!isSpecial(ch)) {
               break;
            }
            off++;
         }
         if(!acronym()) {
            token();
            number();
         }
      }
      return builder.toString();
   }
   
   /**
    * This is used to extract a token from the source string. Once a
    * token has been extracted the <code>commit</code> method is 
    * called to add it to the string being build. Each time this is
    * called a token, if extracted, will be committed to the string.
    * Before being committed the string is parsed for styling.
    */
   private void token() {
      int mark = off;
      
      while(mark < count) {
         char ch = text[mark];
         
         if(!isLetter(ch)) {
            break;
         } 
         if(mark > off) {
            if(isUpper(ch)) {
               break;
            }
         }
         mark++;
      }
      if(mark > off) {
         parse(text, off, mark - off);
         commit(text, off, mark - off);
      }
      off = mark;
   }
   
   /**
    * This is used to extract a acronym from the source string. Once 
    * a token has been extracted the <code>commit</code> method is 
    * called to add it to the string being build. Each time this is
    * called a token, if extracted, will be committed to the string.
    * 
    * @return true if an acronym was extracted from the source
    */
   private boolean acronym() { // is it the last one?
      int mark = off;
      int size = 0;
      
      while(mark < count) {
         char ch = text[mark];
         
         if(isUpper(ch)) {
            size++;
         } else {
            break;
         }
         mark++;
      }
      if(size > 1) {
         if(mark < count) {
            char ch = text[mark-1];
            
            if(isUpper(ch)) {
               mark--;
            }
         }
         commit(text, off, mark - off);
         off = mark;
      }
      return size > 1;
   }
   
   /**
    * This is used to extract a number from the source string. Once 
    * a token has been extracted the <code>commit</code> method is 
    * called to add it to the string being build. Each time this is
    * called a token, if extracted, will be committed to the string.
    * 
    * @return true if an number was extracted from the source
    */
   private boolean number() {
      int mark = off;
      int size = 0;
      
      while(mark < count) {
         char ch = text[mark];
         
         if(isDigit(ch)) {
            size++;
         } else {
            break;
         }
         mark++;
      }
      if(size > 0) {
         commit(text, off, mark - off);
      }
      off = mark;
      return size > 0;
   }

   /**
    * This is used to determine if the provided string evaluates to
    * a letter character. This delegates to <code>Character</code> 
    * so that the full range of unicode characters are considered.
    * 
    * @param ch this is the character that is to be evaluated
    * 
    * @return this returns true if the character is a letter
    */
   private boolean isLetter(char ch) {
      return Character.isLetter(ch);
   }
   
   /**
    * This is used to determine if the provided string evaluates to
    * a symbol character. This delegates to <code>Character</code> 
    * so that the full range of unicode characters are considered.
    * 
    * @param ch this is the character that is to be evaluated
    * 
    * @return this returns true if the character is a symbol
    */
   private boolean isSpecial(char ch) {
      return !Character.isLetterOrDigit(ch);
   }
   
   /**
    * This is used to determine if the provided string evaluates to
    * a digit character. This delegates to <code>Character</code> 
    * so that the full range of unicode characters are considered.
    * 
    * @param ch this is the character that is to be evaluated
    * 
    * @return this returns true if the character is a digit
    */
   private boolean isDigit(char ch) {
      return Character.isDigit(ch);
   }
   
   /**
    * This is used to determine if the provided string evaluates to
    * an upper case letter. This delegates to <code>Character</code> 
    * so that the full range of unicode characters are considered.
    * 
    * @param ch this is the character that is to be evaluated
    * 
    * @return this returns true if the character is upper case
    */
   private boolean isUpper(char ch) {
      return Character.isUpperCase(ch);
   }
   
   /**
    * This is used to convert the provided character to an upper
    * case character. This delegates to <code>Character</code> to
    * perform the conversion so unicode characters are considered.
    * 
    * @param ch this is the character that is to be converted
    * 
    * @return the character converted to upper case
    */
   protected char toUpper(char ch) {
      return Character.toUpperCase(ch);
   }
   
   /**
    * This is used to convert the provided character to a lower
    * case character. This delegates to <code>Character</code> to
    * perform the conversion so unicode characters are considered.
    * 
    * @param ch this is the character that is to be converted
    * 
    * @return the character converted to lower case
    */
   protected char toLower(char ch) {
      return Character.toLowerCase(ch);
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
   protected abstract void parse(char[] text, int off, int len);
   
   /**
    * This is used to commit the provided text in to the style that
    * is required. Committing the text to the buffer assembles the
    * tokens resulting in a complete token.
    * 
    * @param text this is the text buffer to acquire the token from
    * @param off this is the offset in the buffer token starts at
    * @param len this is the length of the token to be committed
    */
   protected abstract void commit(char[] text, int off, int len);
}
