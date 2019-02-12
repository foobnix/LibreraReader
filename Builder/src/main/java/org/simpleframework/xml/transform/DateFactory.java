/*
 * DateTransform.java May 2007
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

package org.simpleframework.xml.transform;

import java.lang.reflect.Constructor;
import java.util.Date;

/**
 * The <code>DateFactory</code> object is used to create instances
 * or subclasses of the <code>Date</code> object. This will create
 * the instances of the date objects using a constructor that takes
 * a single <code>long</code> parameter value. 
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.transform.DateTransform
 */
class DateFactory<T extends Date> {
   
   /**
    * This is used to create instances of the date object required.
    */
   private final Constructor<T> factory;
   
   /**
    * Constructor for the <code>DateFactory</code> object. This is
    * used to create instances of the specified type. All objects
    * created by this instance must take a single long parameter.
    * 
    * @param type this is the date implementation to be created
    */
   public DateFactory(Class<T> type) throws Exception {
      this(type, long.class);
   }
   
   /**
    * Constructor for the <code>DateFactory</code> object. This is
    * used to create instances of the specified type. All objects
    * created by this instance must take the specified parameter.
    * 
    * @param type this is the date implementation to be created
    * @param list is basically the list of accepted parameters
    */
   public DateFactory(Class<T> type, Class... list) throws Exception {
      this.factory = type.getDeclaredConstructor(list);
   }
   
   /**
    * This is used to create instances of the date using a delegate
    * date. A <code>long</code> parameter is extracted from the 
    * given date an used to instantiate a date of the required type.
    * 
    * @param list this is the type used to provide the long value
    * 
    * @return this returns an instance of the required date type
    */
   public T getInstance(Object... list) throws Exception {
      return factory.newInstance(list);
   }
}