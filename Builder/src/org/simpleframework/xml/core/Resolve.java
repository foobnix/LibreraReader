/*
 * Resolve.java June 2007
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

package org.simpleframework.xml.core;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;

/**
 * The <code>Resolve</code> method is used to resolve an object that
 * has been deserialized from the XML document. This is used when the
 * deserialized object whats to provide a substitute to itself within
 * the object graph. This is particularly useful when an object is
 * used to reference an external XML document, as it allows that XML
 * document to be deserialized in to a new object instance.
 * <p>
 * This is similar to the <code>readResolve</code> method used within
 * Java Object Serialization in that it is used to create a object to 
 * plug in to the object graph after it has been fully deserialized.
 * Care should be taken when using this annotation as the object that
 * is returned from the resolve method must match the field type such
 * that the resolved object is an assignable substitute.
 * 
 * @author Niall Gallagher
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Resolve {  
}