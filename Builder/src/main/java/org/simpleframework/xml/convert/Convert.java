/*
 * Convert.java January 2010
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

package org.simpleframework.xml.convert;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The <code>Convert</code> annotation is used to specify a converter
 * class to use for serialization. This annotation is used when an
 * object needs to be serialized but can not be annotated or when the
 * object can not conform to an existing XML structure. In order to
 * specify a <code>Converter</code> object a field or method can be
 * annotated like the field below.
 * <pre>
 * 
 *    &#64;Element
 *    &#64;Convert(ExampleConverter.class)
 *    private Example example;
 * 
 * </pre>
 * Note that for the above field the <code>Element</code> annotation
 * is required. If this is used with any other XML annotation such 
 * as the <code>ElementList</code> or <code>Text</code> annotation
 * then an exception will be thrown. As well as field and methods
 * this can be used to suggest a converter for a class. Take the 
 * class below which is annotated.
 * <pre>
 * 
 *    &#64;Root
 *    &#64;Convert(DemoConverter.class)
 *    public class Demo {
 *       ...
 *    }
 * 
 * </pre>
 * For the above class the specified converter will be used. This is
 * useful when the class is used within a <code>java.util.List</code>
 * or another similar collection. Finally, in order for this to work
 * it must be used with the <code>AnnotationStrategy</code> which is
 * used to scan for annotations in order to delegate to converters.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.convert.AnnotationStrategy
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Convert {
   
   /**
    * Specifies the <code>Converter</code> implementation to be used
    * to convert the annotated object. The converter specified will
    * be used to convert the object to XML by intercepting the 
    * serialization and deserialization process as it happens. A
    * converter should typically be used to handle an object of 
    * a specific type.
    * 
    * @return this returns the converter that has been specified
    */
   Class<? extends Converter> value();
}
