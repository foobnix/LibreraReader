/*
 * Context.java July 2006
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

import org.simpleframework.xml.Version;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Value;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>Context</code> object acts as a contextual object that is
 * used to store all information regarding an instance of serialization
 * or deserialization. This maintains the <code>Strategy</code> as
 * well as the <code>Filter</code> used to replace template variables.
 * When serialization and deserialization are performed the source is
 * required as it acts as a factory for objects used in the process.
 * <p>
 * For serialization the source object is required as a factory for
 * <code>Schema</code> objects, which are used to visit each field 
 * in the class that can be serialized. Also this can be used to get
 * any data entered into the session <code>Map</code> object.
 * <p>
 * When deserializing the source object provides the contextual data
 * used to replace template variables extracted from the XML source.
 * This is performed using the <code>Filter</code> object. Also, as 
 * in serialization it acts as a factory for the <code>Schema</code> 
 * objects used to examine the serializable fields of an object.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.strategy.Strategy
 */ 
interface Context {
   
   /**
    * This is used to determine if the deserialization mode is strict
    * or not. If this is not strict then deserialization will be done
    * in such a way that additional elements and attributes can be
    * ignored. This allows external XML formats to be used without 
    * having to match the object structure to the XML fully.
    * 
    * @return this returns true if the deserialization is strict
    */
   boolean isStrict();
   
   /**
    * This is used to acquire the <code>Style</code> for the format.
    * If no style has been set a default style is used, which does 
    * not modify the attributes and elements that are used to build
    * the resulting XML document.
    * 
    * @return this returns the style used for this format object
    */
   Style getStyle();
   
   /**
    * This is used to acquire the <code>Session</code> object that 
    * is used to store the values used within the serialization
    * process. This provides the internal map that is passed to all
    * of the call back methods so that is can be populated.
    * 
    * @return this returns the session that is used by this source
    */
   Session getSession();
   
   /**
    * This is used to acquire the <code>Support</code> object.
    * The support object is used to translate strings to and from
    * their object representations and is also used to convert the
    * strings to their template values. This is the single source 
    * of translation for all of the strings encountered.
    * 
    * @return this returns the support used by the context
    */
   Support getSupport();
   
   /**
    * This is used to determine if the type specified is a floating
    * point type. Types that are floating point are the double and
    * float primitives as well as the java types for this primitives.
    * 
    * @param type this is the type to determine if it is a float
    * 
    * @return this returns true if the type is a floating point
    */
   boolean isFloat(Class type) throws Exception;
   
   /**
    * This is used to determine if the type specified is a floating
    * point type. Types that are floating point are the double and
    * float primitives as well as the java types for this primitives.
    * 
    * @param type this is the type to determine if it is a float
    * 
    * @return this returns true if the type is a floating point
    */
   boolean isFloat(Type type) throws Exception;
   
   /**
    * This is used to determine whether the scanned class represents
    * a primitive type. A primitive type is a type that contains no
    * XML annotations and so cannot be serialized with an XML form.
    * Instead primitives a serialized using transformations.
    *
    * @param type this is the type to determine if it is primitive
    * 
    * @return this returns true if no XML annotations were found
    */
   boolean isPrimitive(Class type) throws Exception;  
   
   /**
    * This is used to determine whether the scanned type represents
    * a primitive type. A primitive type is a type that contains no
    * XML annotations and so cannot be serialized with an XML form.
    * Instead primitives a serialized using transformations.
    *
    * @param type this is the type to determine if it is primitive
    * 
    * @return this returns true if no XML annotations were found
    */
   boolean isPrimitive(Type type) throws Exception;   
   
   /**
    * This will create an <code>Instance</code> that can be used
    * to instantiate objects of the specified class. This leverages
    * an internal constructor cache to ensure creation is quicker.
    * 
    * @param value this contains information on the object instance
    * 
    * @return this will return an object for instantiating objects
    */
   Instance getInstance(Value value);

   /**
    * This will create an <code>Instance</code> that can be used
    * to instantiate objects of the specified class. This leverages
    * an internal constructor cache to ensure creation is quicker.
    * 
    * @param type this is the type that is to be instantiated
    * 
    * @return this will return an object for instantiating objects
    */
   Instance getInstance(Class type);
   
