/*
 * Group.java March 2011
 *
 * Copyright (C) 2011, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>Group</code> interface represents a group of labels
 * associated with a particular union annotation. A group allows
 * a <code>Label</code> to be acquired by name or type. Acquiring
 * the label by type allows the serialization process to dynamically
 * select a label, and thus converter, based on the instance type.
 * On deserialization a label is dynamically selected based on name.
 * 
 * @author Niall Gallagher
 */
interface Group {
   
   /**
    * This is used to determine if the group is inline. A group is
    * inline if all of the elements in the group is inline. If any of
    * the <code>Label<code> objects in the group is not inline then
    * the entire group is not inline, although this is unlikely.
    * 
    * @return this returns true if each label in the group is inline
    */
   boolean isInline();
   
   /**
    * This is used to determine if an annotated list is a text 
    * list. A text list is a list of elements that also accepts
    * free text. Typically this will be an element list union that
    * will allow unstructured XML such as XHTML to be parsed.
    * 
    * @return returns true if the label represents a text list
    */
   boolean isTextList();
   
   /**
    * This is used to acquire a <code>Label</code> based on the type
    * of an object. Selecting a label based on the type ensures that
    * the serialization process can dynamically convert an object
    * to XML. If the type is not supported, this returns null.
    * 
    * @param type this is the type to select the label from
    * 
    * @return this returns the label based on the type
    */
   Label getLabel(Class type);
   
   /**
    * This is used to acquire a <code>LabelMap</code> containing the
    * labels available to the group. Providing a context object 
    * ensures that each of the labels is mapped to a name that is
    * styled according to its internal style.
    * 
    * @return this returns a label map containing the labels 
    */
   LabelMap getElements() throws Exception;
   
   /**
    * This is used to get a <code>Label</code> that represents the
    * text between elements on an element union. Providing a label
    * here ensures that the free text found between elements can
    * be converted in to strings and added to the list.
    * 
    * @return a label if a text annotation has been declared
    */
   Label getText() throws Exception;
   
   /**
    * This returns a string representation of the union group.
    * Providing a string representation in this way ensures that the
    * group can be used in exception messages and for any debugging.
    * 
    * @return this returns a string representation of the group
    */
   String toString();
}
