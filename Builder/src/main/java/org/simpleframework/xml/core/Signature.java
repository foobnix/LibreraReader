/*
 * Signature.java April 2009
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

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>Signature</code> object represents a constructor
 * of parameters iterable in declaration order. This is used so
 * that parameters can be acquired by name for validation. It is
 * also used to create an array of <code>Parameter</code> objects
 * that can be used to acquire the correct deserialized values
 * to use in order to instantiate the object.
 * 
 * @author Niall Gallagher
 */
class Signature implements Iterable<Parameter> {
   
   /**
    * This is the map of parameters that this signature uses.
    */
   private final ParameterMap parameters;
   
   /**
    * This is the type that the parameters are created for.
    */
   private final Constructor factory;
   
   /**
    * This is the type that the signature was created for.
    */
   private final Class type;
   
   /**
    * Constructor for the <code>Signature</code> object. This 
    * is used to create a hash map that can be used to acquire
    * parameters by name. It also provides the parameters in
    * declaration order within a for each loop.
    * 
    * @param signature this is the signature to be copied
    */
   public Signature(Signature signature) {
      this(signature.factory, signature.type);
   }
   
   /**
    * Constructor for the <code>Signature</code> object. This 
    * is used to create a hash map that can be used to acquire
    * parameters by name. It also provides the parameters in
    * declaration order within a for each loop.
    * 
    * @param factory this is the constructor this represents
    */
   public Signature(Constructor factory) {
      this(factory, factory.getDeclaringClass());
   }
   
   /**
    * Constructor for the <code>Signature</code> object. This 
    * is used to create a hash map that can be used to acquire
    * parameters by name. It also provides the parameters in
    * declaration order within a for each loop.
    * 
    * @param factory this is the constructor this represents
    * @param type this is the type the map is created for
    */
   public Signature(Constructor factory, Class type) {
      this.parameters = new ParameterMap();
      this.factory = factory;
      this.type = type;
   }
   
   /**
    * This represents the number of parameters this signature has.
    * A signature with no parameters is the default no argument
    * constructor, anything else is a candidate for injection.
    * 
    * @return this returns the number of annotated parameters
    */
   public int size() {
      return parameters.size();
   }
   
   /**
    * This is used to determine if there are any parameters in the
    * signature. If the signature contains no parameters then this
    * will return true, if it does then this returns false.
    * 
    * @return this returns true of the signature has no parameters
    */
   public boolean isEmpty() {
      return parameters.isEmpty();
   }
   
   /**
    * This returns true if the signature contains a parameter that
    * is mapped to the specified key. If no parameter exists with
    * this key then this will return false.
    * 
    * @param key this is the key the parameter is mapped to
    * 
    * @return this returns true if there is a parameter mapping
    */
   public boolean contains(Object key) {
      return parameters.containsKey(key);
   }
   
   /**
    * This is used to iterate over <code>Parameter</code> objects.
    * Parameters are iterated in the order that they are added to
    * the map. This is primarily used for convenience iteration. 
    * 
    * @return this returns an iterator for the parameters
    */
   public Iterator<Parameter> iterator() {
      return parameters.iterator();
   }
   
   /**
    * This is used to remove a parameter from the signature. This 
    * returns any parameter removed if it exists, if not then this
    * returns null. This is used when performing matching.
    * 
    * @param key this is the key of the parameter to remove
    * 
    * @return this returns the parameter that was removed
    */
   public Parameter remove(Object key) {
      return parameters.remove(key);
   }
   
   /**
    * This is used to acquire a <code>Parameter</code> using the
    * position of that parameter within the constructor. This 
    * allows a builder to determine which parameters to use.
    * 
    * @param ordinal this is the position of the parameter
    * 
    * @return this returns the parameter for the position
    */
   public Parameter get(int ordinal) {
      return parameters.get(ordinal);
   }
   
   /**
    * This is used to acquire the parameter based on its name. This
    * is used for convenience when the parameter name needs to be
    * matched up with an annotated field or method.
    * 
    * @param key this is the key of the parameter to acquire
    * 
    * @return this is the parameter mapped to the given name
    */
   public Parameter get(Object key) {
      return parameters.get(key);
   }
    
   /**
    * This is used to acquire an list of <code>Parameter</code>
    * objects in declaration order. This list will help with the
    * resolution of the correct constructor for deserialization
    * of the XML. It also provides a faster method of iteration.
    * 
    * @return this returns the parameters in declaration order
    */
   public List<Parameter> getAll() {
      return parameters.getAll();
   }
   
   /**
    * This will add the provided parameter to the signature. The
    * parameter is added to the signature mapped to the key of
    * the parameter. If the key is null it is not added.
    * 
    * @param parameter this is the parameter to be added
    */
   public void add(Parameter parameter) {
      Object key = parameter.getKey();
      
      if(key != null) {
         parameters.put(key, parameter);
      }
   }
   
   /**
    * This will add a new mapping to the signature based on the
    * provided key. Adding a mapping to a parameter using something
    * other than the key for the parameter allows for resolution
    * of the parameter based on a path or a name if desired.
    * 
    * @param key this is the key to map the parameter to
    * 
    * @param parameter this is the parameter to be mapped
    */
   public void set(Object key, Parameter parameter) {
      parameters.put(key, parameter);
   }
   
   /**
    * This is used to instantiate the object using the default no
    * argument constructor. If for some reason the object can not be
    * instantiated then this will throw an exception with the reason.    
    * 
    * @return this returns the object that has been instantiated
    */
   public Object create() throws Exception {
      if(!factory.isAccessible()) {
         factory.setAccessible(true);
      } 
      return factory.newInstance();
   }
   
   /**
    * This is used to instantiate the object using a constructor that
    * takes deserialized objects as arguments. The objects that have
    * been deserialized are provided in declaration order so they can
    * be passed to the constructor to instantiate the object.
    * 
    * @param list this is the list of objects used for instantiation
    * 
    * @return this returns the object that has been instantiated
    */
   public Object create(Object[] list) throws Exception {
      if(!factory.isAccessible()) {
         factory.setAccessible(true);
      } 
      return factory.newInstance(list);
   }
   
   /**
    * This is used to build a <code>Signature</code> with the given
    * context so that keys are styled. This allows serialization to
    * match styled element names or attributes to ensure that they 
    * can be used to acquire the parameters.
    * 
    * @return this returns a signature with styled keys
    */
   public Signature copy() throws Exception {
      Signature signature = new Signature(this);
      
      for(Parameter parameter : this) {  
         signature.add(parameter);
      }
      return signature;
   }
   
   /**
    * This is the type associated with the <code>Signature</code>.
    * All instances returned from this creator will be of this type.
    * 
    * @return this returns the type associated with the signature
    */
   public Class getType() {
      return type;
   }
   
   /**
    * This is used to acquire a descriptive name for the instantiator.
    * Providing a name is useful in debugging and when exceptions are
    * thrown as it describes the constructor the instantiator represents.
    * 
    * @return this returns the name of the constructor to be used
    */
   public String toString() {
      return factory.toString();
   }
}