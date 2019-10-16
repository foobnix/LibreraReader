/*
 * ContactMap.java January 2010
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

package org.simpleframework.xml.core;

import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * The <code>ContactMap</code> object is used to keep track of the
 * contacts that have been processed. Keeping track of the contacts
 * that have been processed ensures that no two contacts are used
 * twice. This ensures a consistent XML class schema.
 * 
 * @author Niall Gallagher
 */
class ContactMap extends LinkedHashMap<Object, Contact> implements Iterable<Contact> {
   
   /**
    * This is used to iterate over the <code>Contact</code> objects
    * in a for each loop. Iterating over the contacts allows them
    * to be easily added to a list of unique contacts.
    * 
    * @return this is used to return the contacts registered
    */
   public Iterator<Contact> iterator(){
      return values().iterator();
   }
   
}