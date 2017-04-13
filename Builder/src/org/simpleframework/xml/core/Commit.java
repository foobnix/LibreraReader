/*
 * Commit.java July 2006
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
 * The <code>Commit</code> annotation is used to mark a method within
 * a serializable object that requires a callback from the persister
 * once the deserialization completes. The commit method is invoked
 * by the <code>Persister</code> after all fields have been assigned
 * and after the validation method has been invoked, if the object
 * has a method marked with the <code>Validate</code> annotation.
 * <p>
 * Typically the commit method is used to complete deserialization
 * by allowing the object to build further data structures from the
 * fields that have been created from the deserialization process.
 * The commit method must be a no argument method or a method that
 * takes a single <code>Map</code> object argument, and may throw an
 * exception, in which case the deserialization process terminates.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Validate
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Commit {
}
