/*
 * CacheParameter.java April 2012
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

package org.simpleframework.xml.core;

import java.lang.annotation.Annotation;

/**
 * The <code>CacheParameter</code> object represents a parameter 
 * which caches its values internally. As well as caching parameter
 * values this also caches a key from a <code>Label</code> object
 * which ties the parameter an label together making it possible
 * to reference each other from hash containers.
 * 
 * @author Niall Gallagher
 */
class CacheParameter implements Parameter{

   /**
    * This is the annotation used to represent this parameter.
    */
   private final Annotation annotation;
   
   /**
    * This is the XPath expression used to represent the parameter.
    */
   private final Expression expression;
   
   /**
    * This is the name of the element or attribute for the parameter.
    */
   private final String name;
   
   /**
    * This is the path of the element or attribute for the parameter.
    */
   private final String path;
   
   /**
    * This is the string representation of this parameter object.
    */
   private final String string;
   
   /**
    * This is the type within the constructor of this parameter.
    */
   private final Class type;
   
   /**
    * This is the key that uniquely identifies this parameter.
    */
   private final Object key;
   
   /**
    * This is the index within the constructor for the parameter.
    */
   private final int index;
   
   /**
    * Determines if this parameter represents a primitive value.
    */
   private final boolean primitive;
   
   /**
    * This is true if this parameter is required to exist.
    */
   private final boolean required;
   
   /**
    * This is true if this parameter represents an attribute.
    */
   private final boolean attribute;
   
   /**
    * This is true if this parameter represents a text value.
    */
   private final boolean text;
   
   /**
    * Constructor for the <code>CacheParameter</code> object. This
    * is used to create a parameter that internally caches all of
    * the information of the provided parameter and also makes 
    * use of the label provided to generate a unique key.
    * 
    * @param value this is the parameter to cache values from
    * @param label this is the label to acquire the key from
    */
   public CacheParameter(Parameter value, Label label) throws Exception {
      this.annotation = value.getAnnotation();
      this.expression = value.getExpression();
      this.attribute = value.isAttribute();
      this.primitive = value.isPrimitive();
      this.required = label.isRequired();
      this.string = value.toString();
      this.text = value.isText();
      this.index = value.getIndex();
      this.name = value.getName();
      this.path = value.getPath();
      this.type = value.getType();
      this.key = label.getKey();
   }
   
   /**
    * This is the key used to represent the parameter. The key is
    * used to store the parameter in hash containers. Unlike the
    * path is not necessarily the path for the parameter.
    * 
    * @return this is the key used to represent the parameter
    */
   public Object getKey() {
      return key;
   }

   /**
    * This is used to acquire the annotated type class. The class
    * is the type that is to be deserialized from the XML. This
    * is used to validate against annotated fields and methods.
    * 
    * @return this returns the type used for the parameter
    */
   public Class getType() {
      return type;
   }

   /**
    * This returns the index position of the parameter in the
    * constructor. This is used to determine the order of values
    * that are to be injected in to the constructor.
    * 
    * @return this returns the index for the parameter
    */
   public int getIndex() {
      return index;
   }

   /**
    * This is used to acquire the annotation that is used for the
    * parameter. The annotation provided will be an XML annotation
    * such as the <code>Element</code> or <code>Attribute</code>
    * annotation.
    * 
    * @return this returns the annotation used on the parameter
    */
   public Annotation getAnnotation() {
      return annotation;
   }

   /**
    * This method is used to return an XPath expression that is 
    * used to represent the position of this parameter. If there is 
    * no XPath expression associated with this then an empty path 
    * is returned. This will never return a null expression.
    * 
    * @return the XPath expression identifying the location
    */
   public Expression getExpression() {
      return expression;
   }

   /**
    * This is used to acquire the name of the parameter that this
    * represents. The name is determined using annotation and 
    * the name attribute of that annotation, if one is provided.
    * 
    * @return this returns the name of the annotated parameter
    */
   public String getName() {
      return name;
   }

   /**
    * This is used to acquire the path of the element or attribute
    * represented by this parameter. The path is determined by
    * acquiring the XPath expression and appending the name of the
    * label to form a fully qualified path.
    * 
    * @return returns the path that is used for this parameter
    */
   public String getPath() {
      return path;
   }

   /**
    * This is used to determine if the parameter is required. If 
    * an attribute is not required then it can be null. Which 
    * means that we can inject a null value. Also, this means we
    * can match constructors in a more flexible manner.
    * 
    * @return this returns true if the parameter is required
    */
   public boolean isRequired() {
      return required;
   }

   /**
    * This is used to determine if the parameter is primitive. A
    * primitive parameter must not be null. As there is no way to
    * provide the value to the constructor. A default value is 
    * not a good solution as it affects the constructor score.
    * 
    * @return this returns true if the parameter is primitive
    */
   public boolean isPrimitive() {
      return primitive;
   }

   /**
    * This method is used to determine if the parameter represents 
    * an attribute. This is used to style the name so that elements
    * are styled as elements and attributes are styled as required.
    * 
    * @return this is used to determine if this is an attribute
    */
   public boolean isAttribute() {
      return attribute;
   }

   /**
    * This is used to determine if the parameter represents text. 
    * If this represents text it typically does not have a name,
    * instead the empty string represents the name. Also text
    * parameters can not exist with other text parameters.
    * 
    * @return returns true if this parameter represents text
    */
   public boolean isText() {
      return text;
   }
   
   /**
    * This is used to provide a textual representation of the 
    * parameter. Providing a string describing the parameter is
    * useful for debugging and for exception messages.
    * 
    * @return this returns the string representation for this
    */
   public String toString() {
      return string;
   }  
}