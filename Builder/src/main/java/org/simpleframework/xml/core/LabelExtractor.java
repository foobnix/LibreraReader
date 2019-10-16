/*
 * LabelFactory.java July 2006
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

import static java.util.Collections.emptyList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.ElementMapUnion;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.Version;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>LabelExtractor</code> object is used to create instances of
 * the <code>Label</code> object that can be used to convert an XML
 * node into a Java object. Each label created requires the contact it
 * represents and the XML annotation it is marked with.  
 * <p>
 * The <code>Label</code> objects created by this factory a selected
 * using the XML annotation type. If the annotation type is not known
 * the factory will throw an exception, otherwise a label instance
 * is created that will expose the properties of the annotation.
 * 
 * @author Niall Gallagher
 */
class LabelExtractor {

   /**
    * This is used to cache the list of labels that have been created.
    */
   private final Cache<LabelGroup> cache;
  
   /**
    * Contains the format that is associated with the serializer.
    */
   private final Format format;
   
   /**
    * Constructor for the <code>LabelExtractor</code> object. This
    * creates an extractor that will extract labels for a specific
    * contact. Labels are cached within the extractor so that they 
    * can be looked up without having to rebuild it each time.
    * 
    * @param format this is the format used by the serializer
    */
   public LabelExtractor(Format format) {
      this.cache = new ConcurrentCache<LabelGroup>();
      this.format = format;
   }
   
   /**
    * Creates a <code>Label</code> using the provided contact and XML
    * annotation. The label produced contains all information related
    * to an object member. It knows the name of the XML entity, as
    * well as whether it is required. Once created the converter can
    * transform an XML node into Java object and vice versa.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * 
    * @return returns the label instantiated for the contact
    */
   public Label getLabel(Contact contact, Annotation label) throws Exception {
      Object key = getKey(contact, label);
      LabelGroup list = getGroup(contact, label, key);
      
      if(list != null) {
         return list.getPrimary();
      }
      return null;
   } 
   
   /**
    * Creates a <code>List</code> using the provided contact and XML
    * annotation. The labels produced contain all information related
    * to an object member. It knows the name of the XML entity, as
    * well as whether it is required. Once created the converter can
    * transform an XML node into Java object and vice versa.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * 
    * @return returns the list of labels associated with the contact
    */
   public List<Label> getList(Contact contact, Annotation label) throws Exception {
      Object key = getKey(contact, label);
      LabelGroup list = getGroup(contact, label, key);
      
      if(list != null) {
         return list.getList();
      }
      return emptyList();
   }

   /**
    * Creates a <code>LabelGroup</code> using the provided contact and
    * annotation. The labels produced contain all information related
    * to an object member. It knows the name of the XML entity, as
    * well as whether it is required. Once created the converter can
    * transform an XML node into Java object and vice versa.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * @param key this is the key that uniquely represents the contact
    * 
    * @return returns the list of labels associated with the contact
    */
   private LabelGroup getGroup(Contact contact, Annotation label, Object key) throws Exception {
      LabelGroup value = cache.fetch(key);
      
      if(value == null) {
         LabelGroup list = getLabels(contact, label);
         
         if(list != null) {
            cache.cache(key, list);
         }
         return list;
      }
      return value;
   }
   
   /**
    * Creates a <code>LabelGroup</code> using the provided contact and
    * annotation. The labels produced contain all information related
    * to an object member. It knows the name of the XML entity, as
    * well as whether it is required. Once created the converter can
    * transform an XML node into Java object and vice versa.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * 
    * @return returns the list of labels associated with the contact
    */
   private LabelGroup getLabels(Contact contact, Annotation label) throws Exception {
      if(label instanceof ElementUnion) {
         return getUnion(contact, label);
      }
      if(label instanceof ElementListUnion) {
         return getUnion(contact, label);
      }
      if(label instanceof ElementMapUnion) {
         return getUnion(contact, label);
      }
      return getSingle(contact, label);
   }
   
