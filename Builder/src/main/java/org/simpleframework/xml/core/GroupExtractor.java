/*
 * GroupExtractor.java March 2011
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

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.simpleframework.xml.Text;
import org.simpleframework.xml.stream.Format;

/**
 * The <code>GroupExtractor</code> represents an extractor for labels
 * associated with a particular union annotation. This extractor 
 * registers <code>Label</code> by name and by type. Acquiring
 * the label by type allows the serialization process to dynamically
 * select a label, and thus converter, based on the instance type.
 * On deserialization a label is dynamically selected based on name.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Group
 */
class GroupExtractor implements Group {

   /**
    * This represents a factory for creating union extractors.
    */
   private final ExtractorFactory factory;
   
   /**
    * This represents the union label to be used for this group.
    */
   private final Annotation label;
   
   /**
    * This contains each label registered by name and by type.
    */
   private final Registry registry;
   
   /**
    * This contains each label registered by label name.
    */
   private final LabelMap elements;
   
   /**
    * Constructor for the <code>GroupExtractor</code> object. This
    * will create an extractor for the provided union annotation.
    * Each individual declaration within the union is extracted
    * and made available within the group.
    * 
    * @param contact this is the annotated field or method
    * @param label this is the label associated with the contact
    * @param format this is the format used by this extractor
    */
   public GroupExtractor(Contact contact, Annotation label, Format format) throws Exception{
      this.factory = new ExtractorFactory(contact, label, format);
      this.elements = new LabelMap();
      this.registry = new Registry(elements);
      this.label = label;
      this.extract();
   } 
   
   /**
    * This is used to acquire the names for each label associated
    * with this <code>Group</code> instance. The names provided
    * here are not styled according to a serialization context.
    * 
    * @return this returns the names of each union extracted
    */
   public String[] getNames() throws Exception {
      return elements.getKeys();
   }
   
   /**
    * This is used to acquire the paths for each label associated
    * with this <code>Group</code> instance. The paths provided
    * here are not styled according to a serialization context.
    * 
    * @return this returns the paths of each union extracted
    */
   public String[] getPaths() throws Exception {
      return elements.getPaths();
   }
   
   /**
    * This is used to acquire a <code>LabelMap</code> containing the
    * labels available to the group. Providing a context object 
    * ensures that each of the labels is mapped to a name that is
    * styled according to its internal style.
    * 
    * @return this returns a label map containing the labels 
    */
   public LabelMap getElements() throws Exception {
      return elements.getLabels();
   }

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
   public Label getLabel(Class type) {
      return registry.resolve(type);
   }
   
   /**
    * This is used to get a <code>Label</code> that represents the
    * text between elements on an element union. Providing a label
    * here ensures that the free text found between elements can
    * be converted in to strings and added to the list.
    * 
    * @return a label if a text annotation has been declared
    */
   public Label getText() {
      return registry.resolveText();
   }
   
   /**
    * This is used to determine if the associated type represents a
    * label defined within the union group. If the label exists
    * this returns true, if not then this returns false.
    * 
    * @param type this is the type to check for
    * 
    * @return this returns true if a label for the type exists
    */
   public boolean isValid(Class type) {
      return registry.resolve(type) != null;
   }
   
   /**
    * This is used to determine if a type has been declared by the
    * annotation associated with the group. Unlike determining if
    * the type is valid this will not consider super types.
    * 
    * @param type this is the type to determine if it is declared
    * 
    * @return this returns true if the type has been declared
    */
   public boolean isDeclared(Class type) {
      return registry.containsKey(type);
   }
   
   /**
    * This is used to determine if the group is inline. A group is
    * inline if all of the elements in the group is inline. If any of
    * the <code>Label<code> objects in the group is not inline then
    * the entire group is not inline, although this is unlikely.
    * 
    * @return this returns true if each label in the group is inline
    */
   public boolean isInline() {
      for(Label label : registry) {
         if(!label.isInline()) {
            return false;
         }
      }
      return !registry.isEmpty();
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
      return registry.isText();
   }
   
   /**
    * This is used to extract the labels associated with the group.
    * Extraction will instantiate a <code>Label</code> object for
    * an individual annotation declared within the union. Each of
    * the label instances is then registered by both name and type.
    */
   private void extract() throws Exception {
      Extractor extractor = factory.getInstance();

      if(extractor != null) {
         extract(extractor);
      }
   }
   
   /**
    * This is used to extract the labels associated with the group.
    * Extraction will instantiate a <code>Label</code> object for
    * an individual annotation declared within the union. Each of
    * the label instances is then registered by both name and type.
    * 
    * @param extractor this is the extractor to get labels for
    */
   private void extract(Extractor extractor) throws Exception {
      Annotation[] list = extractor.getAnnotations();
      
      for(Annotation label : list) {
         extract(extractor, label);
      }
   }
   
   /**
    * This is used to extract the labels associated with the group.
    * Extraction will instantiate a <code>Label</code> object for
    * an individual annotation declared within the union. Each of
    * the label instances is then registered by both name and type.
    * 
    * @param extractor this is the extractor to get labels for
    * @param value this is an individual annotation declared
    */
   private void extract(Extractor extractor, Annotation value) throws Exception {
      Label label = extractor.getLabel(value);
      Class type = extractor.getType(value);
      
      if(registry != null) {
         registry.register(type, label);
      }
   }
   
