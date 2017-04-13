/*
 * FieldScanner.java April 2007
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

import static org.simpleframework.xml.DefaultType.FIELD;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.ElementMapUnion;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.Version;

/**
 * The <code>FieldScanner</code> object is used to scan an class for
 * fields marked with an XML annotation. All fields that contain an
 * XML annotation are added as <code>Contact</code> objects to the
 * list of contacts for the class. This scans the object by checking
 * the class hierarchy, this allows a subclass to override a super
 * class annotated field, although this should be used rarely.
 * 
 * @author Niall Gallagher
 */
class FieldScanner extends ContactList {
   
   /**
    * This is used to create the synthetic annotations for fields.
    */
   private final AnnotationFactory factory;
   
   /**
    * This is used to determine which fields have been scanned.
    */
   private final ContactMap done;
   
   /**
    * This object contains various support functions for the class.
    */
   private final Support support;
   
   /**
    * Constructor for the <code>FieldScanner</code> object. This is
    * used to perform a scan on the specified class in order to find
    * all fields that are labeled with an XML annotation.
    * 
    * @param detail this contains the details for the class scanned
    * @param support this contains various support functions
    */
   public FieldScanner(Detail detail, Support support) throws Exception {
      this.factory = new AnnotationFactory(detail, support);
      this.done = new ContactMap();
      this.support = support;
      this.scan(detail);
   }
   
   /**
    * This method is used to scan the class hierarchy for each class
    * in order to extract fields that contain XML annotations. If
    * the field is annotated it is converted to a contact so that
    * it can be used during serialization and deserialization.
    * 
    * @param detail this contains the details for the class scanned
    */
   private void scan(Detail detail) throws Exception {
      DefaultType override = detail.getOverride();
      DefaultType access = detail.getAccess();
      Class base = detail.getSuper();
      
      if(base != null) {
         extend(base, override);
      }
      extract(detail, access);
      extract(detail);
      build();
   }
   
   /**
    * This method is used to extend the provided class. Extending a
    * class in this way basically means that the fields that have
    * been scanned in the specific class will be added to this. Doing
    * this improves the performance of classes within a hierarchy.
    * 
    * @param base the class to inherit scanned fields from
    * @param access this is the access type used for the super type
    */
   private void extend(Class base, DefaultType access) throws Exception {
      ContactList list = support.getFields(base, access);
      
      if(list != null) {
         addAll(list);
      }
   }
   
   /**
    * This is used to scan the declared fields within the specified
    * class. Each method will be check to determine if it contains
    * an XML element and can be used as a <code>Contact</code> for
    * an entity within the object.
    * 
    * @param detail this is one of the super classes for the object
    */  
   private void extract(Detail detail) {
      List<FieldDetail> fields = detail.getFields();
      
      for(FieldDetail entry : fields) {
         Annotation[] list = entry.getAnnotations();
         Field field = entry.getField();
         
         for(Annotation label : list) {
            scan(field, label, list);                  
         }
      }   
   }
   
   /**
    * This is used to scan all the fields of the class in order to
    * determine if it should have a default annotation. If the field
    * should have a default XML annotation then it is added to the
    * list of contacts to be used to form the class schema.
    * 
    * @param detail this is the detail to have its fields scanned
    * @param access this is the default access type for the class
    */
   private void extract(Detail detail, DefaultType access) throws Exception {
      List<FieldDetail> fields = detail.getFields();
      
      if(access == FIELD) {
         for(FieldDetail entry : fields) {
            Annotation[] list = entry.getAnnotations();
            Field field = entry.getField();
            Class real = field.getType();
            
            if(!isStatic(field) && !isTransient(field)) {
               process(field, real, list);
            }
         }   
      }
   }
   
   /**
    * This reflectively checks the annotation to determine the type 
    * of annotation it represents. If it represents an XML schema
    * annotation it is used to create a <code>Contact</code> which 
    * can be used to represent the field within the source object.
    * 
    * @param field the field that the annotation comes from
    * @param label the annotation used to model the XML schema
    * @param list this is the list of annotations on the field
    */
   private void scan(Field field, Annotation label, Annotation[] list) {
      if(label instanceof Attribute) {
         process(field, label, list);
      }
      if(label instanceof ElementUnion) {
         process(field, label, list);
      }
      if(label instanceof ElementListUnion) {
         process(field, label, list);
      }
      if(label instanceof ElementMapUnion) {
         process(field, label, list);
      }
      if(label instanceof ElementList) {
         process(field, label, list);
      }     
      if(label instanceof ElementArray) {
         process(field, label, list);
      }
      if(label instanceof ElementMap) {
         process(field, label, list);
      }
      if(label instanceof Element) {
         process(field, label, list);
      }       
      if(label instanceof Version) {
         process(field, label, list);
      }
      if(label instanceof Text) {
         process(field, label, list);
      }
      if(label instanceof Transient) {
         remove(field, label);
      }
   }
   
   /**
    * This method is used to process the field an annotation given.
    * This will check to determine if the field is accessible, if it
    * is not accessible then it is made accessible so that private
    * member fields can be used during the serialization process.
    * 
    * @param field this is the field to be added as a contact
    * @param type this is the type to acquire the annotation
    * @param list this is the list of annotations on the field
    */
   private void process(Field field, Class type, Annotation[] list) throws Exception {
      Class[] dependents = Reflector.getDependents(field);
      Annotation label = factory.getInstance(type, dependents);
      
      if(label != null) {
         process(field, label, list);
      }
   }
   
