/*
 * InstantiatorBuilder.java July 2011
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

import static org.simpleframework.xml.core.Support.isAssignable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>InstantiatorBuilder</code> object is used to resolve labels
 * based on a provided parameter. Resolution of labels is done using
 * a set of mappings based on the names of the labels. Because many
 * labels might have the same name, this will only map a label if it
 * has unique name. As well as resolving by name, this can resolve
 * using the path of the parameter.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.StructureBuilder
 */
class InstantiatorBuilder {

   /**
    * This is the list of creators representing an object constructor.
    */
   private List<Creator> options; 
   
   /**
    * This is the single instance of the instantiator to be built.
    */
   private Instantiator factory;
   
   /**
    * This is used to maintain the mappings for the attribute labels.
    */
   private LabelMap attributes;
   
   /**
    * This is used to maintain the mappings for the element labels.
    */
   private LabelMap elements;
   
   /**
    * This is used to maintain the mappings for the text labels.
    */
   private LabelMap texts;
  
   /**
    * This is used for validation to compare annotations used.
    */
   private Comparer comparer;
  
   /**
    * This is used to acquire the signatures created for the type.
    */
   private Scanner scanner;
   
   /**
    * This is the detail the instantiator uses to create objects.
    */
   private Detail detail;
   
   /**
    * Constructor for the <code>InstantiatorBuilder</code> object. This 
    * is used to create an object that can resolve a label using a
    * parameter. Resolution is performed using either the name of
    * the parameter or the path of the parameter.
    * 
    * @param scanner this is the scanner to acquire signatures from
    * @param detail contains the details instantiators are built with
    */
   public InstantiatorBuilder(Scanner scanner, Detail detail) {
      this.options = new ArrayList<Creator>();
      this.comparer = new Comparer();
      this.attributes = new LabelMap();
      this.elements = new LabelMap();
      this.texts = new LabelMap();
      this.scanner = scanner;
      this.detail = detail;
   }
   
   /**
    * This is used to build the <code>Instantiator</code> object that
    * will be used to instantiate objects. Validation is performed on 
    * all of the parameters as well as the <code>Creator</code> objects
    * associated with the type. This validation ensures that the labels
    * and constructor parameters match based on annotations.
    * 
    * @return this returns the instance that has been built
    */
   public Instantiator build() throws Exception {
      if(factory == null) {
         populate(detail);
         build(detail);
         validate(detail);
      }
      return factory;
   }
   
   /**
    * This is used to build the <code>Instantiator</code> object that
    * will be used to instantiate objects. Validation is performed on 
    * all of the parameters as well as the <code>Creator</code> objects
    * associated with the type. This validation ensures that the labels
    * and constructor parameters match based on annotations.
    * 
    * @param detail contains the details instantiators are built with
    */
   private Instantiator build(Detail detail) throws Exception {
      if(factory == null) {
         factory = create(detail);
      }
      return factory;
   }
   
   /**
    * This is used to create the <code>Instantiator</code> object that
    * will be used to instantiate objects. Validation is performed on 
    * all of the parameters as well as the <code>Creator</code> objects
    * associated with the type. This validation ensures that the labels
    * and constructor parameters match based on annotations.
    * 
    * @param detail contains the details instantiators are built with
    * 
    * @return this returns the instance that has been created
    */
   private Instantiator create(Detail detail) throws Exception {
      Signature primary = scanner.getSignature();
      ParameterMap registry = scanner.getParameters();
      Creator creator = null;
      
      if(primary != null) {
         creator = new SignatureCreator(primary);
      }
      return new ClassInstantiator(options, creator, registry, detail);
   }
   
   /**
    * This is used to create a new <code>Creator</code> object from 
    * the provided signature. Once created the instance is added to
    * the list of available creators which can be used to instantiate
    * an object instance.
    * 
    * @param signature this is the signature that is to be used
    *  
    * @return this returns the creator associated with this builder
    */
   private Creator create(Signature signature) {
      Creator creator = new SignatureCreator(signature); 
      
      if(signature != null) {
         options.add(creator);
      }
      return creator;
   }
   
   /**
    * This is used to create a <code>Parameter</code> based on the 
    * currently registered labels. Creating a replacement in this way
    * ensures that all parameters are keyed in the exact same way 
    * that the label is, making it easier and quicker to match them.
    * 
    * @param original this is the original parameter to replace
    * 
    * @return this returns the replacement parameter object
    */
   private Parameter create(Parameter original) throws Exception {
      Label label = resolve(original);
      
      if(label != null) {
         return new CacheParameter(original, label);
      }
      return null;
   }
   
