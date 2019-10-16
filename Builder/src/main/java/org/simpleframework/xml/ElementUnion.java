/*
 * ElementUnion.java March 2011
 *
 * Copyright (C) 2011, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>ElementUnion</code> annotation is used to describe fields 
 * and methods that can dynamically match a schema class. Each union
 * can have a number of different XML class schemas matched based on
 * an XML element name or the instance type. This provides a means
 * of expressing a logical OR. By annotating a field or method as a
 * union it can take multiple forms. For example.
 * <pre>
 * 
 *    &#64;ElementUnion({
 *       &#64;Element(name="circle", type=Circle.class),
 *       &#64;Element(name="square", type=Square.class)       
 *    })
 *    private Shape shape;
 *    
 * </pre>
 * For the above definition the <code>Shape</code> field can take
 * be any of the declared types. On deserialization the name of the
 * element will determine the type that is instantiated and the XML
 * structure to be consumed. For serialization the instance type will
 * determine the name of the element the object will serialized as. 
 * This provides a useful means of consume more complicated sources. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.Element
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ElementUnion {
   
   /**
    * This provides the <code>Element</code> annotations that have 
    * been defined for this union. Each element describes the XML
    * class schema to use and the name of the XML element. This 
    * allows the serialization process to determine which elements
    * map to the defined types. Also, the types define how the XML
    * is generated for a given instance.     
    * 
    * @return the elements defined for the union declaration
    */
   Element[] value();
}
