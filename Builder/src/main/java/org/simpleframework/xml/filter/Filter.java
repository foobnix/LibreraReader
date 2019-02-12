/*
 * Filter.java May 2006
 *
 * Copyright (C) 2006, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.xml.filter;

/**
 * The <code>Filter</code> object is used to provide replacement string
 * values for a provided key. This allows values within the XML source
 * document to be replaced using sources such as OS environment variables
 * and Java system properties.
 * <p>
 * All filtered variables appear within the source text using a template
 * and variable keys marked like <code>${example}</code>. When the XML 
 * source file is read all template variables are replaced with the 
 * values provided by the filter. If no replacement exists then the XML
 * source text remains unchanged.
 * 
 * @author Niall Gallagher
 */
public interface Filter {

   /**
    * Replaces the text provided with some property. This method 
    * acts much like a the get method of the <code>Map</code>
    * object, in that it uses the provided text as a key to some 
    * value. However it can also be used to evaluate expressions
    * and output the result for inclusion in the generated XML.
    *
    * @param text this is the text value that is to be replaced
    * 
    * @return returns a replacement for the provided text value
    */
   String replace(String text);        
}
