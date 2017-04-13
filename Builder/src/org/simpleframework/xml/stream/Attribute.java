/*
 * Attribute.java January 2010
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
 * The <code>Attribute</code> interface represents an attribute that
 * is associated with an event node. An attribute is required to 
 * provide the name and value for the attribute, and optionally the
 * namespace reference and prefix. For debugging purposes the source
 * object from the internal XML provider can be returned also.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.stream.EventNode
 */
interface Attribute {
   
   /**
    * This provides the name of the attribute. This should be the
    * name of the XML attribute without any namespace prefix. If
    * the name begins with "xml" then this attribute is reserved.
    * according to the namespaces for XML 1.0 specification.
    * 
    * @return this returns the name of this attribute object
    */
   String getName();
   
   /**
    * This returns the value of the event. Typically this will be
    * the text value that the attribute contains. If the attribute
    * does not contain a value then this returns null. 
    * 
    * @return this returns the value represented by this attribute
    */
   String getValue();
   
   /**
    * This is used to acquire the namespace reference that this 
    * attribute is in. A namespace is normally associated with an
    * attribute if that attribute is prefixed with a known token.
    * If there is no prefix then this will return null.
    * 
    * @return this provides the associated namespace reference
    */
   String getReference();
   
   /**
    * This is used to acquire the namespace prefix associated with
    * this attribute. A prefix is used to qualify the attribute
    * within a namespace. So, if this has a prefix then it should
    * have a reference associated with it.
    * 
    * @return this returns the namespace prefix for the attribute
    */
   String getPrefix();
   
   /**
    * This is used to return the source of the attribute. Depending 
    * on which provider was selected to parse the XML document an
    * object for the internal parsers representation of this will
    * be returned. This is useful for debugging purposes.
    * 
    * @return this will return the source object for this event
    */
   Object getSource();
   
   /**
    * This returns true if the attribute is reserved. An attribute
    * is considered reserved if it begins with "xml" according to 
    * the namespaces in XML 1.0 specification. Such attributes are
    * used for namespaces and other such details.
    *
    * @return this returns true if the attribute is reserved
    */
   boolean isReserved();
}