/*
 * NamespaceMap.java July 2008
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

import java.util.Iterator;

/**
 * The <code>NamespaceMap</code> object is used store the namespaces
 * for an element. Each namespace added to this map can be added
 * with a prefix. A prefix is added only if the associated reference
 * has not been added to a parent element. If a parent element has
 * the associated reference, then the parents prefix is the one that
 * will be returned when requested from this map. 
 * 
 * @author Niall Gallagher
 */
public interface NamespaceMap extends Iterable<String> {
   
   /**
    * This is the prefix that is associated with the source element.
    * If the source element does not contain a namespace reference
    * then this will return its parents namespace. This ensures 
    * that if a namespace has been declared its child elements will
    * inherit its prefix.
    * 
    * @return this returns the prefix that is currently in scope
    */
   String getPrefix();

   /**
    * This acquires the prefix for the specified namespace reference.
    * If the namespace reference has been set on this node with a
    * given prefix then that prefix is returned, however if it has
    * not been set this will search the parent elements to find the
    * prefix that is in scope for the specified reference.
    * 
    * @param reference the reference to find a matching prefix for
    * 
    * @return this will return the prefix that is is scope
    */
   String getPrefix(String reference);
   
   /**
    * This acquires the namespace reference for the specified prefix.
    * If the provided prefix has been set on this node with a given
    * reference then that reference is returned, however if it has
    * not been set this will search the parent elements to find the
    * reference that is in scope for the specified reference.
    * 
    * @param prefix the prefix to find a matching reference for
    * 
    * @return this will return the reference that is is scope
    */
   String getReference(String prefix);

   /**
    * This returns an iterator for the namespace of all the nodes 
    * in this <code>NamespaceMap</code>. This allows the namespaces 
    * to be iterated within a for each loop in order to extract the
    * prefix values associated with the map.
    *
    * @return this returns the namespaces contained in this map
    */ 
   Iterator<String> iterator();
   
   /**
    * This is used to add the namespace reference to the namespace
    * map. If the namespace has been added to a parent node then
    * this will not add the reference. The prefix added to the map
    * will be the default namespace, which is an empty prefix.
    * 
    * @param reference this is the reference to be added 
    * 
    * @return this returns the prefix that has been replaced
    */
   String setReference(String reference);
   
   /**
    * This is used to add the namespace reference to the namespace
    * map. If the namespace has been added to a parent node then
    * this will not add the reference. 
    * 
    * @param reference this is the reference to be added 
    * @param prefix this is the prefix to be added to the reference
    * 
    * @return this returns the prefix that has been replaced
    */
   String setReference(String reference, String prefix);
}