   /**
    * This used to populate replace the parameters extracted from the
    * scanning process with ones matched with registered labels. By 
    * replacing parameters in this way the parameters can be better
    * matched with the associated labels using the label keys.
    * 
    * @param detail contains the details instantiators are built with
    */
   private void populate(Detail detail) throws Exception {
      List<Signature> list = scanner.getSignatures();
      
      for(Signature signature : list) {
         populate(signature);
      }
   }
   
   /**
    * This used to populate replace the parameters extracted from the
    * scanning process with ones matched with registered labels. By 
    * replacing parameters in this way the parameters can be better
    * matched with the associated labels using the label keys.
    * 
    * @param signature this is the signature used for a creator
    */
   private void populate(Signature signature) throws Exception {
      Signature substitute = new Signature(signature);   
      
      for(Parameter parameter : signature) {
         Parameter replace = create(parameter);
         
         if(replace != null) {
            substitute.add(replace);
         }
      }
      create(substitute);
   }
   
   /**
    * This is used to ensure that for each parameter in the builder
    * there is a matching method or field. This ensures that the
    * class schema is fully readable and writable. If not method or
    * field annotation exists for the parameter validation fails.
    * 
    * @param detail contains the details instantiators are built with
    */
   private void validate(Detail detail) throws Exception {
      ParameterMap registry = scanner.getParameters();
      List<Parameter> list = registry.getAll();
      
      for(Parameter parameter : list) {
         Label label = resolve(parameter);
         String path = parameter.getPath();
         
         if(label == null) {
            throw new ConstructorException("Parameter '%s' does not have a match in %s", path, detail);
         }
         validateParameter(label, parameter);
      }
      validateConstructors();
   }
   
   /**
    * This is used to validate the <code>Parameter</code> object that
    * exist in the constructors. Validation is performed against the
    * annotated methods and fields to ensure that they match up.
    * 
    * @param label this is the annotated method or field to validate
    * @param parameter this is the parameter to validate with
    */
   private void validateParameter(Label label, Parameter parameter) throws Exception {
      Contact contact = label.getContact();
      String name = parameter.getName();
      Class expect = parameter.getType();
      Class actual = contact.getType();
      
      if(!isAssignable(expect, actual)) {
         throw new ConstructorException("Type is not compatible with %s for '%s' in %s", label, name, parameter);
      }
      validateNames(label, parameter);
      validateAnnotations(label, parameter);
   }
   
   /**
    * This is used to validate the names associated with the parameters.
    * Validation is performed by checking that the parameter name is
    * present in the list of names the label is known by. This is used
    * to ensure that the if the label is a union the parameter is one of
    * the names declared within the union.
    * 
    * @param label this is the label to validate the parameter against
    * @param parameter this is the parameter that is to be validated
    */
   private void validateNames(Label label, Parameter parameter) throws Exception {
      String[] options = label.getNames();
      String name = parameter.getName();
      
      if(!contains(options, name)) {
         String require = label.getName();
         
         if(name != require) {
            if(name == null || require == null) {
               throw new ConstructorException("Annotation does not match %s for '%s' in %s", label, name, parameter);
            }
            if(!name.equals(require)) {
               throw new ConstructorException("Annotation does not match %s for '%s' in %s", label, name, parameter);              
            }
         }
      }
   }
   
   /**
    * This is used to validate the annotations associated with a field
    * and a matching constructor parameter. Each constructor parameter
    * paired with an annotated field or method must be the same 
    * annotation type and must also contain the same name.
    * 
    * @param label this is the label associated with the parameter
    * @param parameter this is the constructor parameter to use
    */
   private void validateAnnotations(Label label, Parameter parameter) throws Exception {
      Annotation field = label.getAnnotation();
      Annotation argument = parameter.getAnnotation();
      String name = parameter.getName();     
      
      if(!comparer.equals(field, argument)) {
         Class expect = field.annotationType();
         Class actual = argument.annotationType();
         
         if(!expect.equals(actual)) {
            throw new ConstructorException("Annotation %s does not match %s for '%s' in %s", actual, expect, name, parameter);  
         } 
      }
   }
   
   /**
    * This is used to ensure that final methods and fields have a 
    * constructor parameter that allows the value to be injected in
    * to. Validating the constructor in this manner ensures that the
    * class schema remains fully serializable and deserializable.
    */   
   private void validateConstructors() throws Exception {
      List<Creator> list = factory.getCreators(); 
      
      if(factory.isDefault()) {
         validateConstructors(elements);
         validateConstructors(attributes);
      }
      if(!list.isEmpty()) {
         validateConstructors(elements, list);
         validateConstructors(attributes, list);
      }
   }
   
   /**
    * This is used when there are only default constructors. It will
    * check to see if any of the annotated fields or methods is read
    * only. If a read only method or field is found then this will
    * throw an exception to indicate that it is not valid. 
    * 
    * @param map this is the map of values that is to be validated
    */
   private void validateConstructors(LabelMap map) throws Exception {
      for(Label label : map) {
         if(label != null) {
            Contact contact = label.getContact();
            
            if(contact.isReadOnly()) {
               throw new ConstructorException("Default constructor can not accept read only %s in %s", label, detail);
            }
         }
      }
   }
   
