/*
 * OutputBuffer.java June 2007
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
 
package org.simpleframework.xml.stream;

import java.io.IOException;
import java.io.Writer;

/** 
 * This is primarily used to replace the <code>StringBuffer</code> 
 * class, as a way for the <code>Formatter</code> to store the start
 * tag for an XML element. This enables the start tag of the current
 * element to be removed without disrupting any of the other nodes
 * within the document. Once the contents of the output buffer have
 * been filled its contents can be emitted to the writer object.
 *
 * @author Niall Gallagher
 */
class OutputBuffer {      

   /** 
    * The characters that this buffer has accumulated.
    */
   private StringBuilder text;
   
   /** 
    * Constructor for <code>OutputBuffer</code>. The default 
    * <code>OutputBuffer</code> stores 16 characters before a
    * resize is needed to append extra characters. 
    */
   public OutputBuffer() {
      this.text = new StringBuilder();     
   }
   
   /** 
    * This will add a <code>char</code> to the end of the buffer.
    * The buffer will not overflow with repeated uses of the 
    * <code>append</code>, it uses an <code>ensureCapacity</code>
    * method which will allow the buffer to dynamically grow in 
    * size to accommodate more characters.
    *
    * @param ch the character to be appended to the buffer
    */
   public void append(char ch){
      text.append(ch);
   }

   /** 
    * This will add a <code>String</code> to the end of the buffer.
    * The buffer will not overflow with repeated uses of the 
    * <code>append</code>, it uses an <code>ensureCapacity</code> 
    * method which will allow the buffer to dynamically grow in 
    * size to accommodate large string objects.
    *
    * @param value the string to be appended to this output buffer
    */  
   public void append(String value){
      text.append(value);
   }
   
   /** 
    * This will add a <code>char</code> array to the buffer.
    * The buffer will not overflow with repeated uses of the 
    * <code>append</code>, it uses an <code>ensureCapacity</code> 
    * method which will allow the buffer to dynamically grow in 
    * size to accommodate large character arrays.
    *
    * @param value the character array to be appended to this
    */   
   public void append(char[] value){
      text.append(value, 0, value.length);
   }
   
   /** 
    * This will add a <code>char</code> array to the buffer.
    * The buffer will not overflow with repeated uses of the 
    * <code>append</code>, it uses an <code>ensureCapacity</code> 
    * method which will allow the buffer to dynamically grow in 
    * size to accommodate large character arrays.
    *
    * @param value the character array to be appended to this
    * @param off the read offset for the array to begin reading
    * @param len the number of characters to append to this
    */   
   public void append(char[] value, int off, int len){
      text.append(value, off, len);
   }
   
   /** 
    * This will add a <code>String</code> to the end of the buffer.
    * The buffer will not overflow with repeated uses of the 
    * <code>append</code>, it uses an <code>ensureCapacity</code>
    * method which will allow the buffer to dynamically grow in 
    * size to accommodate large string objects.
    *
    * @param value the string to be appended to the output buffer
    * @param off the offset to begin reading from the string
    * @param len the number of characters to append to this
    */   
   public void append(String value, int off, int len){
      text.append(value, off, len);
   }
   
   /**
    * This method is used to write the contents of the buffer to the
    * specified <code>Writer</code> object. This is used when the
    * XML element is to be committed to the resulting XML document.
    * 
    * @param out this is the writer to write the buffered text to
    * 
    * @throws IOException thrown if there is an I/O problem
    */
   public void write(Writer out) throws IOException {
      out.append(text);      
   }
   
   /** 
    * This will empty the <code>OutputBuffer</code> so that it does
    * not contain any content. This is used to that when the buffer
    * is written to a specified <code>Writer</code> object nothing
    * is written out. This allows XML elements to be removed.
    */
   public void clear(){     
      text.setLength(0);
   }
}   

