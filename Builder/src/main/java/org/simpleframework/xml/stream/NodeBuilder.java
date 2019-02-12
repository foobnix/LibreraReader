/*
 * NodeBuilder.java July 2006
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

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * The <code>NodeBuilder</code> object is used to create either an
 * input node or an output node for a given source or destination. 
 * If an <code>InputNode</code> is required for reading an XML
 * document then a reader must be provided to read the content from.
 * <p>
 * If an <code>OutputNode</code> is required then a destination is
 * required. The provided output node can be used to generate well
 * formed XML to the specified writer. 
 * 
 * @author Niall Gallagher
 */ 
public final class NodeBuilder {
 
   /**
    * This is the XML provider implementation that creates readers.
    */         
   private static Provider PROVIDER;

   static {
      PROVIDER = ProviderFactory.getInstance();                    
   }

   /**
    * This is used to create an <code>InputNode</code> that can be 
    * used to read XML from the specified stream. The stream will
    * be positioned at the root element in the XML document.
    *
    * @param source this contains the contents of the XML source
    *
    * @throws Exception thrown if there is an I/O exception
    */   
   public static InputNode read(InputStream source) throws Exception {
      return read(PROVIDER.provide(source));   
   }
        
   /**
    * This is used to create an <code>InputNode</code> that can be 
    * used to read XML from the specified reader. The reader will
    * be positioned at the root element in the XML document.
    *
    * @param source this contains the contents of the XML source
    *
    * @throws Exception thrown if there is an I/O exception
    */   
   public static InputNode read(Reader source) throws Exception {
      return read(PROVIDER.provide(source));   
   }

   /**
    * This is used to create an <code>InputNode</code> that can be 
    * used to read XML from the specified reader. The reader will
    * be positioned at the root element in the XML document.
    *
    * @param source this contains the contents of the XML source
    *
    * @throws Exception thrown if there is an I/O exception
    */     
   private static InputNode read(EventReader source) throws Exception {
      return new NodeReader(source).readRoot();           
   }
   
   /**
    * This is used to create an <code>OutputNode</code> that can be
    * used to write a well formed XML document. The writer specified
    * will have XML elements, attributes, and text written to it as
    * output nodes are created and populated.
    * 
    * @param result this contains the result of the generated XML
    *
    * @throws Exception this is thrown if there is an I/O error
    */ 
   public static OutputNode write(Writer result) throws Exception {
      return write(result, new Format());
   }

   /**
    * This is used to create an <code>OutputNode</code> that can be
    * used to write a well formed XML document. The writer specified
    * will have XML elements, attributes, and text written to it as
    * output nodes are created and populated.
    * 
    * @param result this contains the result of the generated XML
    * @param format this is the format to use for the document
    *
    * @throws Exception this is thrown if there is an I/O error
    */ 
   public static OutputNode write(Writer result, Format format) throws Exception {
      return new NodeWriter(result, format).writeRoot();
   }   
}
