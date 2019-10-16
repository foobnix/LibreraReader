/*
 * SignatureScanner.java July 2009
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
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

/**
 * The <code>SignatureScanner</code> object is used to scan each of
 * the parameters within a constructor for annotations. When each of
 * the annotations has been extracted it is used to build a parameter
 * which is then used to build a grid of parameter annotation pairs.
 * A single constructor can result in multiple signatures and if a
 * union annotation is used like <code>ElementUnion</code> then this
 * can result is several annotations being declared.
 * 
 * @author Niall Gallagher
 */
class SignatureScanner {
   
   /**
    * This is used to build permutations of parameters extracted.
    */
   private final SignatureBuilder builder;
   
   /**
    * This factory is used to creating annotated parameter objects.
    */
   private final ParameterFactory factory;
   
   /**
    * This is used to collect all the parameters that are extracted.
    */
   private final ParameterMap registry;
   
   /**
    * This is the constructor that is scanned for the parameters.
    */
   private final Constructor constructor;
   
   /**
    * This is the declaring class for the constructor scanned.
    */
   private final Class type;
 
   /**
    * Constructor for the <code>SignatureScanner</code> object. This
    * creates a scanner for a single constructor. As well as scanning
    * for parameters within the constructor this will collect each
    * of the scanned parameters in a registry so it can be validated.
    * 
    * @param constructor this is the constructor that will be scanned
    * @param registry this is the registry used to collect parameters
    * @param format this is the format used to style parameters
    */
   public SignatureScanner(Constructor constructor, ParameterMap registry, Support support) throws Exception {
      this.builder = new SignatureBuilder(constructor);
      this.factory = new ParameterFactory(support);
      this.type = constructor.getDeclaringClass();
      this.constructor = constructor;
      this.registry = registry;
      this.scan(type);
   }
   
   /**
    * This is used to determine if this scanner is valid. The scanner
    * may not be valid for various reasons. Currently this method 
    * determines if a scanner is valid by checking the constructor
    * to see if the object can be instantiated.
    * 
    * @return this returns true if this scanner is valid
    */
   public boolean isValid() {
      return builder.isValid();
   }
   
   /**
    * This is used to acquire the signature permutations for the
    * constructor. Typically this will return a single signature. If
    * the constructor parameters are annotated with unions then this
    * will return several signatures representing each permutation.
    * 
    * @return this signatures that have been extracted from this
    */
   public List<Signature> getSignatures() throws Exception {
      return builder.build();
   }
   
   /**
    * This is used to scan the specified constructor for annotations
    * that it contains. Each parameter annotation is evaluated and 
    * if it is an XML annotation it is considered to be a valid
    * parameter and is added to the signature builder.
    * 
    * @param type this is the constructor that is to be scanned
    */
   private void scan(Class type) throws Exception {
      Class[] types = constructor.getParameterTypes();

      for(int i = 0; i < types.length; i++) {         
         scan(types[i], i);
      }
   }
   
   /**
    * This is used to scan the specified constructor for annotations
    * that it contains. Each parameter annotation is evaluated and 
    * if it is an XML annotation it is considered to be a valid
    * parameter and is added to the signature builder.
    * 
    * @param type this is the parameter type to be evaluated
    * @param index this is the index of the parameter to scan
    */
   private void scan(Class type, int index) throws Exception {
      Annotation[][] labels = constructor.getParameterAnnotations();
        
      for(int j = 0; j < labels[index].length; j++) {
         Collection<Parameter> value = process(labels[index][j], index); 
         
         for(Parameter parameter : value) {
            builder.insert(parameter, index);
         }
      }
   }
   
