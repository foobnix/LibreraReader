/*
 * LabelGroup.java July 2006
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

import java.util.Arrays;
import java.util.List;

/**
 * The <code>LabelList</code> contains a group of labels associated
 * with a specific contact. Here any number of annotations can be
 * associated with a single contact. This allows for element unions
 * that may contain more than one label to be represented.
 * 
 * @author Niall Gallagher
 */
class LabelGroup {
   
   /**
    * This contains the list of labels associated with a contact.
    */
   private final List<Label> list;
   
   /**
    * This contains the number of labels this list contains.
    */
   private final int size;
   
   /**
    * Constructor for the <code>LabelList</code> object. This is 
    * used to create a group of labels that contains a single 
    * label. Typically this is used for non-union annotations.
    * 
    * @param label this is the label that this group represents
    */
   public LabelGroup(Label label) {
      this(Arrays.asList(label));
   }
   
   /**
    * Constructor for the <code>LabelList</code> object. This is 
    * used to create a group of labels that contains a multiple 
    * labels. Typically this is used for union annotations.
    * 
    * @param list this is the labels that this group contains
    */
   public LabelGroup(List<Label> list) {
      this.size = list.size();
      this.list = list;
   }
   
   /**
    * This is used to acquire all of the labels associated with
    * this group. For a non-union annotation this will contain 
    * a list containing a single label object.
    * 
    * @return this returns a list containing a single label
    */
   public List<Label> getList() {
      return list;
   }
   
   /**
    * This is used to acquire the primary label associated with
    * this group. If this group represents a non-union then this
    * will return the only label associated with the contact.
    * 
    * @return this returns the primary annotation for the group
    */
   public Label getPrimary(){
      if(size > 0) {
         return list.get(0);
      }
      return null;
   }
}