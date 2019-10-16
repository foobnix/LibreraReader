/*
 * EventToken.java January 2010
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

import java.util.Iterator;

/**
 * The <code>EventToken</code> object is used to represent an event
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
abstract class EventToken implements EventNode  {

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
    * This provides the name of the event. Typically this will be
    * the name of an XML element if the event represents an element.
    * If however the event represents a text token or an element
    * close token then this method may return null for the name.
    * 
    * @return this returns the name of this event or null
    */
   public String getName() {
      return null;
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
    * This is used to acquire the namespace reference that this 
    * node is in. A namespace is normally associated with an XML
    * element or attribute, so text events and element close events
    * are not required to contain any namespace references. 
    * 
    * @return this will provide the associated namespace reference
    */
   public String getReference() {
      return null;
   }
   
   /**
    * This is used to acquire the namespace prefix associated with
    * this node. A prefix is used to qualify an XML element or
    * attribute within a namespace. So, if this represents a text
    * event then a namespace prefix is not required.
    * 
    * @return this returns the namespace prefix for this event
    */
   public String getPrefix() {
      return null;
   }
   
   /**
    * This is used to return the source of the event. Depending on
    * which provider was selected to parse the XML document an
    * object for the internal parsers representation of the event
    * will be returned. This is useful for debugging purposes.
    * 
    * @return this will return the source object for this event
    */
   public Object getSource() {
      return null;
   }

   /**
    * This is used to acquire the <code>Attribute</code> objects
    * that are associated with this event. Attributes are typically
    * associated wit start events. So, if the node is not a start
    * event his may return a null value or an empty iterator.
    * 
    * @return this returns an iterator for iterating attributes
    */
   public Iterator<Attribute> iterator() {
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
      return false;
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