   /**
    * This is used to create <code>Parameter</code> objects which are
    * used to represent the parameters in a constructor. Each parameter
    * contains an annotation an the index it appears in.
    * 
    * @param label this is the annotation used for the parameter
    * @param ordinal this is the position the parameter appears at
    * 
    * @return this returns the parameters for the constructor
    */
   private List<Parameter> process(Annotation label, int ordinal) throws Exception{
      if(label instanceof Attribute) {
         return create(label, ordinal);
      }
      if(label instanceof Element) {
         return create(label, ordinal);
      }
      if(label instanceof ElementList) {
         return create(label, ordinal);
      }     
      if(label instanceof ElementArray) {
         return create(label, ordinal);
      }
      if(label instanceof ElementMap) {
         return create(label, ordinal);
      }
      if(label instanceof ElementListUnion) {
         return union(label, ordinal);
      }     
      if(label instanceof ElementMapUnion) {
         return union(label, ordinal);
      }
      if(label instanceof ElementUnion) {
         return union(label, ordinal);
      }
      if(label instanceof Text) {
         return create(label, ordinal);
      }
      return emptyList();
   }
   
   /**
    * This is used to create a <code>Parameter</code> object which is
    * used to represent a parameter to a constructor. Each parameter
    * contains an annotation an the index it appears in.
    * 
    * @param label this is the annotation used for the parameter
    * @param ordinal this is the position the parameter appears at
    * 
    * @return this returns the parameter for the constructor
    */
   private List<Parameter> union(Annotation label, int ordinal) throws Exception {
      Signature signature = new Signature(constructor);
      Annotation[] list = extract(label);

      for(Annotation value : list) {
         Parameter parameter = factory.getInstance(constructor, label, value, ordinal);
         String path = parameter.getPath(); 
         
         if(signature.contains(path)) {
            throw new UnionException("Annotation name '%s' used more than once in %s for %s", path, label, type);
         } else {
            signature.set(path, parameter);
         }
         register(parameter);
      }
      return signature.getAll();
   }
   
   /**
    * This is used to create a <code>Parameter</code> object which is
    * used to represent a parameter to a constructor. Each parameter
    * contains an annotation an the index it appears in.
    * 
    * @param label this is the annotation used for the parameter
    * @param ordinal this is the position the parameter appears at
    * 
    * @return this returns the parameter for the constructor
    */
   private List<Parameter> create(Annotation label, int ordinal) throws Exception {
      Parameter parameter = factory.getInstance(constructor, label, ordinal);
      
      if(parameter != null) {
         register(parameter);
      }
      return singletonList(parameter);
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
   private Annotation[] extract(Annotation label) throws Exception {
      Class union = label.annotationType();
      Method[] list = union.getDeclaredMethods();
      
      if(list.length != 1) {
         throw new UnionException("Annotation '%s' is not a valid union for %s", label, type);
      }
      Method method = list[0];
      Object value = method.invoke(label);
      
      return (Annotation[])value;
   }
   
   /**
    * This is used to register the provided parameter using the
    * given path. If this parameter has already existed then this
    * will validate the parameter against the existing one.  All
    * registered parameters are registered in to a single table.
    * 
    * @param parameter this is the parameter to be registered
    */
   private void register(Parameter parameter) throws Exception {
      String path = parameter.getPath();
      Object key = parameter.getKey();
      
      if(registry.containsKey(key)) {
         validate(parameter, key);
      }
      if(registry.containsKey(path)) {
         validate(parameter, path);
      }
      registry.put(path, parameter);
      registry.put(key, parameter);
   }
   
   /**
    * This is used to validate the parameter against all the other
    * parameters for the class. Validating each of the parameters
    * ensures that the annotations for the parameters remain
    * consistent throughout the class.
    * 
    * @param parameter this is the parameter to be validated
    * @param key this is the key of the parameter to validate
    */
   private void validate(Parameter parameter, Object key) throws Exception {
      Parameter other = registry.get(key);
      
      if(parameter.isText() != other.isText()) {
         Annotation expect = parameter.getAnnotation();
         Annotation actual = other.getAnnotation();
         String path = parameter.getPath();
         
         if(!expect.equals(actual)) {
            throw new ConstructorException("Annotations do not match for '%s' in %s", path, type);
         }
         Class real = other.getType();
      
         if(real != parameter.getType()) {
            throw new ConstructorException("Parameter types do not match for '%s' in %s", path, type);
         }
      }
   }
}
