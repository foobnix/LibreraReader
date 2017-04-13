/*
 * Formatter.java July 2006
 *
 * Copyright (C) 2006, Niall Gallagher <niallg@users.sf.net>
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

import java.io.BufferedWriter;
import java.io.Writer;

/**
 * The <code>Formatter</code> object is used to format output as XML
 * indented with a configurable indent level. This is used to write
 * start and end tags, as well as attributes and values to the given
 * writer. The output is written directly to the stream with and
 * indentation for each element appropriate to its position in the
 * document hierarchy. If the indent is set to zero then no indent
 * is performed and all XML will appear on the same line.
 *
 * @see org.simpleframework.xml.stream.Indenter
 */ 
class Formatter {

   /**
    * Represents the prefix used when declaring an XML namespace.
    */
   private static final char[] NAMESPACE = { 'x', 'm', 'l', 'n', 's' };
   
   /**
    * Represents the XML escape sequence for the less than sign.
    */ 
   private static final char[] LESS = { '&', 'l', 't', ';'};        
   
   /**
    * Represents the XML escape sequence for the greater than sign.
    */ 
   private static final char[] GREATER = { '&', 'g', 't', ';' };

   /**
    * Represents the XML escape sequence for the double quote.
    */ 
   private static final char[] DOUBLE = { '&', 'q', 'u', 'o', 't', ';' };

   /**
    * Represents the XML escape sequence for the single quote.
    */ 
   private static final char[] SINGLE = { '&', 'a', 'p', 'o', 's', ';' };

   /**
    * Represents the XML escape sequence for the ampersand sign.
    */ 
   private static final char[] AND = { '&', 'a', 'm', 'p', ';' };
   
   /**
    * This is used to open a comment section within the document.
    */
   private static final char[] OPEN = { '<', '!', '-', '-', ' ' };
   
   /**
    * This is used to close a comment section within the document.
    */
   private static final char[] CLOSE = { ' ', '-', '-', '>' };
   
   /**
    * Output buffer used to write the generated XML result to.
    */ 
   private OutputBuffer buffer;
   
   /**
    * Creates the indentations that are used buffer the XML file.
    */         
   private Indenter indenter;
   
   /**
    * This is the writer that is used to write the XML document.
    */
   private Writer result;

   /**
    * Represents the prolog to insert at the start of the document.
    */ 
   private String prolog;
   
   /**
    * Represents the last type of content that was written.
    */ 
   private Tag last;
   
   /**
    * Constructor for the <code>Formatter</code> object. This creates
    * an object that can be used to write XML in an indented format
    * to the specified writer. The XML written will be well formed.
    *
    * @param result this is where the XML should be written to
    * @param format this is the format object to use 
    */ 
   public Formatter(Writer result, Format format){
       this.result = new BufferedWriter(result, 1024);
       this.indenter = new Indenter(format);
       this.buffer = new OutputBuffer();
       this.prolog = format.getProlog();      
   }

   /**
    * This is used to write a prolog to the specified output. This is
    * only written if the specified <code>Format</code> object has
    * been given a non null prolog. If no prolog is specified then no
    * prolog is written to the generated XML.
    *
    * @throws Exception thrown if there is an I/O problem 
    */ 
   public void writeProlog() throws Exception {
      if(prolog != null) {
         write(prolog);         
         write("\n");         
      }
   }
  
   /**
    * This is used to write any comments that have been set. The
    * comment will typically be written at the start of an element
    * to describe the purpose of the element or include debug data
    * that can be used to determine any issues in serialization.
    * 
    * @param comment this is the comment that is to be written
    */  
   public void writeComment(String comment) throws Exception {
      String text = indenter.top();
      
      if(last == Tag.START) {
         append('>');
      }
      if(text != null) {
         append(text);
         append(OPEN);
         append(comment);
         append(CLOSE);
      }
      last = Tag.COMMENT;
   }
   
   /**
    * This method is used to write a start tag for an element. If a
    * start tag was written before this then it is closed. Before
    * the start tag is written an indent is generated and placed in
    * front of the tag, this is done for all but the first start tag.
    * 
    * @param name this is the name of the start tag to be written
    *
    * @throws Exception thrown if there is an I/O exception
    */ 
   public void writeStart(String name, String prefix) throws Exception{
      String text = indenter.push();

      if(last == Tag.START) {
         append('>');    
      }        
      flush();
      append(text);
      append('<');
      
      if(!isEmpty(prefix)) {
         append(prefix);
         append(':');
      }
      append(name);
      last = Tag.START;
   }
  