   /**
    * This returns a string representation of the union group.
    * Providing a string representation in this way ensures that the
    * group can be used in exception messages and for any debugging.
    * 
    * @return this returns a string representation of the group
    */
   public String toString() {
      return label.toString();
   }
   
   /**
    * The <code>Registry</code> object is used to maintain mappings
    * from types to labels. Each of the mappings can be used to 
    * dynamically select a label based on the instance type that is
    * to be serialized. This also registers based on the label name.
    */
   private static class Registry extends LinkedHashMap<Class, Label> implements Iterable<Label> {
   
      /**
       * This maintains a mapping between label names and labels.
       */
      private LabelMap elements;
      
      /**
       * This label represents the free text between elements.
       */
      private Label text;
      
      /**
       * Constructor for the <code>Registry</code> object. This is
       * used to register label instances using both the name and
       * type of the label. Registration in this way ensures that
       * each label can be dynamically selected.
       * 
       * @param elements this contains name to label mappings
       */
      public Registry(LabelMap elements){
         this.elements = elements;
      }
      
      /**
       * This is used to determine if an annotated list is a text 
       * list. A text list is a list of elements that also accepts
       * free text. Typically this will be an element list union that
       * will allow unstructured XML such as XHTML to be parsed.
       * 
       * @return returns true if the label represents a text list
       */
      public boolean isText() {
         return text != null;
      }
      
      /**
       * This is used so that all the <code>Label</code> objects
       * that have been added to the registry can be iterated over.
       * Iteration over the labels allows for easy validation.
       * 
       * @return this returns an iterator for all the labels
       */
      public Iterator<Label> iterator() {
         return values().iterator();
      }
      
      /**
       * This is used to resolve the text for this registry. If there
       * is a text annotation declared with the union then this will
       * return a <code>Label</code> that can be used to convert the
       * free text between elements in to strings.
       * 
       * @return this returns the label representing free text
       */
      public Label resolveText() {
         return resolveText(String.class);
      }
      
      /**
       * Here we resolve the <code>Label</code> the type is matched
       * with by checking if the type is directly mapped or if any of
       * the super classes of the type are mapped. If there are no
       * classes in the hierarchy of the type that are mapped then
       * this will return null otherwise the label will be returned.
       * 
       * @param type this is the type to resolve the label for
       * 
       * @return this will return the label that is best matched
       */
      public Label resolve(Class type) {
         Label label = resolveText(type);
         
         if(label == null) {
            return resolveElement(type);
         }
         return label;
      }
      
      /**
       * This is used to resolve the text for this registry. If there
       * is a text annotation declared with the union then this will
       * return a <code>Label</code> that can be used to convert the
       * free text between elements in to strings.
       * 
       * @param type this is the type to resolve the text as
       * 
       * @return this returns the label representing free text
       */
      private Label resolveText(Class type) {
         if(text != null) {
            if(type == String.class) {
               return text;
            }
         }
         return null;
      }
      
      /**
       * Here we resolve the <code>Label</code> the type is matched
       * with by checking if the type is directly mapped or if any of
       * the super classes of the type are mapped. If there are no
       * classes in the hierarchy of the type that are mapped then
       * this will return null otherwise the label will be returned.
       * 
       * @param type this is the type to resolve the label for
       * 
       * @return this will return the label that is best matched
       */
      private Label resolveElement(Class type) {
         while(type != null) {
            Label label = get(type);
            
            if(label != null) {
               return label;
            }
            type = type.getSuperclass();
         }
         return null; 
      }
      
      /**
       * This is used to register a label based on the name. This is
       * done to ensure the label instance can be dynamically found
       * during the deserialization process by providing the name.
       * 
       * @param name this is the name of the label to be registered
       * @param label this is the label that is to be registered
       */
      public void register(Class type, Label label) throws Exception {
         Label cache = new CacheLabel(label);
         
         registerElement(type, cache);
         registerText(cache);
      }
      
      
      /**
       * This is used to register a label based on the name. This is
       * done to ensure the label instance can be dynamically found
       * during the deserialization process by providing the name.
       * 
       * @param name this is the name of the label to be registered
       * @param label this is the label that is to be registered
       */
      private void registerElement(Class type, Label label) throws Exception {
         String name = label.getName();
         
         if(!elements.containsKey(name)) {
            elements.put(name, label);
         }
         if(!containsKey(type)) {
            put(type, label);
         }
      }
      
      /**
       * This is used to register the provided label is a text label.
       * Registration as a text label can only happen if the field or
       * method has a <code>Text</code> annotation declared on it.
       * 
       * @param label this is the label to register as text
       */
      private void registerText(Label label) throws Exception {
         Contact contact = label.getContact();
         Text value = contact.getAnnotation(Text.class);
         
         if(value != null) {
            text = new TextListLabel(label, value);
         }
      }
   }
} 