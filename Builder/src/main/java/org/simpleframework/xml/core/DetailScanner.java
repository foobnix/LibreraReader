/*
 * DetailScanner.java July 2012
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Order;
import org.simpleframework.xml.Root;

/**
 * The <code>DetailScanner</code> is used to scan a class for methods
 * and fields as well as annotations. Scanning a type in this way 
 * ensures that all its details can be extracted and cached in one
 * place. This greatly improves performance on platforms that do not
 * cache reflection well, like Android. 
 * 
 * @author Niall Gallagher
 */
class DetailScanner implements Detail {
   
   /**
    * This contains a list of methods that are extracted for this.
    */
   private List<MethodDetail> methods;
   
   /**
    * This contains a list of fields that are extracted for this.
    */
   private List<FieldDetail> fields;
   
   /**
    * This represents the namespace list declared on the type.
    */
   private NamespaceList declaration;
   
   /**
    * This represents the namespace annotation declared on the type.
    */
   private Namespace namespace;
   
   /**
    * This represents all the annotations declared for the type.
    */
   private Annotation[] labels;
   
   /**
    * This represents the access type override declared or the type.
    */
   private DefaultType override;
   
   /**
    * This represents the default access type declared or the type.
    */
   private DefaultType access;
   
   /**
    * This is the order annotation that is declared for the type.
    */
   private Order order;
   
   /**
    * This is the root annotation that is declared for the type.
    */
   private Root root;
   
   /**
    * This is the type that is represented by this instance.
    */
   private Class type;
   
   /**
    * This represents the name of the type used for XML elements.
    */
   private String name;
   
   /**
    * This is used to determine if the default type is required.
    */
   private boolean required;
   
   /**
    * This is used to determine if strict XML parsing is done.
    */
   private boolean strict;
   
   /**
    * Constructor for the <code>DetailScanner</code> object. This is
    * used to create a detail object from a type. All of the methods
    * fields and annotations are extracted so that they can be used
    * many times over without the need to process them again.
    * 
    * @param type this is the type to scan for various details
    */
   public DetailScanner(Class type) {
      this(type, null);
   }
   
   /**
    * Constructor for the <code>DetailScanner</code> object. This is
    * used to create a detail object from a type. All of the methods
    * fields and annotations are extracted so that they can be used
    * many times over without the need to process them again.
    * 
    * @param type this is the type to scan for various details
    * @param override this is the override used for this detail
    */
   public DetailScanner(Class type, DefaultType override) {
      this.methods = new LinkedList<MethodDetail>();
      this.fields = new LinkedList<FieldDetail>();
      this.labels = type.getDeclaredAnnotations();
      this.override = override;
      this.strict = true;
      this.type = type;
      this.scan(type);
   }

   /**
    * This is used to determine if the generated annotations are
    * required or not. By default generated parameters are required.
    * Setting this to false means that null values are accepted
    * by all defaulted fields or methods depending on the type.
    * 
    * @return this is used to determine if defaults are required
    */
   public boolean isRequired() {
      return required;
   }
     
   /**
    * This method is used to determine whether strict mappings are
    * required. Strict mapping means that all labels in the class
    * schema must match the XML elements and attributes in the
    * source XML document. When strict mapping is disabled, then
    * XML elements and attributes that do not exist in the schema
    * class will be ignored without breaking the parser.
    *
    * @return true if strict parsing is enabled, false otherwise
    */ 
   public boolean isStrict() {
      return strict;
   }
   
   /**
    * This is used to determine whether this detail represents a
    * primitive type. A primitive type is any type that does not
    * extend <code>Object</code>, examples are int, long and double.
    * 
    * @return this returns true if no XML annotations were found
    */
   public boolean isPrimitive() {
      return type.isPrimitive();
   }
   
   /**
    * This is used to determine if the class is an inner class. If
    * the class is a inner class and not static then this returns
    * false. Only static inner classes can be instantiated using
    * reflection as they do not require a "this" argument.
    * 
    * @return this returns true if the class is a static inner
    */
   public boolean isInstantiable() {
      int modifiers = type.getModifiers();
       
      if(Modifier.isStatic(modifiers)) {
         return true;
      }
      return !type.isMemberClass();       
   }
   