   /**
    * This method is used to process the field an annotation given.
    * This will check to determine if the field is accessible, if it
    * is not accessible then it is made accessible so that private
    * member fields can be used during the serialization process.
    * 
    * @param field this is the field to be added as a contact
    * @param label this is the XML annotation used by the field
    * @param list this is the list of annotations on the field
    */
   private void process(Field field, Annotation label, Annotation[] list) {
      Contact contact = new FieldContact(field, label, list);
      Object key = new FieldKey(field);
      
      if(!field.isAccessible()) {
         field.setAccessible(true);              
      }  
      insert(key, contact);
   }
   
   /**
    * This is used to insert a contact to this contact list. Here if
    * a <code>Text</code> annotation is declared on a field that
    * already has an annotation then the other annotation is given
    * the priority, this is to so text can be processes separately.
    * 
    * @param key this is the key that uniquely identifies the field
    * @param contact this is the contact that is to be inserted
    */
   private void insert(Object key, Contact contact) {
      Contact existing = done.remove(key);
      
      if(existing != null)  {
         if(isText(contact)) {
            contact = existing;
         }
      }
      done.put(key, contact);
   }

   /**
    * This is used to determine if the <code>Text</code> annotation
    * has been declared on the field. If this annotation is used
    * then this will return true, otherwise this returns false.
    * 
    * @param contact the contact to check for the text annotation
    * 
    * @return true if the text annotation was declared on the field
    */
   private boolean isText(Contact contact) {
      Annotation label = contact.getAnnotation();
      
      if(label instanceof Text) {
         return true;
      }
      return false;
   }
   
   /**
    * This is used to remove a field from the map of processed fields.
    * A field is removed with the <code>Transient</code> annotation
    * is used to indicate that it should not be processed by the
    * scanner. This is required when default types are used.
    * 
    * @param field this is the field to be removed from the map
    * @param label this is the label associated with the field
    */
   private void remove(Field field, Annotation label) {
      done.remove(new FieldKey(field));
   }
 
   /**
    * This is used to build a list of valid contacts for this scanner.
    * Valid contacts are fields that are either defaulted or those
    * that have an explicit XML annotation. Any field that has been
    * marked as transient will not be considered as valid.
    */
   private void build() {
      for(Contact contact : done) {
         add(contact);
      }
   }
   
   /**
    * This is used to determine if a field is static. If a field is
    * static it should not be considered as a default field. This
    * ensures the default annotation does not pick up static finals.
    * 
    * @param field this is the field to determine if it is static
    * 
    * @return true if the field is static, false otherwise
    */
   private boolean isStatic(Field field) {
      int modifier = field.getModifiers();
      
      if(Modifier.isStatic(modifier)) {
         return true;
      }
      return false;
   }
   
   /**
    * This is used to determine if a field is transient. For default
    * fields that are processed no transient field should be 
    * considered. This ensures that the serialization of the object
    * behaves in the same manner as with Java Object Serialization.
    * 
    * @param field this is the field to check for transience
    * 
    * @return this returns true if the field is a transient one
    */
   private boolean isTransient(Field field) {
      int modifier = field.getModifiers();
      
      if(Modifier.isTransient(modifier)) {
         return true;
      }
      return false;
   }
   
   /**
    * The <code>FieldKey</code> object is used to create a key that
    * can store a contact using a field without using the methods
    * of <code>hashCode</code> and <code>equals</code> on the field
    * directly, as these can perform poorly on certain platforms.
    */
   private static class FieldKey {
      
      /**
       * This is the class that the field has been declared on.
       */
      private final Class type;
      
      /**
       * This is the name of the field that this represents.
       */
      private final String name;
      
      /**
       * Constructor of the <code>FieldKey</code> object. This is
       * used to create an object that can reference something
       * in a similar manner to a field. 
       * 
       * @param field this is the field to create the key with
       */
      public FieldKey(Field field) {
         this.type = field.getDeclaringClass();
         this.name = field.getName();
      }
      
      /**
       * This is basically the hash code for the field name. Because
       * field names are unique within a class collisions using 
       * just the name for the hash code should be infrequent.
       * 
       * @return this returns the hash code for this key
       */
      public int hashCode() {
         return name.hashCode();
      }
      
      /**
       * This method is used to compare this key to other keys. The
       * declaring class and the name of the field are used to test
       * for equality. If both are the same this returns true.
       * 
       * @param value this is the value that is to be compared to
       * 
       * @return this returns true if the field values are equal
       */
      public boolean equals(Object value) {
         if(value instanceof FieldKey) {
            return equals((FieldKey)value);
         }
         return false;
      }
      
      /**
       * This method is used to compare this key to other keys. The
       * declaring class and the name of the field are used to test
       * for equality. If both are the same this returns true.
       * 
       * @param other this is the value that is to be compared to
       * 
       * @return this returns true if the field values are equal
       */
      private boolean equals(FieldKey other) {
         if(other.type != type) {
            return false;
         }
         return other.name.equals(name);
      }
   }
}