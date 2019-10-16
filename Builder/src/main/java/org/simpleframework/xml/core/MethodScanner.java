/*
 * MethodScanner.java April 2007
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

import static org.simpleframework.xml.DefaultType.PROPERTY;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 * The <code>MethodScanner</code> object is used to scan an object 
 * for matching get and set methods for an XML annotation. This will
 * scan for annotated methods starting with the most specialized
 * class up the class hierarchy. Thus, annotated methods can be 
 * overridden in a type specialization.
 * <p>
 * The annotated methods must be either a getter or setter method
 * following the Java Beans naming conventions. This convention is
 * such that a method must begin with "get", "set", or "is". A pair
 * of set and get methods for an annotation must make use of the
 * same type. For instance if the return type for the get method
 * was <code>String</code> then the set method must have a single
 * argument parameter that takes a <code>String</code> type.
 * <p>
 * For a method to be considered there must be both the get and set
 * methods. If either method is missing then the scanner fails with
 * an exception. Also, if an annotation marks a method which does
 * not follow Java Bean naming conventions an exception is thrown.
 *    
 * @author Niall Gallagher
 */
class MethodScanner extends ContactList {
   
   /**
    * This is a factory used for creating property method parts.
    */
   private final MethodPartFactory factory;
   
   /**
    * This object contains various support functions for the class.
    */
   private final Support support;
   
   /**
    * This is used to collect all the set methods from the object.
    */
   private final PartMap write;
   
   /**
    * This is used to collect all the get methods from the object.
    */
   private final PartMap read;

   /**
    * This contains the details for the class that is being scanned.
    */
   private final Detail detail;
      
   /**
    * Constructor for the <code>MethodScanner</code> object. This is
    * used to create an object that will scan the specified class
    * such that all bean property methods can be paired under the
    * XML annotation specified within the class.
    * 
    * @param detail this contains the details for the class scanned
    * @param support this contains various support functions
    */
   public MethodScanner(Detail detail, Support support) throws Exception {
      this.factory = new MethodPartFactory(detail, support);
      this.write = new PartMap();
      this.read = new PartMap();
      this.support = support;
      this.detail = detail;
      this.scan(detail);
   }
   
   /**
    * This method is used to scan the class hierarchy for each class
    * in order to extract methods that contain XML annotations. If
    * a method is annotated it is converted to a contact so that
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
      validate();
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
      ContactList list = support.getMethods(base, access);
      
      for(Contact contact : list) {
         process((MethodContact)contact);
      }
   }
   
   /**
    * This is used to scan the declared methods within the specified
    * class. Each method will be checked to determine if it contains
    * an XML element and can be used as a <code>Contact</code> for
    * an entity within the object.
    * 
    * @param detail this is one of the super classes for the object
    */
   private void extract(Detail detail) throws Exception {
      List<MethodDetail> methods = detail.getMethods();

      for(MethodDetail entry: methods) {
         Annotation[] list = entry.getAnnotations();
         Method method = entry.getMethod();
         
         for(Annotation label : list) {
            scan(method, label, list);             
         }
      }     
   }
   
   /**
    * This is used to scan all the methods of the class in order to
    * determine if it should have a default annotation. If the method
    * should have a default XML annotation then it is added to the
    * list of contacts to be used to form the class schema.
    * 
    * @param detail this is the detail to have its methods scanned
    * @param access this is the default access type for the class
    */
   private void extract(Detail detail, DefaultType access) throws Exception {
      List<MethodDetail> methods = detail.getMethods();

      if(access == PROPERTY) {
         for(MethodDetail entry : methods) {
            Annotation[] list = entry.getAnnotations();
            Method method = entry.getMethod();
            Class value = factory.getType(method);
            
            if(value != null) {
               process(method, list);
            }
         }  
      }
   }
   
   /**
    * This reflectively checks the annotation to determine the type 
    * of annotation it represents. If it represents an XML schema
    * annotation it is used to create a <code>Contact</code> which 
    * can be used to represent the method within the source object.
    * 
    * @param method the method that the annotation comes from
    * @param label the annotation used to model the XML schema
    * @param list this is the list of annotations on the method
    */ 
   private void scan(Method method, Annotation label, Annotation[] list) throws Exception {
      if(label instanceof Attribute) {
         process(method, label, list);
      }
      if(label instanceof ElementUnion) {
         process(method, label, list);
      }
      if(label instanceof ElementListUnion) {
         process(method, label, list);
      }
      if(label instanceof ElementMapUnion) {
         process(method, label, list);
      }
      if(label instanceof ElementList) {
         process(method, label, list);
      }
      if(label instanceof ElementArray) {
         process(method, label, list);
      }
      if(label instanceof ElementMap) {
         process(method, label, list);
      }
      if(label instanceof Element) {
         process(method, label, list);
      }    
      if(label instanceof Version) {
         process(method, label, list);
      }
      if(label instanceof Text) {
         process(method, label, list);
      }
      if(label instanceof Transient) {
         remove(method, label, list);
      }
   }
  