   /**
    * Creates a <code>LabelGroup</code> using the provided contact and
    * annotation. The labels produced contain all information related
    * to an object member. It knows the name of the XML entity, as
    * well as whether it is required. Once created the converter can
    * transform an XML node into Java object and vice versa.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * 
    * @return returns the list of labels associated with the contact
    */
   private LabelGroup getSingle(Contact contact, Annotation label) throws Exception {
      Label value = getLabel(contact, label, null);
      
      if(value != null) {
         value = new CacheLabel(value);
      }
      return new LabelGroup(value);
   }
   
   /**
    * Creates a <code>LabelGroup</code> using the provided contact and
    * annotation. The labels produced contain all information related
    * to an object member. It knows the name of the XML entity, as
    * well as whether it is required. Once created the converter can
    * transform an XML node into Java object and vice versa.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * 
    * @return returns the list of labels associated with the contact
    */
   private LabelGroup getUnion(Contact contact, Annotation label) throws Exception {
      Annotation[] list = getAnnotations(label);
      
      if(list.length > 0) {
         List<Label> labels = new LinkedList<Label>();
      
         for(Annotation value : list) {
            Label entry = getLabel(contact, label, value);
            
            if(entry != null) {
               entry = new CacheLabel(entry);
            }
            labels.add(entry);
         }
         return new LabelGroup(labels);
      }
      return null;
   }
   
   /**
    * This is used to extract the individual annotations associated
    * with the union annotation provided. If the annotation does
    * not represent a union then this will return null.
    * 
    * @param label this is the annotation to extract from
    * 
    * @return this returns an array of annotations from the union
    */
   private Annotation[] getAnnotations(Annotation label) throws Exception {
      Class union = label.annotationType();
      Method[] list = union.getDeclaredMethods();
      
      if(list.length > 0) {
         Method method = list[0];
         Object value = method.invoke(label);
      
         return (Annotation[])value;
      }
      return new Annotation[0];
   }
   
   /**
    * Creates a <code>Label</code> using the provided contact and XML
    * annotation. The label produced contains all information related
    * to an object member. It knows the name of the XML entity, as
    * well as whether it is required. Once created the converter can
    * transform an XML node into Java object and vice versa.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * @param entry this is the annotation used for the entries
    * 
    * @return returns the label instantiated for the field
    */
   private Label getLabel(Contact contact, Annotation label, Annotation entry) throws Exception {     
      Constructor factory = getConstructor(label);    
      
      if(entry != null) {
         return (Label)factory.newInstance(contact, label, entry, format);
      }
      return (Label)factory.newInstance(contact, label, format);
   }
    
   /**
    * This is used to create a key to uniquely identify a label that
    * is associated with a contact. A key contains the contact type,
    * the declaring class, the name, and the annotation type. This will
    * uniquely identify the label within the class.
    * 
    * @param contact this is contact that the label is produced for
    * @param label represents the XML annotation for the contact
    * 
    * @return this returns the key associated with the label
    */
   private Object getKey(Contact contact, Annotation label) {
      return new LabelKey(contact, label);
   }
   
    /**
     * Creates a constructor that can be used to instantiate the label
     * used to represent the specified annotation. The constructor
     * created by this method takes two arguments, a contact object 
     * and an <code>Annotation</code> of the type specified.
     * 
     * @param label the XML annotation representing the label
     * 
     * @return returns a constructor for instantiating the label 
     */
    private Constructor getConstructor(Annotation label) throws Exception {
       LabelBuilder builder = getBuilder(label);
       Constructor factory = builder.getConstructor();
       
       if(!factory.isAccessible()) {
          factory.setAccessible(true);
       }
       return factory;
    }
    
