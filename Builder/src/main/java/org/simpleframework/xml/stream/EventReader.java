/*
 * EventReader.java January 2010
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

/**
 * The <code>EventReader</code> interface is used to represent an XML
 * reader that can be used to read a source document. This provides
 * a convenient abstraction that can be used by any number of parser
 * implementations. In essence it is similar to the Streaming API for
 * XML, however other implementations can easily be adapted.
 * 
 * @author Niall Gallagher
 */
interface EventReader {

   /**
    * This is used to take the next node from the document. This will
    * scan through the document, ignoring any comments to find the
    * next relevant XML event to acquire. Typically events will be
    * the start and end of an element, as well as any text nodes.
    * 
    * @return this returns the next event taken from the source XML
    */
   EventNode next() throws Exception;
   
   /**
    * This is used to peek at the node from the document. This will
    * scan through the document, ignoring any comments to find the
    * next relevant XML event to acquire. Typically events will be
    * the start and end of an element, as well as any text nodes.
    * 
    * @return this returns the next event taken from the source XML
    */
   EventNode peek() throws Exception;
}
