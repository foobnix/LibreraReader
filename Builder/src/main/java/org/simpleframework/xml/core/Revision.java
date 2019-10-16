/*
 * Revision.java July 2008
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

/**
 * The <code>Revision</code> object is used represent the revision
 * of a class as read from a version attribute. It determines the
 * type of deserialization that takes place. 
 * 
 * @author Niall Gallagher
 */
class Revision {
  
   /**
    * This is used to track the revision comparision of the class.
    */
   private boolean equal;
   
   /**
    * Constructor of the <code>Revision</code> object. This is used
    * to create a comparator object that will compare and cache the
    * comparison of the expected and current version of the class.
    */
   public Revision() {
      this.equal = true;
   }
   
   /**
    * This is used to acquire the default revision. The default
    * revision is the revision expected if there is not attribute
    * representing the version in the XML element for the object.
    * 
    * @return this returns the default version for the object
    */
   public double getDefault() {
      return 1.0;
   }

   /**
    * This is used to compare the expected and current versions of
    * the class. Once compared the comparison result is cached
    * within the revision class so that it can be used repeatedly.
    * 
    * @param expected this is the expected version of the class
    * @param current this is the current version of the class
    * 
    * @return this returns true if the versions are the same
    */
   public boolean compare(Object expected, Object current) {
      if(current != null) {
         equal = current.equals(expected);
      } else if(expected != null) {
         equal = expected.equals(1.0);
      }
      return equal;
   }
   
   /**
    * This returns the cached comparision of the revisions. This
    * will be true if not comparison was performed. If however one
    * was performed then this will represent the result.
    * 
    * @return this returns the cached version of the comparison
    */
   public boolean isEqual() {
      return equal;
   }
}