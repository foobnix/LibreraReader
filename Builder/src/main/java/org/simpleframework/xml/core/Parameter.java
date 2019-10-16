/*
 * Parameter.java July 2009
 *
 * Copyright (C) 2009, Niall Gallagher <niallg@users.sf.net>
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

import java.lang.annotation.Annotation;

/**
 * The <code>Parameter</code> is used to represent a constructor 
 * parameter. It contains the XML annotation used on the parameter
 * as well as the name of the parameter and its position index.
 * A parameter is used to validate against the annotated methods 
 * and fields and also to determine the deserialized values that
 * should be injected in to the constructor to instantiate it.
 * 
 * @author Niall Gallagher
 */
interface Parameter {
   
   /**
    * This is the key used to represent the parameter. The key is
    * used to store the parameter in hash containers. Unlike the
    * path is not necessarily the path for the parameter.
    * 
    * @return this is the key used to represent the parameter
    */
   Object getKey();
   
   /**
    * This is used to acquire the annotated type class. The class
    * is the type that is to be deserialized from the XML. This
    * is used to validate against annotated fields and methods.
    * 
    * @return this returns the type used for the parameter
    */
   Class getType();
   
   /**
    * This returns the index position of the parameter in the
    * constructor. This is used to determine the order of values
    * that are to be injected in to the constructor.
    * 
    * @return this returns the index for the parameter
    */
   int getIndex();
   
   /**
    * This is used to acquire the annotation that is used for the
    * parameter. The annotation provided will be an XML annotation
    * such as the <code>Element</code> or <code>Attribute</code>
    * annotation.
    * 
    * @return this returns the annotation used on the parameter
    */
   Annotation getAnnotation();

   /**
    * This method is used to return an XPath expression that is 
    * used to represent the position of this parameter. If there is 
    * no XPath expression associated with this then an empty path 
    * is returned. This will never return a null expression.
    * 
    * @return the XPath expression identifying the location
    */
   Expression getExpression(); 
   
   /**
    * This is used to acquire the name of the parameter that this
    * represents. The name is determined using annotation and 
    * the name attribute of that annotation, if one is provided.
    * 
    * @return this returns the name of the annotated parameter
    */
   String getName();
   
   /**
    * This is used to acquire the path of the element or attribute
    * represented by this parameter. The path is determined by
    * acquiring the XPath expression and appending the name of the
    * label to form a fully qualified path.
    * 
    * @return returns the path that is used for this parameter
    */
   String getPath();
   
   /**
    * This is used to determine if the parameter is required. If 
    * an attribute is not required then it can be null. Which 
    * means that we can inject a null value. Also, this means we
    * can match constructors in a more flexible manner.
    * 
    * @return this returns true if the parameter is required
    */
   boolean isRequired();
   
   /**
    * This is used to determine if the parameter is primitive. A
    * primitive parameter must not be null. As there is no way to
    * provide the value to the constructor. A default value is 
    * not a good solution as it affects the constructor score.
    * 
    * @return this returns true if the parameter is primitive
    */
   boolean isPrimitive();
   
   /**
    * This method is used to determine if the parameter represents 
    * an attribute. This is used to style the name so that elements
    * are styled as elements and attributes are styled as required.
    * 
    * @return this is used to determine if this is an attribute
    */
   boolean isAttribute();
   
   /**
    * This is used to determine if the parameter represents text. 
    * If this represents text it typically does not have a name,
    * instead the empty string represents the name. Also text
    * parameters can not exist with other text parameters.
    * 
    * @return returns true if this parameter represents text
    */
   boolean isText();
   
   /**
    * This is used to provide a textual representation of the 
    * parameter. Providing a string describing the parameter is
    * useful for debugging and for exception messages.
    * 
    * @return this returns the string representation for this
    */
   String toString();
}
