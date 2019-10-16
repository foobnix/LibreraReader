/*
 * ParameterFactory.java July 2006
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
import java.lang.reflect.Constructor;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.ElementMapUnion;
import org.simpleframework.xml.ElementUnion;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.stream.Format;

/**
 * The <code>ParameterFactory</code> object is used to create instances 
 * of the <code>Parameter</code> object. Each parameter created can be
 * used to validate against the annotated fields and methods to ensure
 * that the annotations are compatible. 
 * <p>
 * The <code>Parameter</code> objects created by this are selected
 * using the XML annotation type. If the annotation type is not known
 * the factory will throw an exception, otherwise a parameter instance
 * is created that will expose the properties of the annotation.
 * 
 * @author Niall Gallagher
 */
class ParameterFactory {

   /**
    * This format contains the style which is used to create names.
    */
   private final Format format;
   
   /**
    * Constructor for the <code>ParameterFactory</code> object. This 
    * factory is used for creating parameters within a constructor.
    * Parameters can be annotated in the same way as methods or
    * fields, this object is used to create such parameters.
    * 
    * @param support this contains various support functions
    */
   public ParameterFactory(Support support) {
      this.format = support.getFormat();
   }
   
   /**
    * Creates a <code>Parameter</code> using the provided constructor
    * and the XML annotation. The parameter produced contains all 
    * information related to the constructor parameter. It knows the 
    * name of the XML entity, as well as the type. 
    * 
    * @param factory this is the constructor the parameter exists in
    * @param label represents the XML annotation for the contact
    * @param index the index of the parameter in the constructor
    * 
    * @return returns the parameter instantiated for the field
    */
   public Parameter getInstance(Constructor factory, Annotation label, int index) throws Exception {   
      return getInstance(factory, label, null, index);
   }
   
   /**
    * Creates a <code>Parameter</code> using the provided constructor
    * and the XML annotation. The parameter produced contains all 
    * information related to the constructor parameter. It knows the 
    * name of the XML entity, as well as the type. 
    * 
    * @param factory this is the constructor the parameter exists in
    * @param label represents the XML annotation for the contact
    * @param entry this is the entry annotation for the parameter
    * @param index the index of the parameter in the constructor
    * 
    * @return returns the parameter instantiated for the field
    */
   public Parameter getInstance(Constructor factory, Annotation label, Annotation entry, int index) throws Exception {   
      Constructor builder = getConstructor(label);    
      
      if(entry != null) {
         return (Parameter)builder.newInstance(factory, label, entry, format, index);
      }
      return (Parameter)builder.newInstance(factory, label, format, index);
   }
    
    /**
     * Creates a constructor that is used to instantiate the parameter
     * used to represent the specified annotation. The constructor
     * created by this method takes three arguments, a constructor, 
     * an annotation, and the parameter index.
     * 
     * @param label the XML annotation representing the label
     * 
     * @return returns a constructor for instantiating the parameter 
     * 
     * @throws Exception thrown if the annotation is not supported
     */
    private Constructor getConstructor(Annotation label) throws Exception {
      ParameterBuilder builder = getBuilder(label);
      Constructor factory = builder.getConstructor();
      
      if(!factory.isAccessible()) {
         factory.setAccessible(true);
      }
      return factory;
    }
    
    /**
     * Creates an entry that is used to select the constructor for the
     * parameter. Each parameter must implement a constructor that takes 
     * a constructor, and annotation, and the index of the parameter. If
     * the annotation is not know this method throws an exception.
     * 
     * @param label the XML annotation used to create the parameter
     * 
     * @return this returns the entry used to create a constructor
     */
    private ParameterBuilder getBuilder(Annotation label) throws Exception{      
       if(label instanceof Element) {
          return new ParameterBuilder(ElementParameter.class, Element.class);
       }
       if(label instanceof ElementList) {
          return new ParameterBuilder(ElementListParameter.class, ElementList.class);
       }
       if(label instanceof ElementArray) {
          return new ParameterBuilder(ElementArrayParameter.class, ElementArray.class);               
       }
       if(label instanceof ElementMapUnion) {
          return new ParameterBuilder(ElementMapUnionParameter.class, ElementMapUnion.class, ElementMap.class);
       }
       if(label instanceof ElementListUnion) {
          return new ParameterBuilder(ElementListUnionParameter.class, ElementListUnion.class, ElementList.class);
       }
       if(label instanceof ElementUnion) {
          return new ParameterBuilder(ElementUnionParameter.class, ElementUnion.class, Element.class);
       }
       if(label instanceof ElementMap) {
          return new ParameterBuilder(ElementMapParameter.class, ElementMap.class);
       }
       if(label instanceof Attribute) {
          return new ParameterBuilder(AttributeParameter.class, Attribute.class);
       }
       if(label instanceof Text) {
          return new ParameterBuilder(TextParameter.class, Text.class);
       }
       throw new PersistenceException("Annotation %s not supported", label);
    }
    
    /**
     * The <code>ParameterBuilder<code> is used to create a constructor 
     * that can be used to instantiate the correct parameter for the 
     * XML annotation specified. The constructor requires three 
     * arguments, the constructor, the annotation, and the index.
     * 
     * @see java.lang.reflect.Constructor
     */
    private static class ParameterBuilder {
             
       /**
        * This is the entry that is used to create the parameter.
        */
       private final Class entry;
       
       /**       
        * This is the XML annotation type within the constructor.
        */
       private final Class label;
       
       /**
        * This is the parameter type that is to be instantiated.
        */
       private final Class type;
       
       /**
        * Constructor for the <code>PameterBuilder</code> object. This 
        * pairs the parameter type with the annotation argument used 
        * within the constructor. This allows constructor to be selected.
        * 
        * @param type this is the parameter type to be instantiated
        * @param label the type that is used within the constructor
        */
       public ParameterBuilder(Class type, Class label) {
          this(type, label, null);
       }
       
       /**
        * Constructor for the <code>PameterBuilder</code> object. This 
        * pairs the parameter type with the annotation argument used 
        * within the constructor. This allows constructor to be selected.
        * 
        * @param type this is the parameter type to be instantiated
        * @param label the type that is used within the constructor
        * @param entry this is the entry used to create the parameter
        */
       public ParameterBuilder(Class type, Class label, Class entry) {
          this.label = label;
          this.entry = entry;
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
        * Creates the constructor used to instantiate the parameter
        * for the XML annotation. The constructor returned will take 
        * two arguments, a contact and the XML annotation type. 
        * 
        * @param label the type that is used within the constructor
        * 
        * @return returns the constructor for the parameter object
        */
       public Constructor getConstructor(Class label) throws Exception {
          return getConstructor(Constructor.class, label, Format.class, int.class);
       }
       
       /**
        * Creates the constructor used to instantiate the parameter
        * for the XML annotation. The constructor returned will take 
        * two arguments, a contact and the XML annotation type. 
        * 
        * @param label the type that is used within the constructor
        * @param entry this is the entry used to create the parameter
        * 
        * @return returns the constructor for the parameter object
        */
       public Constructor getConstructor(Class label, Class entry) throws Exception {
          return getConstructor(Constructor.class, label, entry, Format.class, int.class);
       }
       
       /**
        * Creates the constructor used to instantiate the parameter 
        * for the XML annotation. The constructor returned will take 
        * three arguments, a constructor, an annotation and a type.
        * 
        * @param types these are the arguments for the constructor
        * 
        * @return returns the constructor for the parameter object
        */
       private Constructor getConstructor(Class... types) throws Exception {
          return type.getConstructor(types);
       }
    }
}