   /**
    * This returns the <code>Root</code> annotation for the class.
    * The root determines the type of deserialization that is to
    * be performed and also contains the name of the root element. 
    * 
    * @return this returns the name of the object being scanned
    */
   public Root getRoot() {
      return root;
   }
   
   /**
    * This returns the name of the class processed by this scanner.
    * The name is either the name as specified in the last found
    * <code>Root</code> annotation, or if a name was not specified
    * within the discovered root then the Java Bean class name of
    * the last class annotated with a root annotation.
    * 
    * @return this returns the name of the object being scanned
    */
   public String getName() {
      return name;
   }
   
   /**
    * This returns the type represented by this detail. The type is
    * the class that has been scanned for annotations, methods and
    * fields. All super types of this are represented in the detail.
    * 
    * @return the type that this detail object represents
    */
   public Class getType() {
      return type;
   }
   
   /**
    * This returns the order annotation used to determine the order
    * of serialization of attributes and elements. The order is a
    * class level annotation that can be used only once per class
    * XML schema. If none exists then this will return null.
    *  of the class processed by this scanner.
    * 
    * @return this returns the name of the object being scanned
    */
   public Order getOrder() {
      return order;
   }
   
   /**
    * This returns the <code>DefaultType</code> override used for this
    * detail. An override is used only when the class contains no
    * annotations and does not have a <code>Transform</code> of any 
    * type associated with it. It allows serialization of external
    * objects without the need to annotate the types.
    * 
    * @return this returns the  access type override for this type
    */
   public DefaultType getOverride() {
      return override;
   }
   
   /**
    * This returns the <code>Default</code> annotation access type
    * that has been specified by this. If no default annotation has
    * been declared on the type then this will return null.
    * 
    * @return this returns the default access type for this type
    */
   public DefaultType getAccess() {
      if(override != null) {
         return override;
      }
      return access;
   }
   
   /**
    * This returns the <code>Namespace</code> annotation that was
    * declared on the type. If no annotation has been declared on the
    * type this will return null as not belonging to any.
    * 
    * @return this returns the namespace this type belongs to, if any
    */
   public Namespace getNamespace() {
      return namespace;
   }
   
   /**
    * This returns the <code>NamespaceList</code> annotation that was
    * declared on the type. A list of namespaces are used to simply 
    * declare the namespaces without specifically making the type
    * belong to any of the declared namespaces.
    * 
    * @return this returns the namespace declarations, if any
    */
   public NamespaceList getNamespaceList() {
      return declaration;
   }
   
   /**
    * This returns a list of the methods that belong to this type. 
    * The methods here do not include any methods from the super
    * types and simply provides a means of caching method data.
    * 
    * @return returns the list of methods declared for the type
    */
   public List<MethodDetail> getMethods() {
      return methods;
   }
   
   /**
    * This returns a list of the fields that belong to this type. 
    * The fields here do not include any fields from the super
    * types and simply provides a means of caching method data.
    * 
    * @return returns the list of fields declared for the type
    */
   public List<FieldDetail> getFields() {
      return fields;
   }
   
   /**
    * This returns the annotations that have been declared for this
    * type. It is preferable to acquire the declared annotations
    * from this method as they are cached. Older versions of some
    * runtime environments, particularly Android, are slow at this.
    * 
    * @return this returns the annotations associated with this
    */
   public Annotation[] getAnnotations() {
      return labels;
   }

   /**
    * This returns the constructors that have been declared for this
    * type. It is preferable to acquire the declared constructors
    * from this method as they are cached. Older versions of some
    * runtime environments, particularly Android, are slow at this.
    * 
    * @return this returns the constructors associated with this
    */
   public Constructor[] getConstructors() {
      return type.getDeclaredConstructors();
   }
   
   /**
    * This is used to acquire the super type for the class that is
    * represented by this detail. If the super type for the class
    * is <code>Object</code> then this will return null.
    * 
    * @return returns the super type for this class or null
    */
   public Class getSuper() {
      Class base = type.getSuperclass();
      
      if(base == Object.class) {
         return null;
      }
      return base;
   }
   
   /**
    * This method is used to scan the type for all of its annotations
    * as well as its methods and fields. Everything that is scanned 
    * is cached within the instance to ensure that it can be reused
    * when ever an object of this type is to be scanned.
    * 
    * @param type this is the type to scan for details
    */
   private void scan(Class type) {
      methods(type);
      fields(type);
      extract(type);
   }
   
