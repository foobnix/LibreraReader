/*
 * Caller.java June 2007
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

/**
 * The <code>Caller</code> acts as a means for the schema to invoke
 * the callback methods on an object. This ensures that the correct
 * method is invoked within the schema class. If the annotated method
 * accepts a map then this will provide that map to the method. This
 * also ensures that if specific annotation is not present in the 
 * class that no action is taken on a persister callback. 
 * 
 * @author Niall Gallagher
 */
class Caller {
   
   /**
    * This is the pointer to the schema class commit function.
    */
   private final Function commit;

   /**
    * This is the pointer to the schema class validation function.
    */
   private final Function validate;

   /**
    * This is the pointer to the schema class persist function.
    */
   private final Function persist;

   /**
    * This is the pointer to the schema class complete function.
    */
   private final Function complete;
   
   /**
    * This is the pointer to the schema class replace function.
    */
   private final Function replace;
   
   /**
    * This is the pointer to the schema class resolve function.
    */
   private final Function resolve;
   
   /**
    * This is the context that is used to invoke the functions.
    */
   private final Context context;
   
   /**
    * Constructor for the <code>Caller</code> object. This is used 
    * to wrap the schema class such that callbacks from the persister
    * can be dealt with in a seamless manner. This ensures that the
    * correct function and arguments are provided to the functions.
    * element and attribute XML annotations scanned from
    * 
    * @param schema this is the scanner that contains the functions
    * @param context this is the context used to acquire the session
    */
   public Caller(Scanner schema, Context context) {     
      this.validate = schema.getValidate();      
      this.complete = schema.getComplete();
      this.replace = schema.getReplace();
      this.resolve = schema.getResolve();
      this.persist = schema.getPersist();  
      this.commit = schema.getCommit();  
      this.context = context;
   }
   
   /**
    * This is used to replace the deserialized object with another
    * instance, perhaps of a different type. This is useful when an
    * XML schema class acts as a reference to another XML document
    * which needs to be loaded externally to create an object of
    * a different type.
    * 
    * @param source the source object to invoke the function on
    * 
    * @return this returns the object that acts as the replacement
    * 
    * @throws Exception if the replacement function cannot complete
    */
   public Object replace(Object source) throws Exception {
      if(replace != null) {        
         return replace.call(context, source);
      }
      return source;
   }
   
   /**
    * This is used to replace the deserialized object with another
    * instance, perhaps of a different type. This is useful when an
    * XML schema class acts as a reference to another XML document
    * which needs to be loaded externally to create an object of
    * a different type.
    * 
    * @param source the source object to invoke the function on 
    * 
    * @return this returns the object that acts as the replacement
    * 
    * @throws Exception if the replacement function cannot complete
    */
   public Object resolve(Object source) throws Exception {
      if(resolve != null) {
         return resolve.call(context, source);
      }
      return source;
   }
   
   /**
    * This method is used to invoke the provided objects commit function
    * during the deserialization process. The commit function must be
    * marked with the <code>Commit</code> annotation so that when the
    * object is deserialized the persister has a chance to invoke the
    * function so that the object can build further data structures.
    * 
    * @param source this is the object that has just been deserialized
    * 
    * @throws Exception thrown if the commit process cannot complete
    */
   public void commit(Object source) throws Exception {
      if(commit != null) {
         commit.call(context, source);
      }
   }

   /**
    * This method is used to invoke the provided objects validation
    * function during the deserialization process. The validation function
    * must be marked with the <code>Validate</code> annotation so that
    * when the object is deserialized the persister has a chance to 
    * invoke that function so that object can validate its field values.
    * 
    * @param source this is the object that has just been deserialized
    * 
    * @throws Exception thrown if the validation process failed
    */
   public void validate(Object source) throws Exception {
      if(validate != null) {
         validate.call(context, source);
      }
   }
   
   /**
    * This method is used to invoke the provided objects persistence
    * function. This is invoked during the serialization process to
    * get the object a chance to perform an necessary preparation
    * before the serialization of the object proceeds. The persist
    * function must be marked with the <code>Persist</code> annotation.
    * 
    * @param source the object that is about to be serialized
    * 
    * @throws Exception thrown if the object cannot be persisted
    */
   public void persist(Object source) throws Exception {
      if(persist != null) {
         persist.call(context, source);
      }
   }
   
   /**
    * This method is used to invoke the provided objects completion
    * function. This is invoked after the serialization process has
    * completed and gives the object a chance to restore its state
    * if the persist function required some alteration or locking.
    * This is marked with the <code>Complete</code> annotation.
    * 
    * @param source this is the object that has been serialized
    * 
    * @throws Exception thrown if the object cannot complete
    */
   public void complete(Object source) throws Exception {
      if(complete != null) {
         complete.call(context, source);
      }
   }
}
