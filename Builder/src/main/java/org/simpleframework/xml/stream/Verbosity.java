/*
 * Verbosity.java July 2012
 *
 * Copyright (C) 2012, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>Verbosity</code> enumeration is used to specify a verbosity
 * preference for the resulting XML. Typically the verbosity preference
 * is used when serializing an object that does not have explicit XML
 * annotations associated with a type. In such a scenario this will
 * indicate whether a high verbosity level is required or a low one.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.stream.Format
 */
public enum Verbosity {
   
   /**
    * This specifies a preference for elements over attributes.
    */
   HIGH,
   
   /**
    * This specifies a preference for attributes over elements.
    */
   LOW;
}
