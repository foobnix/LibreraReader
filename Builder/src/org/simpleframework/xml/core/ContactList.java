/*
 * ContactList.java April 2007
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

package org.simpleframework.xml.core;

import java.util.ArrayList;

/**
 * The <code>ContactList</code> object is used to represent a list
 * that contains contacts for an object. This is used to collect
 * the methods and fields within an object that are to be used in
 * the serialization and deserialization process.
 * 
 * @author Niall Gallagher
 */ 
abstract class ContactList extends ArrayList<Contact>  {

   /**
    * Constructor for the <code>ContactList</code> object. This
    * must be subclassed by a scanning class which will fill the
    * list with the contacts from a specified class.
    */      
   protected ContactList() {
      super();           
   }        
}