   /**
    * This is used to classify the specified method into either a get
    * or set method. If the method is neither then an exception is
    * thrown to indicate that the XML annotations can only be used
    * with methods following the Java Bean naming conventions. Once
    * the method is classified is is added to either the read or 
    * write map so that it can be paired after scanning is complete.
    * 
    * @param method this is the method that is to be classified
    * @param label this is the annotation applied to the method
    * @param list this is the list of annotations on the method
    */  
   private void process(Method method, Annotation label, Annotation[] list) throws Exception {
      MethodPart part = factory.getInstance(method, label, list);
      MethodType type = part.getMethodType();     
      
      if(type == MethodType.GET) {
         process(part, read);
      }
      if(type == MethodType.IS) {
         process(part, read);
      }
      if(type == MethodType.SET) {
         process(part, write);
      }
   } 
   
   /**
    * This is used to classify the specified method into either a get
    * or set method. If the method is neither then an exception is
    * thrown to indicate that the XML annotations can only be used
    * with methods following the Java Bean naming conventions. Once
    * the method is classified is is added to either the read or 
    * write map so that it can be paired after scanning is complete.
    * 
    * @param method this is the method that is to be classified
    * @param list this is the list of annotations on the method
    */  
   private void process(Method method, Annotation[] list) throws Exception {
      MethodPart part = factory.getInstance(method, list);
      MethodType type = part.getMethodType();     
      
      if(type == MethodType.GET) {
         process(part, read);
      }
      if(type == MethodType.IS) {
         process(part, read);
      }
      if(type == MethodType.SET) {
         process(part, write);
      }      
   }
   
   /**
    * This is used to determine whether the specified method can be
    * inserted into the given <code>PartMap</code>. This ensures 
    * that only the most specialized method is considered, which 
    * enables annotated methods to be overridden in subclasses.
    * 
    * @param method this is the method part that is to be inserted
    * @param map this is the part map used to contain the method
    */
   private void process(MethodPart method, PartMap map) {
      String name = method.getName();
      
      if(name != null) {
         map.put(name, method);
      }
   }
   
   /**
    * This is used to process a method from a super class. Processing
    * the inherited method involves extracting out the individual
    * parts of the method an initializing the internal state of this
    * scanner. If method is overridden it overwrites the parts.
    * 
    * @param contact this is a method inherited from a super class
    */
   private void process(MethodContact contact) {
      MethodPart get = contact.getRead();
      MethodPart set = contact.getWrite();
      
      if(set != null) {
         insert(set, write);
      }
      insert(get, read);
   }
   
   /**
    * This is used to insert a contact to this contact list. Here if
    * a <code>Text</code> annotation is declared on a method that
    * already has an annotation then the other annotation is given
    * the priority, this is to so text can be processes separately.
    * 
    * @param method this is the part that is to be inserted
    * @param map this is the map that the part is to be inserted in
    */
   private void insert(MethodPart method, PartMap map) {
      String name = method.getName();
      MethodPart existing = map.remove(name);
      
      if(existing != null) {
         if(isText(method)) {
            method = existing;
         }
      }
      map.put(name, method);
   }
      
   /**
    * This is used to determine if the <code>Text</code> annotation
    * has been declared on the method. If this annotation is used
    * then this will return true, otherwise this returns false.
    * 
    * @param contact the contact to check for the text annotation
    * 
    * @return true if the text annotation was declared on the method
    */
   private boolean isText(MethodPart method) {
      Annotation label = method.getAnnotation();
      
      if(label instanceof Text) {
         return true;
      }
      return false;
   }
   
   /**
    * This method is used to remove a particular method from the list
    * of contacts. If the <code>Transient</code> annotation is used
    * by any method then this method must be removed from the schema.
    * In particular it is important to remove methods if there are
    * defaults applied to the class.
    * 
    * @param method this is the method that is to be removed
    * @param label this is the label associated with the method
    * @param list this is the list of annotations on the method
    */
   private void remove(Method method, Annotation label, Annotation[] list) throws Exception {
      MethodPart part = factory.getInstance(method, label, list);
      MethodType type = part.getMethodType();     
      
      if(type == MethodType.GET) {
         remove(part, read);
      }
      if(type == MethodType.IS) {
         remove(part, read);
      }
      if(type == MethodType.SET) {
         remove(part, write);
      }
   } 
   
