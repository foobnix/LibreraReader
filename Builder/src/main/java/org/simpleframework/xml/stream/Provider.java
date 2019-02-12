/*
 * Provider.java January 2010
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

/**
 * The <code>Provider</code> object is used to represent the provider
 * of an XML parser. All XML parsers are represented as an event
 * reader much like the StAX event reader. Providing a interface to
 * the parser in this manner ensures that the core framework is not
 * coupled to any specific implementation and also ensures that it
 * should run in multiple environments that may support specific XML
 * parsers. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.stream.NodeBuilder
 */
interface Provider {

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
   EventReader provide(InputStream source) throws Exception;
   
   /**
    * This provides an <code>EventReader</code> that will read from
    * the specified reader. When reading from a reader the character
    * encoding should be the same as the source XML document.
    * 
    * @param source this is the reader to read the document with
    * 
    * @return this is used to return the event reader implementation
    */
   EventReader provide(Reader source) throws Exception;
}
