/*
 * Default.java January 2010
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The <code>Default</code> annotation is used to specify that all
 * fields or methods should be serialized in a default manner. This
 * basically allows an objects fields or properties to be serialized
 * without the need to annotate them. This has advantages if the
 * format of the serialized object is not important, as it allows
 * the object to be serialized with a minimal use of annotations.
 * <pre>
 * 
 *    &#64;Root
 *    &#64;Default(DefaultType.FIELD)
 *    public class Example {
 *       ...
 *    }
 * 
 * </pre>
 * Defaults can be applied to either fields or property methods. If
 * this annotation is applied to a class, certain fields or methods
 * can be ignored using the <code>Transient</code> annotation. If a
 * member is marked as transient then it will not be serialized. The
 * defaults are applied only to those members that are not otherwise
 * annotated with an XML annotation.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.Transient
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {
   
   /**
    * This method is used to return the type of default that is to
    * be applied to the class. Defaults can be applied to either
    * fields or property methods. Any member with an XML annotation
    * will not be treated as a default. 
    * 
    * @return this returns the type of defaults to be applied
    */
   DefaultType value() default DefaultType.FIELD;
   
   /**
    * This is used to determine if the generated annotations are
    * required or not. By default generated parameters are required.
    * Setting this to false means that null values are accepted
    * by all defaulted fields or methods depending on the type.
    * 
    * @return this is used to determine if defaults are required
    */
   boolean required() default true;
}