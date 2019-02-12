/*
 * ElementArrayLabel.java July 2006
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

import java.lang.annotation.Annotation;

import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>ElementArrayLabel</code> represents a label that is used
 * to represent an XML element array in a class schema. This element 
 * array label can be used to convert an XML node into an array of
 * composite or primitive objects. If the array is of primitive types
 * then the <code>entry</code> attribute must be specified so that 
 * the primitive values can be serialized in a structured manner.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.ElementArray
 */
class ElementArrayLabel extends TemplateLabel {
   
   /**
    * This is the decorator that is associated with the element.
    */
   private Decorator decorator;

   /**
    * This references the annotation that the field uses.
    */
   private ElementArray label;
   
   /**
    * This contains the details of the annotated contact object.
    */
   private Introspector detail;
   
   /**
    * This is the path that is used to represent this attribute.
    */
   private Expression path;
   
   /**
    * This is used to style the elements associated with this.
    */
   private Format format;
   
   /**
    * This is the type of array this label will represent.
    */
   private Class type;
   
   /**
    * This is the name of the XML entry from the annotation.
    */
   private String entry;
   
   /**
    * This is the name of the element for this label instance.
    */
   private String name;
   
   /**
    * This is used to determine if the element is required.
    */
   private boolean required;
   
   /**
    * This is used to determine if the element is data.
    */
   private boolean data;
   
   /**
    * Constructor for the <code>ElementArrayLabel</code> object. This
    * creates a label object, which can be used to convert an element
    * node to an array of XML serializable objects.
    * 
    * @param contact this is the contact that this label represents
    * @param label the annotation that contains the schema details
    * @param format this is used to style the elements for this label
    */
   public ElementArrayLabel(Contact contact, ElementArray label, Format format) {
      this.detail = new Introspector(contact, this, format);
      this.decorator = new Qualifier(contact);
      this.required = label.required();
      this.type = contact.getType();
      this.entry = label.entry();
      this.data = label.data();
      this.name = label.name();
      this.format = format;
      this.label = label;
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
      return decorator;
   }

   /**
    * This will create a <code>Converter</code> for transforming an XML
    * element into an array of XML serializable objects. The XML schema
    * class for these objects must present the element array annotation. 
    * 
    * @param context this is the context object used for serialization
    * 
    * @return this returns the converter for creating a collection 
    */
   public Converter getConverter(Context context) throws Exception {
      Contact contact = getContact();
      String entry = getEntry();
      
      if(!type.isArray()) {
         throw new InstantiationException("Type is not an array %s for %s", type, contact);
      }
      return getConverter(context, entry);
   }  
      
   /**
    * This will create a <code>Converter</code> for transforming an XML
    * element into an array of XML serializable objects. The XML schema
    * class for these objects must present the element array annotation. 
    * 
    * @param context this is the context object used for serialization
    * @param name this is the name of the entry XML element to use
    * 
    * @return this returns the converter for creating a collection 
    */      
   private Converter getConverter(Context context, String name) throws Exception {
      Type entry = getDependent();
      Type type = getContact();
      
      if(!context.isPrimitive(entry)) { 
         return new CompositeArray(context, type, entry, name);        
      }
      return new PrimitiveArray(context, type, entry, name);            
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
   public Object getEmpty(Context context) throws Exception {
      Type array = new ClassType(type);
      Factory factory = new ArrayFactory(context, array);
      
      if(!label.empty()) {
         return factory.getInstance();
      }
      return null;
   }
   
   /**
    * This is used to either provide the entry value provided within
    * the annotation or compute a entry value. If the entry string
    * is not provided the the entry value is calculated as the type
    * of primitive the object is as a simplified class name.
    * 
    * @return this returns the name of the XML entry element used 
    */
   public String getEntry() throws Exception {      
      Style style = format.getStyle();
      
      if(detail.isEmpty(entry)) {
         entry = detail.getEntry();
      }
      return style.getElement(entry);
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
   public String getName() throws Exception{
      Style style = format.getStyle();
      String name = detail.getName();
     
      return style.getElement(name);
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
      Expression path = getExpression();
      String name = getName();
   
      return path.getElement(name);   
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
      if(path == null) {
         path = detail.getExpression();
      }
      return path;
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
      return label;
   }
   
   /**
    * This is used to acquire the dependent type for the annotated
    * array. This will simply return the type that the array is
    * composed to hold. This must be a serializable type, that is,
    * a type that is annotated with the <code>Root</code> class.
    * 
    * @return this returns the component type for the array
    */
   public Type getDependent() {
      Class entry = type.getComponentType();
      
      if(entry == null) {
         return new ClassType(type);
      }
      return new ClassType(entry);
   }
   
   /**
    * This acts as a convenience method used to determine the type of
    * contact this represents. This is used when an object is written
    * to XML. It determines whether a <code>class</code> attribute
    * is required within the serialized XML element, that is, if the
    * class returned by this is different from the actual value of the
    * object to be serialized then that type needs to be remembered.
    *  
    * @return this returns the type of the contact class
    */
   public Class getType() {
      return type;      
   }
   
   /**
    * This is used to acquire the contact object for this label. The 
    * contact retrieved can be used to set any object or primitive that
    * has been deserialized, and can also be used to acquire values to
    * be serialized in the case of object persistence. All contacts
    * that are retrieved from this method will be accessible. 
    * 
    * @return returns the contact that this label is representing
    */   
   public Contact getContact() {
      return detail.getContact();
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
      return name;
   }
   
   /**
    * This is used to determine whether the XML element is required. 
    * This ensures that if an XML element is missing from a document
    * that deserialization can continue. Also, in the process of
    * serialization, if a value is null it does not need to be 
    * written to the resulting XML document.
    * 
    * @return true if the label represents a some required data
    */  
   public boolean isRequired() {
      return required;
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
      return data;
   }
   
   /**
    * This is used to describe the annotation and method or field
    * that this label represents. This is used to provide error
    * messages that can be used to debug issues that occur when
    * processing a method. This will provide enough information
    * such that the problem can be isolated correctly. 
    * 
    * @return this returns a string representation of the label
    */
   public String toString() {
      return detail.toString();
   }   
}
