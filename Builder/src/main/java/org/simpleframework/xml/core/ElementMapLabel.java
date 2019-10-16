/*
 * ElementMapLabel.java July 2007
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

import java.lang.annotation.Annotation;

import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>ElementMapLabel</code> represents a label that is used
 * to represent an XML element map in a class schema. This element 
 * list label can be used to convert an XML node into a map object 
 * of composite or primitive key value pairs. Each element converted 
 * with the converter this creates must be an XML serializable element.
 * 
 * @author Niall Gallagher
 * 
 *  @see org.simpleframework.xml.ElementMap
 */
class ElementMapLabel extends TemplateLabel {
   
   /**
    * This is the decorator that is associated with the element.
    */
   private Decorator decorator;

   /**
    * This contains the details of the annotated contact object.
    */
   private Introspector detail;
   
   /**
    * This references the annotation that the field uses.
    */
   private ElementMap label;
   
   /**
    * This is a cache of the expression for this element map.
    */
   private Expression cache;
   
   /**
    * This is the format used to style the elements for this.
    */
   private Format format;
   
   /**
    * The entry object contains the details on how to write the map.
    */
   private Entry entry;
   
   /**
    * This is the name of the element for this label instance.
    */
   private String override;  
   
   /**
    * This is the name of the XML entry from the annotation.
    */
   private String parent;
   
   /**
    * This is the path of the element for this label instance.
    */
   private String path;  
   
   /**
    * This is the name of the element for this label instance.
    */
   private String name;  
   
   /**
    * Represents the type of objects this map object will hold.
    */
   private Class[] items;
   
   /**
    * This is the type of map object this list will instantiate.
    */
   private Class type;
   
   /**
    * This is used to determine if the attribute is required.
    */
   private boolean required;
   
   /**
    * This is used to determine if the attribute is inline.
    */
   private boolean inline;
   
   /**
    * This is used to determine if the attribute is data.
    */
   private boolean data;
   
   /**
    * Constructor for the <code>ElementMapLabel</code> object. This
    * creates a label object, which can be used to convert an XML 
    * node to a <code>Map</code> of XML serializable objects.
    * 
    * @param contact this is the contact that this label represents
    * @param label the annotation that contains the schema details
    * @param format this is the format used to style this label
    */
   public ElementMapLabel(Contact contact, ElementMap label, Format format) {
      this.detail = new Introspector(contact, this, format);
      this.decorator = new Qualifier(contact);
      this.entry = new Entry(contact, label);
      this.required = label.required();
      this.type = contact.getType();
      this.inline = label.inline();
      this.override = label.name();      
      this.data = label.data();
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
      Type type = getMap();
      
      if(!label.inline()) {
         return new CompositeMap(context, entry, type);
      }
      return new CompositeInlineMap(context, entry, type);     
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
      Type map = new ClassType(type);
      Factory factory = new MapFactory(context, map);
      
      if(!label.empty()) {
         return factory.getInstance();
      }
      return null;
   }
   
   /**
    * This is used to acquire the dependent type for the annotated
    * list. This will simply return the type that the map object is
    * composed to hold. This must be a serializable type, that is,
    * a type that is annotated with the <code>Root</code> class.
    * 
    * @return this returns the component type for the map object
    */
   public Type getDependent() throws Exception  {
      Contact contact = getContact();
     
      if(items == null) {
         items = contact.getDependents();
      }        
      if(items == null) {
         throw new ElementException("Unable to determine type for %s", contact);           
      }    
      if(items.length == 0) {
         return new ClassType(Object.class);
      }
      return new ClassType(items[0]);
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
      
      if(detail.isEmpty(parent)) {
         parent = detail.getEntry();
      }
      return style.getElement(parent);
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
      if(name == null) {
         Style style = format.getStyle();
         String value = entry.getEntry();
         
         if(!label.inline()) {
            value = detail.getName();
         }
         name = style.getElement(value);
      }
      return name;
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
      if(path == null) {
         Expression expression = getExpression();
         String name = getName();
         
         path = expression.getElement(name);  
      }
      return path;
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
      if(cache == null) {
         cache = detail.getExpression();
      }
      return cache;
   }
   
   /**
    * This returns the map type for this label. The primary type
    * is the type of the <code>Map</code> that this creates. The key
    * and value types are the types used to populate the primary.
    * 
    * @return this returns the map type to use for the label
    */
   private Type getMap() {
      return new ClassType(type);
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
      return override;
   }
   
   /**
    * This is used to determine whether the annotation requires it
    * and its children to be written as a CDATA block. This is done
    * when a primitive or other such element requires a text value
    * and that value needs to be encapsulated within a CDATA block.
    * 
    * @return currently the element list does not require CDATA
    */
   public boolean isData() {
      return data;
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
    * This is used to determine whether the list has been specified
    * as inline. If the list is inline then no overrides are needed
    * and the outer XML element for the list is not used.
    * 
    * @return this returns whether the annotation is inline
    */
   public boolean isInline() {
      return inline;
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
