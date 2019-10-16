/*
 * Order.java November 2007
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

package org.simpleframework.xml;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;

/**
 * The <code>Order</code> annotation is used to specify the order of
 * appearance of XML elements and attributes. When used it ensures 
 * that on serialization the XML generated is predictable. By default
 * serialization of fields is done in declaration order. 
 * 
 * @author Niall Gallagher
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Order {

   /**
    * Specifies the appearance order of the XML elements within the
    * generated document. This overrides the default order used, 
    * which is the declaration order within the class. If an element 
    * is not specified within this array then its order will be the
    * appearance order directly after the last specified element.
    * 
    * @return an ordered array of elements representing order
    */
   String[] elements() default {};
   
   /**
    * Specifies the appearance order of the XML attributes within 
    * the generated document. This overrides the default order used, 
    * which is the declaration order within the class. If an attribute 
    * is not specified within this array then its order will be the
    * appearance order directly after the last specified attribute.
    * 
    * @return an ordered array of attributes representing order
    */
   String[] attributes() default {};
}