   /**
    * This is used to remove the method part from the specified map.
    * Removal is performed using the name of the method part. If it
    * has been scanned and added to the map then it will be removed
    * and will not form part of the class schema.
    * 
    * @param part this is the part to be removed from the map 
    * @param map this is the map to removed the method part from
    */
   private void remove(MethodPart part, PartMap map) throws Exception {
      String name = part.getName();
      
      if(name != null) {
         map.remove(name);
      }
   }
   
   /**
    * This method is used to pair the get methods with a matching set
    * method. This pairs methods using the Java Bean method name, the
    * names must match exactly, meaning that the case and value of
    * the strings must be identical. Also in order for this to succeed
    * the types for the methods and the annotation must also match.
    */
   private void build() throws Exception {
      for(String name : read) {
         MethodPart part = read.get(name);
         
         if(part != null) {
            build(part, name);
         }
      }
   }
   
   /**
    * This method is used to pair the get methods with a matching set
    * method. This pairs methods using the Java Bean method name, the
    * names must match exactly, meaning that the case and value of
    * the strings must be identical. Also in order for this to succeed
    * the types for the methods and the annotation must also match.
    * 
    * @param read this is a get method that has been extracted
    * @param name this is the Java Bean methods name to be matched
    */
   private void build(MethodPart read, String name) throws Exception {      
      MethodPart match = write.take(name);

      if(match != null) {
         build(read, match);
      } else {
         build(read); 
      }
   }   
   
   /**
    * This method is used to create a read only contact. A read only
    * contact object is used when there is constructor injection used
    * by the class schema. So, read only methods can be used in a 
    * fully serializable and deserializable object.
    * 
    * @param read this is the part to add as a read only contact
    */
   private void build(MethodPart read) throws Exception {
      add(new MethodContact(read));
   }
   
   /**
    * This method is used to pair the get methods with a matching set
    * method. This pairs methods using the Java Bean method name, the
    * names must match exactly, meaning that the case and value of
    * the strings must be identical. Also in order for this to succeed
    * the types for the methods and the annotation must also match.
    * 
    * @param read this is a get method that has been extracted
    * @param write this is the write method to compare details with    
    */
   private void build(MethodPart read, MethodPart write) throws Exception {
      Annotation label = read.getAnnotation();
      String name = read.getName();
      
      if(!write.getAnnotation().equals(label)) {
         throw new MethodException("Annotations do not match for '%s' in %s", name, detail);
      }
      Class type = read.getType();
      
      if(type != write.getType()) {
         throw new MethodException("Method types do not match for %s in %s", name, type);
      }
      add(new MethodContact(read, write));
   }
   
   /**
    * This is used to validate the object once all the get methods
    * have been matched with a set method. This ensures that there
    * is not a set method within the object that does not have a
    * match, therefore violating the contract of a property.
    */
   private void validate() throws Exception {
      for(String name : write) {
         MethodPart part = write.get(name);
         
         if(part != null) {
            validate(part, name);
         }
      }
   }
   
   /**
    * This is used to validate the object once all the get methods
    * have been matched with a set method. This ensures that there
    * is not a set method within the object that does not have a
    * match, therefore violating the contract of a property.
    * 
    * @param write this is a get method that has been extracted
    * @param name this is the Java Bean methods name to be matched 
    */
   private void validate(MethodPart write, String name) throws Exception {      
      MethodPart match = read.take(name);     
      Method method = write.getMethod();      
         
      if(match == null) {
         throw new MethodException("No matching get method for %s in %s", method, detail);
      }      
   }
   
   /**
    * The <code>PartMap</code> is used to contain method parts using
    * the Java Bean method name for the part. This ensures that the
    * scanned and extracted methods can be acquired using a common 
    * name, which should be the parsed Java Bean method name.
    * 
    * @see org.simpleframework.xml.core.MethodPart
    */
   private static class PartMap extends LinkedHashMap<String, MethodPart> implements Iterable<String>{
      
      /**
       * This returns an iterator for the Java Bean method names for
       * the <code>MethodPart</code> objects that are stored in the
       * map. This allows names to be iterated easily in a for loop.
       * 
       * @return this returns an iterator for the method name keys
       */
      public Iterator<String> iterator() {
         return keySet().iterator();
      }
      
      /**
       * This is used to acquire the method part for the specified
       * method name. This will remove the method part from this map
       * so that it can be checked later to ensure what remains.
       * 
       * @param name this is the method name to get the method with       
       * 
       * @return this returns the method part for the given key
       */
      public MethodPart take(String name) {
         return remove(name);
      }
   }
}