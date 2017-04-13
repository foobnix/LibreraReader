/*
 * EventElement.java January 2010
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

import java.util.ArrayList;

/**
 * The <code>EventElement</code> object is used to represent an event
 * that has been extracted from the XML document. Events provide a
 * framework neutral way to represent a token from the source XML.
 * It provides the name and value of the event, if applicable, and
 * also provides namespace information. Some nodes will have 
 * associated <code>Attribute</code> objects, typically these will 
 * be the XML element events. Also, if available, the event will 
 * provide the line number the event was encountered in the XML.
 * 
 * @author Niall Gallagher
 */
abstract class EventElement extends ArrayList<Attribute> implements EventNode {

   /**
    * This is used to provide the line number the XML event was
    * encountered at within the XML document. If there is no line
    * number available for the node then this will return a -1.
    * 
    * @return this returns the line number if it is available
    */
   public int getLine() {
      return -1;
   }

   /**
    * This returns the value of the event. Typically this will be
    * the text value that the token contains. If the event does 
    * not contain a value then this returns null. Only text events
    * are required to produce a value via this methods. 
    * 
    * @return this returns the value represented by this event
    */
   public String getValue() {
      return null;
   }

   /**
    * This is true when the node represents an element close. Such
    * events are required by the core reader to determine if a 
    * node is still in context. This helps to determine if there
    * are any more children to be read from a specific node.
    * 
    * @return this returns true if the event is an element close
    */
   public boolean isEnd() {
      return false;
   }

   /**
    * This is true when the node represents a new element. This is
    * the core event type as it contains the element name and any
    * associated attributes. The core reader uses this to compose
    * the input nodes that are produced.
    * 
    * @return this returns true if the event represents an element
    */
   public boolean isStart() {
      return true;
   }

   /**
    * This is true when the node represents a text token. Text 
    * tokens are required to provide a value only. So namespace
    * details and the node name will typically return null.
    *  
    * @return this returns true if this represents text  
    */
   public boolean isText() {
      return false;
   }
}