    /**
     * Creates an entry that is used to select the constructor for the
     * label. Each label must implement a constructor that takes a
     * contact and the specific XML annotation for that field. If the
     * annotation is not know this method throws an exception.
     * 
     * @param label the XML annotation used to create the label
     * 
     * @return this returns the entry used to create a constructor
     */
    private LabelBuilder getBuilder(Annotation label) throws Exception{   
       if(label instanceof Element) {
          return new LabelBuilder(ElementLabel.class, Element.class);
       }
       if(label instanceof ElementList) {
          return new LabelBuilder(ElementListLabel.class, ElementList.class);
       }
       if(label instanceof ElementArray) {
          return new LabelBuilder(ElementArrayLabel.class, ElementArray.class);               
       }
       if(label instanceof ElementMap) {
          return new LabelBuilder(ElementMapLabel.class, ElementMap.class);
       }
       if(label instanceof ElementUnion) {
          return new LabelBuilder(ElementUnionLabel.class, ElementUnion.class, Element.class);
       }
       if(label instanceof ElementListUnion) {
          return new LabelBuilder(ElementListUnionLabel.class, ElementListUnion.class, ElementList.class);
       }
       if(label instanceof ElementMapUnion) {
          return new LabelBuilder(ElementMapUnionLabel.class, ElementMapUnion.class, ElementMap.class);
       }
       if(label instanceof Attribute) {
          return new LabelBuilder(AttributeLabel.class, Attribute.class);
       }
       if(label instanceof Version) {
          return new LabelBuilder(VersionLabel.class, Version.class);
       }
       if(label instanceof Text) {
          return new LabelBuilder(TextLabel.class, Text.class);
       }
       throw new PersistenceException("Annotation %s not supported", label);
    }
    
    /**
     * The <code>LabelBuilder<code> object will create a constructor 
     * that can be used to instantiate the correct label for the XML
     * annotation specified. The constructor requires two arguments
     * a <code>Contact</code> and the specified XML annotation.
     * 
     * @see java.lang.reflect.Constructor
     */
    private static class LabelBuilder {
       
       /**       
        * This is the XML annotation type within the constructor.
        */
       private final Class label;
       
       /**
        * This is the individual entry annotation used for the label.
        */
       private final Class entry;
       
       /**
        * This is the label type that is to be instantiated.
        */
       private final Class type;
       
       /**
        * Constructor for the <code>LabelBuilder</code> object. This 
        * pairs the label type with the XML annotation argument used 
        * within the constructor. This create the constructor.
        * 
        * @param type this is the label type to be instantiated
        * @param label type that is used within the constructor
        */
       public LabelBuilder(Class type, Class label) {
          this(type, label, null);
       }
       
       /**
        * Constructor for the <code>LabelBuilder</code> object. This 
        * pairs the label type with the XML annotation argument used 
        * within the constructor. This will create the constructor.
        * 
        * @param type this is the label type to be instantiated
        * @param label type that is used within the constructor
        * @param entry entry that is used within the constructor
        */
       public LabelBuilder(Class type, Class label, Class entry) {
          this.entry = entry;
          this.label = label;
          this.type = type;
       }
       
       /**
        * Creates the constructor used to instantiate the label for
        * the XML annotation. The constructor returned will take two
        * arguments, a contact and the XML annotation type. 
        * 
        * @return returns the constructor for the label object
        */
       public Constructor getConstructor() throws Exception {
          if(entry != null) {
             return getConstructor(label, entry);
          }
          return getConstructor(label);
       }
       
       /**
        * Creates the constructor used to instantiate the label for
        * the XML annotation. The constructor returned will take two
        * arguments, a contact and the XML annotation type. 
        * 
        * @return returns the constructor for the label object
        */
       private Constructor getConstructor(Class label) throws Exception {
          return type.getConstructor(Contact.class, label, Format.class);
       }
       
       /**
        * Creates the constructor used to instantiate the label for
        * the XML annotation. The constructor returned will take two
        * arguments, a contact and the XML annotation type.
        * 
        * @param label this is the XML annotation argument type used
        * @param entry this is the entry type to use for the label
        * 
        * @return returns the constructor for the label object
        */
       private Constructor getConstructor(Class label, Class entry) throws Exception {
          return type.getConstructor(Contact.class, label, entry, Format.class);
       }
    }
}