   /**
    * This is used to write a name value attribute pair. If the last
    * tag written was not a start tag then this throws an exception.
    * All attribute values written are enclosed in double quotes.
    * 
    * @param name this is the name of the attribute to be written
    * @param value this is the value to assigne to the attribute
    *
    * @throws Exception thrown if there is an I/O exception
    */  
   public void writeAttribute(String name, String value, String prefix) throws Exception{
      if(last != Tag.START) {
         throw new NodeException("Start element required");              
      }         
      write(' ');
      write(name, prefix);
      write('=');
      write('"');
      escape(value);
      write('"');               
   }
   
   /**
    * This is used to write the namespace to the element. This will
    * write the special attribute using the prefix and reference
    * specified. This will escape the reference if it is required.
    * 
    * @param reference this is the namespace URI reference to use
    * @param prefix this is the prefix to used for the namespace
    *
    * @throws Exception thrown if there is an I/O exception
    */  
   public void writeNamespace(String reference, String prefix) throws Exception{
      if(last != Tag.START) {
         throw new NodeException("Start element required");              
      }         
      write(' ');
      write(NAMESPACE);
      
      if(!isEmpty(prefix)) {
         write(':');
         write(prefix);
      }
      write('=');
      write('"');
      escape(reference);
      write('"');               
   }

   /**
    * This is used to write the specified text value to the writer.
    * If the last tag written was a start tag then it is closed.
    * By default this will escape any illegal XML characters. 
    *
    * @param text this is the text to write to the output
    *
    * @throws Exception thrown if there is an I/O exception
    */ 
   public void writeText(String text) throws Exception{
      writeText(text, Mode.ESCAPE);      
   }
   
   /**
    * This is used to write the specified text value to the writer.
    * If the last tag written was a start tag then it is closed.
    * This will use the output mode specified. 
    *
    * @param text this is the text to write to the output
    *
    * @throws Exception thrown if there is an I/O exception
    */ 
   public void writeText(String text, Mode mode) throws Exception{
      if(last == Tag.START) {
         write('>');
      }                
      if(mode == Mode.DATA) {
         data(text);
      } else {
         //escape(text);
          write(text);
      }         
      last = Tag.TEXT;
   }
   
   /**
    * This is used to write an end element tag to the writer. This
    * will close the element with a short <code>/&gt;</code> if the
    * last tag written was a start tag. However if an end tag or 
    * some text was written then a full end tag is written.
    *
    * @param name this is the name of the element to be closed
    *
    * @throws Exception thrown if there is an I/O exception
    */ 
   public void writeEnd(String name, String prefix) throws Exception {
      String text = indenter.pop();

      if(last == Tag.START) {
         write('/');
         write('>');
      } else {                       
         if(last != Tag.TEXT) {
            write(text);   
         }                        
         if(last != Tag.START) {
            write('<');
            write('/');
            write(name, prefix);
            write('>');
         }                    
      }                    
      last = Tag.END;
   }

   /**
    * This is used to write a character to the output stream without
    * any translation. This is used when writing the start tags and
    * end tags, this is also used to write attribute names.
    *
    * @param ch this is the character to be written to the output
    */ 
   private void write(char ch) throws Exception {     
      buffer.write(result);
      buffer.clear();
      result.write(ch);      
   }

   /**
    * This is used to write plain text to the output stream without
    * any translation. This is used when writing the start tags and
    * end tags, this is also used to write attribute names.
    *
    * @param plain this is the text to be written to the output
    */    
   private void write(char[] plain) throws Exception {      
      buffer.write(result);
      buffer.clear();
      result.write(plain);  
   }

   /**
    * This is used to write plain text to the output stream without
    * any translation. This is used when writing the start tags and
    * end tags, this is also used to write attribute names.
    *
    * @param plain this is the text to be written to the output
    */    
   private void write(String plain) throws Exception{      
      buffer.write(result);
      buffer.clear();
      result.write(plain);  
   }
   
   /**
    * This is used to write plain text to the output stream without
    * any translation. This is used when writing the start tags and
    * end tags, this is also used to write attribute names.
    *
    * @param plain this is the text to be written to the output
    * @param prefix this is the namespace prefix to be written
    */    
   private void write(String plain, String prefix) throws Exception{      
      buffer.write(result);
      buffer.clear();
      
      if(!isEmpty(prefix)) {
         result.write(prefix);
         result.write(':');
      }
      result.write(plain);  
   }
   
   /**
    * This is used to buffer a character to the output stream without
    * any translation. This is used when buffering the start tags so
    * that they can be reset without affecting the resulting document.
    *
    * @param ch this is the character to be written to the output
    */ 
   private void append(char ch) throws Exception {
      buffer.append(ch);           
   }
   
