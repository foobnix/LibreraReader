/*
 * TemplateLabel.java July 2009
 *
 * Copyright (C) 2009, Niall Gallagher <niallg@users.sf.net>
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

import org.simpleframework.xml.strategy.Type;

/**
 * The <code>TemplateLabel</code> object is used to provide stock
 * functions that can be used by all implementations. This ensures
 * there is a consistent set of behaviours for each label. It also
 * reduces the number of methods that need to be maintained for
 * each <void>Label</code> implementation.
 * 
 * @author Niall Gallagher
 */
abstract class TemplateLabel implements Label {
   
   /**
    * This is the builder that is used to generate label keys.
    */
   private final KeyBuilder builder;
   
   /**
    * Constructor for the <code>TemplateLabel</code> is used to
    * create a template for other labels. If any of the method
    * implementations are not as required or they should be
    * overridden by the subclass.
    */
   protected TemplateLabel() {
      this.builder = new KeyBuilder(this);
   }
   
   /**
    * This is used to acquire the <code>Type</code> that the type
    * provided is represented by. Typically this will return the
    * field or method represented by the label. However, in the 
    * case of unions this will provide an override type.
    * 
    * @param type this is the class to acquire the type for
    * 
    * @return this returns the type represented by this class
    */
   public Type getType(Class type) throws Exception {
      return getContact();
   }
   
   /**
    * This is used to acquire the <code>Label</code> that the type
    * provided is represented by. Typically this will return the
    * same instance. However, in the case of unions this will
    * look for an individual label to match the type provided.
    * 
    * @param type this is the type to acquire the label for
    * 
    * @return this returns the label represented by this type
    */
   public Label getLabel(Class type) throws Exception {
      return this;
   }
   
   /**
    * This returns a <code>Collection</code> of element names. This
    * will typically contain both the name and path of the label. 
    * However, if this is a union it can contain many names and
    * paths. This method should never return null. 
    * 
    * @return this returns the names of each of the elements
    */
   public String[] getNames() throws Exception {
      String path = getPath();
      String name = getName();
      
      return new String[] {path, name};
   }
   
   /**
    * This returns a <code>Collection</code> of element paths. This
    * will typically contain only the path of the label, which is
    * composed using the <code>Path</code> annotation and the name
    * of the label. However, if this is a union it can contain many 
    * paths. This method should never return null.
    * 
    * @return this returns the names of each of the elements
    */
   public String[] getPaths() throws Exception {
      String path = getPath();
      
      return new String[] {path};
   }
   
   /**
    * This is the key used to represent this label. The key is used
    * to store the parameter in hash containers. Typically the
    * key is generated from the paths associated with the label.
    * 
    * @return this is the key used to represent the label
    */
   public Object getKey() throws Exception {
      return builder.getKey();
   }
   
   /**
    * This is typically used to acquire the entry value as acquired
    * from the annotation. However given that the annotation this
    * represents does not have a entry attribute this will always
    * provide a null value for the entry string.
    * 
    * @return this will always return null for the entry value 
    */
   public String getEntry() throws Exception {
      return null;
   }
   
   /**
    * This is used to acquire the dependent class for this label. 
    * This returns null as there are no dependents to the element
    * annotation as it can only hold primitives with no dependents.
    * 
    * @return this is used to return the dependent type of null
    */
   public Type getDependent() throws Exception {
      return null;
   }
   
   /**
    * This method is used to determine if the label represents an
    * attribute. This is used to style the name so that elements
    * are styled as elements and attributes are styled as required.
    * 
    * @return this is used to determine if this is an attribute
    */
   public boolean isAttribute() {
      return false;
   }
   
   /**
    * This is used to determine if the label is a collection. If the
    * label represents a collection then any original assignment to
    * the field or method can be written to without the need to 
    * create a new collection. This allows obscure collections to be
    * used and also allows initial entries to be maintained.
    * 
    * @return true if the label represents a collection value
    */
   public boolean isCollection() {
      return false;
   }
   
   /**
    * This is used to determine whether the label represents an
    * inline XML entity. The <code>ElementList</code> annotation
    * and the <code>Text</code> annotation represent inline 
    * items. This means that they contain no containing element
    * and so can not specify overrides or special attributes.
    * 
    * @return this returns true if the annotation is inline
    */
   public boolean isInline() {
      return false;
   }
   
   /**
    * This is used to determine if the label represents text. If
    * a label represents text it typically does not have a name,
    * instead the empty string represents the name. Also text
    * labels can not exist with other text labels, or elements.
    * 
    * @return this returns true if this label represents text
    */
   public boolean isText() {
      return false;
   }
   
   /**
    * This is used to determine if an annotated list is a text 
    * list. A text list is a list of elements that also accepts
    * free text. Typically this will be an element list union that
    * will allow unstructured XML such as XHTML to be parsed.
    * 
    * @return returns true if the label represents a text list
    */
   public boolean isTextList() {
      return false;
   }
   
   /**
    * This is used to determine if this label is a union. If this
    * is true then this label represents a number of labels and
    * is simply a wrapper for these labels. 
    * 
    * @return this returns true if the label represents a union
    */
   public boolean isUnion() {
      return false;
   }
}