   /**
    * This is used to ensure that final methods and fields have a 
    * constructor parameter that allows the value to be injected in
    * to. Validating the constructor in this manner ensures that the
    * class schema remains fully serializable and deserializable.
    * 
    * @param map this is the map that contains the labels to validate
    * @param list this is the list of builders to validate
    */
   private void validateConstructors(LabelMap map, List<Creator> list) throws Exception {      
      for(Label label : map) {         
         if(label != null) {
            validateConstructor(label, list);
         }
      }   
      if(list.isEmpty()) {
         throw new ConstructorException("No constructor accepts all read only values in %s", detail);
      }
   }
   
   /**
    * This is used to ensure that final methods and fields have a 
    * constructor parameter that allows the value to be injected in
    * to. Validating the constructor in this manner ensures that the
    * class schema remains fully serializable and deserializable.
    * 
    * @param label this is the variable to check in constructors
    * @param list this is the list of builders to validate
    */
   private void validateConstructor(Label label, List<Creator> list) throws Exception {
      Iterator<Creator> iterator = list.iterator();

      while(iterator.hasNext()) {
         Creator instantiator = iterator.next();
         Signature signature = instantiator.getSignature();
         Contact contact = label.getContact();
         Object key = label.getKey();

         if(contact.isReadOnly()) {
            Parameter value = signature.get(key);

            if(value == null) {
               iterator.remove();
            }
         }
      }
   }
   
   /**
    * This <code>register</code> method is used to register a label
    * based on its name and path. Registration like this is done
    * to ensure that the label can be resolved based on a parameter
    * name or path. 
    * 
    * @param label this is the label that is to be registered
    */
   public void register(Label label) throws Exception {
      if(label.isAttribute()) {
         register(label, attributes);
      } else if(label.isText()){
         register(label, texts);
      } else {
         register(label, elements);
      }
   }
   
   /**
    * This <code>register</code> method is used to register a label
    * based on its name and path. Registration like this is done
    * to ensure that the label can be resolved based on a parameter
    * name or path. 
    * <p>
    * Registration here ensures a parameter can be resolved on both
    * name and path. However, because we want these mappings to be
    * unique we do not allow multiple names to be mapped to the same
    * label. For example, say we have 'x/@a' and 'y/@a', these both 
    * have the same name 'a' even though the point/put( to different
    * things. Here we would not allow a mapping from 'a' and keep
    * only mappings based on the full path. This means that any
    * parameters specified must declare the full path also.
    * 
    * @param label this is the label that is to be registered
    * @param map this is the map that the label is registered with
    */
   private void register(Label label, LabelMap map) throws Exception {
      String name = label.getName();
      String path = label.getPath();
      
      if(map.containsKey(name)) {
         Label current = map.get(name);
         String key = current.getPath();
         
         if(!key.equals(name)) {
            map.remove(name); 
         }
      } else {
         map.put(name, label);
      }
      map.put(path, label);
   }
   
   /**
    * This <code>resolve</code> method is used to find a label based
    * on the name and path of the provided parameter. If it can not
    * be found then this will return null.
    * 
    * @param parameter this is the parameter used for resolution
    * 
    * @return the label that has been resolved, or null
    */
   private Label resolve(Parameter parameter) throws Exception {
      if(parameter.isAttribute()) {
         return resolve(parameter, attributes);
      } else if(parameter.isText()){
         return resolve(parameter, texts);
      }
      return resolve(parameter, elements);
 
   }
   
   /**
    * This <code>resolve</code> method is used to find a label based
    * on the name and path of the provided parameter. If it can not
    * be found then this will return null.
    * 
    * @param parameter this is the parameter used for resolution
    * @param map this is the map that is used for resolution
    * 
    * @return the label that has been resolved, or null
    */
   private Label resolve(Parameter parameter, LabelMap map) throws Exception {
      String name = parameter.getName();
      String path = parameter.getPath();
      Label label = map.get(path);
      
      if(label == null) {
         return map.get(name);
      }
      return label;
   }
   
   /**
    * This is used to determine if a value exists within an array. 
    * Searching the array like this is required when no collections 
    * are available to use on the list of attributes.
    * 
    * @param list this is the list to begin searching for a value
    * @param value this is the value to be searched for
    * 
    * @return true if the list contains the specified value
    */
   private boolean contains(String[] list, String value) throws Exception {
      for(String entry : list) {
        if(entry == value) {
           return true;
        }
        if(entry.equals(value)) {
           return true;
        }
      }
      return false;
   }
}