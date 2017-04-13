/*
 * Namespace.java July 2008
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
 * The <code>Namespace</code> annotation is used to set a namespace
 * on an element or attribute. By annotating a method, field or 
 * class with this annotation that entity assumes the XML namespace
 * provided. When used on a class the annotation describes the
 * namespace that should be used, this however can be overridden by
 * an annotated field or method declaration of that type.
 * <pre>
 *  
 *    &lt;book:book xmlns:book="http://www.example.com/book"&gt;
 *       &lt;author&gt;saurabh&lt;/author&gt;
 *       &lt;title&gt;example title&lt;/title&gt;
 *       &lt;isbn&gt;ISB-16728-10&lt;/isbn&gt;
 *    &lt;/book:book&gt;
 *
 * </pre>
 * In the above XML snippet a namespace has been declared with the
 * prefix "book" and the reference "http://www.example.com/book". If
 * such a namespace is applied to a class, method, or field then 
 * that element will contain the namespace and the element name will
 * be prefixed with a namespace qualifier, which is "book" here.
 * <pre>
 *
 *    &lt;example xmlns="http://www.example.com/root"&gt;
 *       &lt;child&gt;
 *          &lt;text xmlns=""&gt;text element&lt;/text&gt;
 *       &lt;/child&gt;
 *    &lt;/example&gt;
 *
 * </pre>
 * In order for a namespace to be inherited it must be specified as
 * a default namespace. A default namespace is one that does not have
 * a prefix. All elements that do not have an explicit namespace will
 * inherit the last default namespace in scope. For details see
 * <a href='http://www.w3.org/TR/xml-names/#defaulting'>Section 6.2</a>
 * of the namespaces in XML 1.0 specification. To remove the default
 * namespace simply specify a namespace with no prefix or reference,
 * such as the "text" element in the above example.  
 *
 * @author Niall Gallagher
 */ 
@Retention(RetentionPolicy.RUNTIME)
public @interface Namespace {

   /**
    * This is used to specify the unique reference URI that is used 
    * to define the namespace within the document. This is typically
    * a URI as this is a well know universally unique identifier. 
    * It can be anything unique, but typically should be a unique
    * URI reference. If left as the empty string then this will
    * signify that the anonymous namespace will be used.
    *
    * @return this returns the reference used by this namespace    
    */         
   String reference() default "";

   /**
    * This is used to specify the prefix used for the namespace. If
    * no prefix is specified then the reference becomes the default
    * namespace for the enclosing element. This means that all 
    * attributes and elements that do not contain a prefix belong
    * to the namespace declared by this annotation.
    *
    * @return this returns the prefix used for this namespace
    */ 
   String prefix() default "";
}
