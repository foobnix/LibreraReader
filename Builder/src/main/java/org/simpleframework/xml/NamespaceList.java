/*
 * NamespaceList.java July 2008
 *
 * Copyright (C) 2008, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>NamespaceList</code> annotation that is used to declare
 * namespaces that can be added to an element. This is used when 
 * there are several namespaces to add to the element without setting
 * any namespace to the element. This is useful when the scope of a
 * namespace needs to span several nodes. All prefixes declared in 
 * the namespaces will be available to the child nodes.
 * <pre>
 * 
 *    &lt;example xmlns:root="http://www.example.com/root"&gt;
 *       &lt;anonymous&gt;anonymous element&lt;/anonymous&gt;
 *    &lt;/example&gt;
 *    
 * </pre>
 * The above XML example shows how a prefixed namespace has been added
 * to the element without qualifying that element. Such declarations
 * will allow child elements to pick up the parents prefix when this
 * is required, this avoids having to redeclare the same namespace.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.Namespace
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface NamespaceList {

   /**
    * This is used to acquire the namespaces that are declared on
    * the class. Any number of namespaces can be declared. None of
    * the declared namespaces will be made the elements namespace,
    * instead it will simply declare the namespaces so that the
    * reference URI and prefix will be made available to children.
    * 
    * @return this returns the namespaces that are declared.
    */
   Namespace[] value() default {};
}
