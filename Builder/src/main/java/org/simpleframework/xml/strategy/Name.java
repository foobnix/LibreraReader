/*
 * Attributes.java January 2010
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

package org.simpleframework.xml.strategy;

/**
 * This contains the default attribute names to use to populate the
 * XML elements with data relating to the object to be serialized. 
 * Various details, such as the class name of an object need to be
 * written to the element in order for it to be deserialized. Such
 * attribute names are shared between strategy implementations.
 * 
 * @author Niall Gallagher
 */
interface Name {
   
   /**
    * The default name of the attribute used to identify an object.
    */
   public static final String MARK = "id";
   
   /**
    * The default name of the attribute used for circular references.
    */
   public static final String REFER = "reference";
   
   /**
    * The default name of the attribute used to specify the length.
    */
   public static final String LENGTH = "length";
   
   /**
    * The default name of the attribute used to specify the class.
    */
   public static final String LABEL = "class"; 
}