   /**
    * This is used to buffer characters to the output stream without
    * any translation. This is used when buffering the start tags so
    * that they can be reset without affecting the resulting document.
    *
    * @param plain this is the string that is to be buffered
    */     
   private void append(char[] plain) throws Exception {
      buffer.append(plain);
   }

   /**
    * This is used to buffer characters to the output stream without
    * any translation. This is used when buffering the start tags so
    * that they can be reset without affecting the resulting document.
    *
    * @param plain this is the string that is to be buffered
    */     
   private void append(String plain) throws Exception{
      buffer.append(plain);                    
   }
   
   /**
    * This method is used to write the specified text as a CDATA block
    * within the XML element. This is typically used when the value is
    * large or if it must be preserved in a format that will not be
    * affected by other XML parsers. For large text values this is 
    * also faster than performing a character by character escaping.
    * 
    * @param value this is the text value to be written as CDATA
    */
   private void data(String value) throws Exception {
      write("<![CDATA[");
      write(value);
      write("]]>");
   }
   
   /**
    * This is used to write the specified value to the output with
    * translation to any symbol characters or non text characters.
    * This will translate the symbol characters such as "&amp;",
    * "&gt;", "&lt;", and "&quot;". This also writes any non text
    * and non symbol characters as integer values like "&#123;".
    *
    * @param value the text value to be escaped and written
    */ 
   private void escape(String value) throws Exception {
      int size = value.length();

      for(int i = 0; i < size; i++){
         escape(value.charAt(i));
      }
   }

   /**
    * This is used to write the specified value to the output with
    * translation to any symbol characters or non text characters.
    * This will translate the symbol characters such as "&amp;",
    * "&gt;", "&lt;", and "&quot;". This also writes any non text
    * and non symbol characters as integer values like "&#123;".
    *
    * @param ch the text character to be escaped and written
    */ 
   private void escape(char ch) throws Exception {
      char[] text = symbol(ch);
         
      if(text != null) {
         write(text);
      } else {
         write(ch);                 
      }
   }   

   /**
    * This is used to flush the writer when the XML if it has been
    * buffered. The flush method is used by the node writer after an
    * end element has been written. Flushing ensures that buffering
    * does not affect the result of the node writer.
    */ 
   public void flush() throws Exception{
      buffer.write(result);
      buffer.clear();
      result.flush();
   }

   /**
    * This is used to convert the the specified character to unicode.
    * This will simply get the decimal representation of the given
    * character as a string so it can be written as an escape.
    *
    * @param ch this is the character that is to be converted
    *
    * @return this is the decimal value of the given character
    */ 
   private String unicode(char ch) {
      return Integer.toString(ch);           
   }
   
   /**
    * This method is used to determine if a root annotation value is
    * an empty value. Rather than determining if a string is empty
    * be comparing it to an empty string this method allows for the
    * value an empty string represents to be changed in future.
    * 
    * @param value this is the value to determine if it is empty
    * 
    * @return true if the string value specified is an empty value
    */
   private boolean isEmpty(String value) {
      if(value != null) {
         return value.length() == 0;
      }
      return true;  
   }

   /**
    * This is used to determine if the specified character is a text
    * character. If the character specified is not a text value then
    * this returns true, otherwise this returns false.
    *
    * @param ch this is the character to be evaluated as text
    *
    * @return this returns the true if the character is textual
    */ 
   private boolean isText(char ch) {
      switch(ch) {
      case ' ': case '\n':
      case '\r': case '\t':
         return true;              
      }           
      if(ch > ' ' && ch <= 0x7E){
         return ch != 0xF7;
      }
      return false;      
   }

   /**
    * This is used to convert the specified character to an XML text
    * symbol if the specified character can be converted. If the
    * character cannot be converted to a symbol null is returned.
    *
    * @param ch this is the character that is to be converted
    *
    * @return this is the symbol character that has been resolved
    */ 
   private char[] symbol(char ch) {
      switch(ch) {
      case '<':
        return LESS;
      case '>':
        return GREATER;
      case '"':
        return DOUBLE;
      case '\'':
        return SINGLE;
      case '&':
        return AND;
      }
      return null;
  }  
   
   /**
    * This is used to enumerate the different types of tag that can
    * be written. Each tag represents a state for the writer. After
    * a specific tag type has been written the state of the writer
    * is updated. This is needed to write well formed XML text.
    */ 
   private enum Tag {
      COMMENT,
      START,
      TEXT,
      END                
  }
}
