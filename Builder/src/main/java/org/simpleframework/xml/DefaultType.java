/*
 * DefaultType.java January 2010
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

package org.simpleframework.xml;

/**
 * The <code>DefaultType</code> enumeration is used to specify the
 * type of defaults to apply to a class. The <code>Default</code> 
 * annotation is used to specify which type of default to apply. If
 * applied the serializer will synthesize an XML annotation for the 
 * fields or properties of the object. The synthesized annotations
 * will have default values for its attributes.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.Default
 */
public enum DefaultType {
   
   /**
    * This tells the serializer to default all member fields.
    */
   FIELD,
   
   /**
    * This tells the serializer to default all property methods.
    */
   PROPERTY
}
