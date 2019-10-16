/*
 * Validate.java July 2006
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
 * The <code>Validate</code> annotation is used to mark a method in
 * a serializable object that requires a callback from the persister
 * once the deserialization completes. The validate method is invoked
 * by the <code>Persister</code> after all fields have been assigned
 * and before the commit method is invoked.
 * <p>
 * Typically the validate method is used to validate the fields that
 * have been assigned once deserialization has been completed. The
 * validate method must be a no argument public method or a method
 * that takes a <code>Map</code> as the only argument. When invoked 
 * the object can determine whether the fields are valid, if the  
 * field values do not conform to the objects requirements then the
 * method can throw an exception to terminate deserialization.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Commit
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Validate {
}