   /**
    * This is used to acquire the name of the specified type using
    * the <code>Root</code> annotation for the class. This will 
    * use either the name explicitly provided by the annotation or
    * it will use the name of the class that the annotation was
    * placed on if there is no explicit name for the root.
    * 
    * @param type this is the type to acquire the root name for
    * 
    * @return this returns the name of the type from the root
    * 
    * @throws Exception if the class contains an illegal schema
    */
   String getName(Class type) throws Exception;
   
   /**
    * This is used to acquire the <code>Caller</code> object. This
    * is used to call the callback methods within the object. If the
    * object contains no callback methods then this will return an
    * object that does not invoke any methods that are invoked. 
    * 
    * @param type this is the type to acquire the caller for
    * 
    * @return this returns the caller for the specified type
    */
   Caller getCaller(Class type) throws Exception;
   
   /**
    * This returns the version for the type specified. The version is
    * used to determine how the deserialization process is performed.
    * If the version of the type is different from the version for
    * the XML document, then deserialization is done in a best effort.
    * 
    * @param type this is the type to acquire the version for
    * 
    * @return the version that has been set for this XML schema class
    */
   Version getVersion(Class type) throws Exception;
   
   /**
    * This will acquire the <code>Decorator</code> for the type.
    * A decorator is an object that adds various details to the
    * node without changing the overall structure of the node. For
    * example comments and namespaces can be added to the node with
    * a decorator as they do not affect the deserialization.
    * 
    * @param type this is the type to acquire the decorator for 
    *
    * @return this returns the decorator associated with this
    */
   Decorator getDecorator(Class type) throws Exception;

   /**
    * This creates a <code>Schema</code> object that can be used to
    * examine the fields within the XML class schema. The schema
    * maintains information when a field from within the schema is
    * visited, this allows the serialization and deserialization
    * process to determine if all required XML annotations are used.
    * 
    * @param type the schema class the schema is created for
    * 
    * @return a new schema that can track visits within the schema
    * 
    * @throws Exception if the class contains an illegal schema 
    */   
   Schema getSchema(Class type) throws Exception;
   
   /**
    * This is used to resolve and load a class for the given element.
    * The class should be of the same type or a subclass of the class
    * specified. It can be resolved using the details within the
    * provided XML element, if the details used do not represent any
    * serializable values they should be removed so as not to disrupt
    * the deserialization process. For example the default strategy
    * removes all "class" attributes from the given elements.
    * 
    * @param type this is the type of the root element expected
    * @param node this is the element used to resolve an override
    * 
    * @return returns the type that should be used for the object
    * 
    * @throws Exception thrown if the class cannot be resolved  
    */
   Value getOverride(Type type, InputNode node) throws Exception;

   /**    
    * This is used to attach elements or attributes to the given 
    * element during the serialization process. This method allows
    * the strategy to augment the XML document so that it can be
    * deserialized using a similar strategy. For example the 
    * default strategy adds a "class" attribute to the element.
    * 
    * @param type this is the field type for the associated value 
    * @param value this is the instance variable being serialized
    * @param node this is the element used to represent the value
    * 
    * @return this returns true if serialization has complete
    * 
    * @throws Exception thrown if the details cannot be set
    */
   boolean setOverride(Type type, Object value, OutputNode node) throws Exception;
   
   /**
    * This is used to determine the type of an object given the 
    * source instance. To provide a best match approach this will
    * first attempt to get the value for the actual instance, if
    * however the instance is null the type is delegated to.
    * 
    * @param type this is the type used in the serialization
    * @param value this is the source instance being used
    * 
    * @return the best match given the criteria
    */
   Class getType(Type type, Object value);

   /**
    * This is used to acquire the attribute mapped to the specified
    * key. In order for this to return a value it must have been
    * previously placed into the context as it is empty by default.
    * 
    * @param key this is the name of the attribute to retrieve
    *
    * @return this returns the value mapped to the specified key
    */     
   Object getAttribute(Object key);
   
   /**
    * Replaces any template variables within the provided string. 
    * This is used in the deserialization process to replace 
    * variables with system properties, environment variables, or
    * used specified mappings. If a template variable does not have
    * a mapping from the <code>Filter</code> is is left unchanged.  
    * 
    * @param text this is processed by the template engine object
    * 
    * @return this returns the text will all variables replaced
    */
   String getProperty(String text);
}