   /**
    * This method is used to extract the annotations associated with
    * the type. Annotations extracted include the <code>Root</code> 
    * annotation and the <code>Namespace</code> annotation as well as
    * other annotations that are used to describe the type.
    * 
    * @param type this is the type to extract the annotations from
    */
   private void extract(Class type) {
      for(Annotation label : labels) {
         if(label instanceof Namespace) {
            namespace(label);
         }
         if(label instanceof NamespaceList) {
            scope(label);
         }
         if(label instanceof Root) {
            root(label);
         }
         if(label instanceof Order) {
            order(label);
         }
         if(label instanceof Default) {
            access(label);
         }
      }
   }
   
   /**
    * This is used to scan the type for its declared methods. Scanning
    * of the methods in this way allows the detail to prepare a cache
    * that can be used to acquire the methods and the associated
    * annotations. This improves performance on some platforms.
    * 
    * @param type this is the type to scan for declared annotations
    */
   private void methods(Class type) {
      Method[] list = type.getDeclaredMethods();
      
      for(Method method : list) {
         MethodDetail detail = new MethodDetail(method);
         methods.add(detail);
      }
   }
   
   /**
    * This is used to scan the type for its declared fields. Scanning
    * of the fields in this way allows the detail to prepare a cache
    * that can be used to acquire the fields and the associated
    * annotations. This improves performance on some platforms.
    * 
    * @param type this is the type to scan for declared annotations
    */
   private void fields(Class type) {
      Field[] list = type.getDeclaredFields();
      
      for(Field field : list) {
         FieldDetail detail = new FieldDetail(field);
         fields.add(detail);
      }
   }

   /**
    * This is used to set the optional <code>Root</code> annotation for
    * the class. The root can only be set once, so if a super type also
    * has a root annotation define it must be ignored. 
    *
    * @param label this is the label used to define the root
    */    
   private void root(Annotation label) {
      if(label != null) {
         Root value = (Root)label;
         String real = type.getSimpleName();
         String text = real;

         if(value != null) {
            text = value.name();

            if(isEmpty(text)) {
               text = Reflector.getName(real);
            }      
            strict = value.strict();
            root = value;
            name = text;  
         }
      }
   }
   
   /**
    * This method is used to determine if a root annotation value is
    * an empty value. Rather than determining if a string is empty
    * be comparing it to an empty string this method allows for the
    * value an empty string represents to be changed in future.
    * 
    * @param value this is the value to determine if it is empty
    * 
    * @return true if the string value specified is an empty value
    */
   private boolean isEmpty(String value) {
      return value.length() == 0;
   }
   
   /**
    * This is used to set the optional <code>Order</code> annotation for
    * the class. The order can only be set once, so if a super type also
    * has a order annotation define it must be ignored. 
    * 
    * @param label this is the label used to define the order
    */
   private void order(Annotation label) {
      if(label != null) {
         order = (Order)label;
      }
   }
   
   /**
    * This is used to set the optional <code>Default</code> annotation for
    * the class. The default can only be set once, so if a super type also
    * has a default annotation define it must be ignored. 
    * 
    * @param label this is the label used to define the defaults
    */
   private void access(Annotation label) {
      if(label != null) {
         Default value = (Default)label;
         
         required = value.required();
         access = value.value();
      }
   }
   
   /**
    * This is use to scan for <code>Namespace</code> annotations on
    * the class. Once a namespace has been located then it is used to
    * populate the internal namespace decorator. This can then be used
    * to decorate any output node that requires it.
    * 
    * @param label the XML annotation to scan for the namespace
    */
   private void namespace(Annotation label) {
      if(label != null) {
         namespace = (Namespace)label;
      }
   }
   
   /**
    * This is use to scan for <code>NamespaceList</code> annotations 
    * on the class. Once a namespace list has been located then it is 
    * used to populate the internal namespace decorator. This can then 
    * be used to decorate any output node that requires it.
    * 
    * @param label the XML annotation to scan for namespace lists
    */
   private void scope(Annotation label) {
      if(label != null) {
         declaration = (NamespaceList)label;
      }
   }
   
   /**
    * This is used to return a string representation of the detail. 
    * The string returned from this is the same that is returned
    * from the <code>toString</code> of the type represented. 
    * 
    * @return this returns the string representation of the type
    */
   public String toString() {
      return type.toString();
   }
}
