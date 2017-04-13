/*
 * StreamProvider.java January 2010
 *
 * Copyright (C) 2010, Niall Gallagher <niallg@users.sf.net>
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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;

/**
 * The <code>StreamProvider</code> object is used to provide event
 * reader implementations for StAX. Wrapping the mechanics of the
 * StAX framework within a <code>Provider</code> ensures that it can
 * be plugged in without any dependencies. This allows other parsers
 * to be swapped in should there be such a requirement.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.stream.StreamProvider
 */
class StreamProvider implements Provider {
   
   /**
    * This is the factory that is used to create StAX parsers.
    */
   private final XMLInputFactory factory;
   
   /**
    * Constructor for the <code>StreamProvider</code> object. This
    * is used to instantiate a parser factory that will be used to
    * create parsers when requested. Instantiating the factory up
    * front also checks that the framework is fully supported.
    */
   public StreamProvider() {
      this.factory = XMLInputFactory.newInstance();
   }

   /**
    * This provides an <code>EventReader</code> that will read from
    * the specified input stream. When reading from an input stream
    * the character encoding should be taken from the XML prolog or
    * it should default to the UTF-8 character encoding.
    * 
    * @param source this is the stream to read the document with
    * 
    * @return this is used to return the event reader implementation
    */
   public EventReader provide(InputStream source) throws Exception {
      return provide(factory.createXMLEventReader(source));
   }
   
   /**
    * This provides an <code>EventReader</code> that will read from
    * the specified reader. When reading from a reader the character
    * encoding should be the same as the source XML document.
    * 
    * @param source this is the reader to read the document with
    * 
    * @return this is used to return the event reader implementation
    */
   public EventReader provide(Reader source) throws Exception {
      return provide(factory.createXMLEventReader(source));
   }
   
   /**
    * This provides an <code>EventReader</code> that will read from
    * the specified reader. The returned event reader is basically
    * a wrapper for the provided StAX implementation.
    * 
    * @param source this is the reader to read the document with
    * 
    * @return this is used to return the event reader implementation
    */
   private EventReader provide(XMLEventReader source) throws Exception {
      return new StreamReader(source);
   }
}