/*
 * Function.java February 2008
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

package org.simpleframework.xml.core;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * The <code>Function</code> object is used to encapsulated the method
 * that is called when serializing an object. This contains details 
 * on the type of method represented and ensures that reflection is
 * not required each time the method is to be invoked.
 * 
 * @author Niall Gallagher
 */
class Function {
   
   /**
    * This is the method that is to be invoked by the function.
    */
   private final Method method;
   
   /**
    * This is used to determine if the method takes the map.
    */
   private final boolean contextual;
   
   /**
    * Constructor for the <code>Function</code> object. This is used
    * to create an object that wraps the provided method it ensures
    * that no reflection is required when the method is to be called.
    * 
    * @param method this is the method that is to be wrapped by this
    */
   public Function(Method method) {
      this(method, false);
   }
   
   /**
    * Constructor for the <code>Function</code> object. This is used
    * to create an object that wraps the provided method it ensures
    * that no reflection is required when the method is to be called.
    * 
    * @param method this is the method that is to be wrapped by this
    * @param contextual determines if the method is a contextual one
    */
   public Function(Method method, boolean contextual) {
      this.contextual = contextual;
      this.method = method;
   }
   
   /**
    * This method used to invoke the callback method of the provided
    * object. This will acquire the session map from the context. If
    * the provided object is not null then this will return null.
    * 
    * @param context this is the context that contains the session
    * @param source this is the object to invoke the function on
    * 
    * @return this returns the result of the method invocation
    */
   public Object call(Context context, Object source) throws Exception {
      if(source != null) {
         Session session = context.getSession();
         Map table = session.getMap();
      
         if(contextual) {              
            return method.invoke(source, table);           
         }
         return method.invoke(source);
      }
      return null;
   }
}