/*
 * TextListLabel.java December 2012
 *
 * Copyright (C) 2012, Niall Gallagher <niallg@users.sf.net>
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

import java.lang.annotation.Annotation;

import org.simpleframework.xml.Text;
import org.simpleframework.xml.strategy.Type;

/**
 * The <code>TextListLabel</code> object is used to create a label
 * that will create a converter used to read free text. Free text is
 * text that exists between elements. An <code>ElementListUnion</code>
 * can be declared to consume text that exists between the individual
 * elements by declaring a <code>Text</code> annotation with it. In 
 * such a declaration unstructured text can be processed.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.GroupExtractor
 */
class TextListLabel extends TemplateLabel {
   
   /**
    * This is the empty value that is declared for the annotation.
    */
   private final String empty;
   
   /**
    * This is the element list union label that is declared with this.
    */
   private final Label label;
   
   /**
    * This is the text annotation declaration on the element list.
    */
   private final Text text;
   
   /**
    * Constructor for the <code>TextListLabel</code> object. This is
    * used to create a label used for reading free text declared 
    * between the elements of an element list union. Such a label
    * enables serialization and deserialization of unstructured XML.
    * 
    * @param label this is the label that is declared with this
    * @param text this is the text annotation delcaration
    */
   public TextListLabel(Label label, Text text) {
      this.empty = text.empty();
      this.label = label;
      this.text = text;
   }

   /**
    * This is used to acquire the <code>Decorator</code> for this.
    * A decorator is an object that adds various details to the
    * node without changing the overall structure of the node. For
    * example comments and namespaces can be added to the node with
    * a decorator as they do not affect the deserialization.
    * 
    * @return this returns the decorator associated with this
    */
   public Decorator getDecorator() throws Exception {
      return null;
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
      return label.getNames();
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
      return label.getPaths();
   }

   /**
    * This is used to provide a configured empty value used when the
    * annotated value is null. This ensures that XML can be created
    * with required details regardless of whether values are null or
    * not. It also provides a means for sensible default values.
    * 
    * @param context this is the context object for the serialization
    * 
    * @return this returns the string to use for default values
    */
   public String getEmpty(Context context) throws Exception {
      return empty;
   }
   
   /**
    * This method returns a <code>Converter</code> which can be used to
    * convert an XML node into an object value and vice versa. The 
    * converter requires only the context object in order to perform
    * serialization or deserialization of the provided XML node.
    * 
    * @param context this is the context object for the serialization
    * 
    * @return this returns an object that is used for conversion
    */
   public Converter getConverter(Context context) throws Exception {
      Type type = getContact();
      
      if(!label.isCollection()) {
         throw new TextException("Cannot use %s to represent %s", type, label);
      }
      return new TextList(context, type, label);
   }

   /**
    * This is used to acquire the name of the element or attribute
    * that is used by the class schema. The name is determined by
    * checking for an override within the annotation. If it contains
    * a name then that is used, if however the annotation does not
    * specify a name the the field or method name is used instead.
    * 
    * @return returns the name that is used for the XML property
    */
   public String getName() throws Exception {
      return label.getName();
   }

   /**
    * This is used to acquire the path of the element or attribute
    * that is used by the class schema. The path is determined by
    * acquiring the XPath expression and appending the name of the
    * label to form a fully qualified path.
    * 
    * @return returns the path that is used for the XML property
    */
   public String getPath() throws Exception {
      return label.getPath();
   }

   /**
    * This method is used to return an XPath expression that is 
    * used to represent the position of this label. If there is no
    * XPath expression associated with this then an empty path is
    * returned. This will never return a null expression.
    * 
    * @return the XPath expression identifying the location
    */
   public Expression getExpression() throws Exception {
      return label.getExpression();
   }

   /**
    * This is used to acquire the dependent class for this label. 
    * This returns null as there are no dependents to the element
    * annotation as it can only hold primitives with no dependents.
    * 
    * @return this is used to return the dependent type of null
    */
   public Type getDependent() throws Exception {
      return label.getDependent();
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
      return label.getEntry();
   }

   /**
    * This is the key used to represent this label. The key is used
    * to store the parameter in hash containers. Typically the
    * key is generated from the paths associated with the label.
    * 
    * @return this is the key used to represent the label
    */
   public Object getKey() throws Exception {
      return label.getKey();
   }

   /**
    * This acquires the annotation associated with this label. This
    * is typically the annotation acquired from the field or method.
    * However, in the case of unions this will return the actual
    * annotation within the union group that this represents.
    * 
    * @return this returns the annotation that this represents
    */
   public Annotation getAnnotation() {
      return label.getAnnotation();
   }

   /**
    * This is used to acquire the contact object for this label. The 
    * contact retrieved can be used to set any object or primitive that
    * has been deserialized, and can also be used to acquire values to
    * be serialized in the case of object persistence. All contacts 
    * that are retrieved from this method will be accessible. 
    * 
    * @return returns the field that this label is representing
    */
   public Contact getContact() {
      return label.getContact();
   }

   /**
    * This acts as a convenience method used to determine the type of
    * the field this represents. This is used when an object is written
    * to XML. It determines whether a <code>class</code> attribute
    * is required within the serialized XML element, that is, if the
    * class returned by this is different from the actual value of the
    * object to be serialized then that type needs to be remembered.
    *  
    * @return this returns the type of the field class
    */
   public Class getType() {
      return label.getType();
   }

   /**
    * This is used to acquire the name of the element or attribute
    * as taken from the annotation. If the element or attribute
    * explicitly specifies a name then that name is used for the
    * XML element or attribute used. If however no overriding name
    * is provided then the method or field is used for the name. 
    * 
    * @return returns the name of the annotation for the contact
    */
   public String getOverride() {
      return label.getOverride();
   }

   /**
    * This is used to determine whether the annotation requires it
    * and its children to be written as a CDATA block. This is done
    * when a primitive or other such element requires a text value
    * and that value needs to be encapsulated within a CDATA block.
    * 
    * @return this returns true if the element requires CDATA
    */
   public boolean isData() {
      return label.isData();
   }

   /**
    * Determines whether the XML attribute or element is required. 
    * This ensures that if an XML element is missing from a document
    * that deserialization can continue. Also, in the process of
    * serialization, if a value is null it does not need to be 
    * written to the resulting XML document.
    * 
    * @return true if the label represents a some required data
    */
   public boolean isRequired() {
      return label.isRequired();
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
      return true;
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
      return label.isInline();
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
      return true;
   }
   
   /**
    * This is used to create a string used to represent this text
    * list label. This will basically create a string representing
    * the other label in addition to the <code>Text</code> label.
    * 
    * @return this is used to build a string for the list label 
    */
   public String toString() {
      return String.format("%s %s", text, label);
   }
}
