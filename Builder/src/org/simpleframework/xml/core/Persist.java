/*
 * Persist.java July 2006
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

package org.simpleframework.xml.core;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;

/**
 * The <code>Persist</code> annotation is used to mark a method that
 * requires a callback from the persister before serialization of
 * an object begins. If a method is marked with this annotation then
 * it will be invoked so that it can prepare the object for the
 * serialization process.
 * <p>
 * The persist method can be used to perform any preparation needed
 * before serialization. For example, should the object be a list
 * or table of sorts the persist method can be used to grab a lock
 * for the internal data structure. Such a scheme will ensure that
 * the object is serialized in a known state. The persist method
 * must be a no argument public method or a method that takes a 
 * single <code>Map</code> argument, it may throw an exception to 
 * terminate the serialization process if required.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Complete
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Persist {
}
