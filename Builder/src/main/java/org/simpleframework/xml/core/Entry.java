/*
 * Entry.java July 2007
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

import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.strategy.Type;

/**
 * The <code>Entry</code> object is used to provide configuration for
 * the serialization and deserialization of a map. Values taken from
 * the <code>ElementMap</code> annotation provide a means to specify
 * how to read and write the map as an XML element. Key and value
 * objects can be written as composite or primitive values. Primitive
 * key values can be written as attributes of the resulting entry
 * and value objects can be written inline if desired.
 * 
 * @author Niall Gallagher
 */
class Entry {
   
   /**
    * Provides the default name for entry XML elements of the map.
    */
   private static final String DEFAULT_NAME = "entry";
     
   /**
    * Represents the annotation that the map object is labeled with.
    */
   private ElementMap label;
   
   /**
    * Provides the point of contact in the object to the map.
    */
   private Contact contact;
   
   /**
    * Provides the class XML schema used for the value objects.
    */
   private Class valueType;
   
   /**
    * Provides the class XML schema used for the key objects.
    */
   private Class keyType;  
   
   /**
    * Specifies the name of the XML entry element used by the map.
    */
   private String entry;
   
   /**
    * Specifies the name of the XML value element used by the map.
    */
   private String value;
   
   /**
    * Specifies the name of the XML key node used by the map.
    */
   private String key;
   
   /**
    * Determines whether the key object is written as an attribute.
    */
   private boolean attribute;

   /**
    * Constructor for the <code>Entry</code> object. This takes the
    * element map annotation that provides configuration as to how
    * the map is serialized and deserialized from the XML document. 
    * The entry object provides a convenient means to access the XML
    * schema configuration using defaults where necessary.
    * 
    * @param contact this is the point of contact to the map object
    * @param label the annotation the map method or field uses
    */
   public Entry(Contact contact, ElementMap label) {  
      this.attribute = label.attribute();   
      this.entry = label.entry();
      this.value = label.value();
      this.key = label.key();
      this.contact = contact;
      this.label = label;
   }
   
   /**
    * This represents the field or method that has been annotated as
    * a map. This can be used to acquire information on the field or
    * method. Also, as a means of reporting errors this can be used.
    * 
    * @return this returns the contact associated with the map
    */
   public Contact getContact() {
      return contact;
   }
   
   /**
    * Represents whether the key value is to be an attribute or an
    * element. This allows the key to be embedded within the entry
    * XML element allowing for a more compact representation. Only
    * primitive key objects can be represented as an attribute. For
    * example a <code>java.util.Date</code> or a string could be
    * represented as an attribute key for the generated XML. 
    *  
    * @return true if the key is to be inlined as an attribute
    */
   public boolean isAttribute() {
      return attribute;
   }
   
   /**
    * Represents whether the value is to be written as an inline text
    * value within the element. This is only possible if the key has
    * been specified as an attribute. Also, the value can only be
    * inline if there is no wrapping value XML element specified.
    * 
    * @return this returns true if the value can be written inline
    */
   public boolean isInline() throws Exception {
      return isAttribute();
   }
   
   /**
    * This is used to get the key converter for the entry. This knows
    * whether the key type is a primitive or composite object and will
    * provide the appropriate converter implementation. This allows 
    * the root composite map converter to concern itself with only the
    * details of the surrounding entry object. 
    * 
    * @param context this is the root context for the serialization
    * 
    * @return returns the converter used for serializing the key
    */
   public Converter getKey(Context context) throws Exception {
      Type type = getKeyType();

      if(context.isPrimitive(type)) {        
         return new PrimitiveKey(context, this, type);
      }
      return new CompositeKey(context, this, type);
   }   
  
   /**
    * This is used to get the value converter for the entry. This knows
    * whether the value type is a primitive or composite object and will
    * provide the appropriate converter implementation. This allows 
    * the root composite map converter to concern itself with only the
    * details of the surrounding entry object. 
    * 
    * @param context this is the root context for the serialization
    * 
    * @return returns the converter used for serializing the value
    */
   public Converter getValue(Context context) throws Exception {
      Type type = getValueType();
      
      if(context.isPrimitive(type)) {
         return new PrimitiveValue(context, this, type);
      }
      return new CompositeValue(context, this, type);
   }

   /**
    * This is used to acquire the dependent key for the annotated
    * map. This will simply return the type that the map object is
    * composed to hold. This must be a serializable type, that is,
    * it must be a composite or supported primitive type.
    * 
    * @return this returns the key object type for the map object
    */
   protected Type getKeyType() throws Exception  {
      if(keyType == null) {
         keyType = label.keyType();
         
         if(keyType == void.class) {
            keyType = getDependent(0);
         }
      }
      return new ClassType(keyType);
   }
   
   /**
    * This is used to acquire the dependent value for the annotated
    * map. This will simply return the type that the map object is
    * composed to hold. This must be a serializable type, that is,
    * it must be a composite or supported primitive type.
    * 
    * @return this returns the value object type for the map object
    */
   protected Type getValueType() throws Exception {
      if(valueType == null) {
         valueType = label.valueType();
         
         if(valueType == void.class) {
            valueType = getDependent(1);
         }
      }
      return new ClassType(valueType);
   }
   
   /**
    * Provides the dependent class for the map as taken from the 
    * specified index. This allows the entry to fall back on generic
    * declarations of the map if no explicit dependent types are 
    * given within the element map annotation.
    * 
    * @param index this is the index to acquire the parameter from
    * 
    * @return this returns the generic type at the specified index
    */
   private Class getDependent(int index) throws Exception {
      Class[] list = contact.getDependents();
      
      if(list.length < index) {
         return Object.class;
      }
      if(list.length == 0) {
         return Object.class;
      }
      return list[index];
   }
   
   /**
    * This is used to provide a key XML element for each of the
    * keys within the map. This essentially wraps the entity to
    * be serialized such that there is an extra XML element present.
    * This can be used to override the default names of primitive
    * keys, however it can also be used to wrap composite keys. 
    * 
    * @return this returns the key XML element for each key
    */
   public String getKey() throws Exception {
      if(key == null) {
         return key;
      }    
      if(isEmpty(key)) {
         key = null;
      }      
      return key;
   } 
   
   /**
    * This is used to provide a value XML element for each of the
    * values within the map. This essentially wraps the entity to
    * be serialized such that there is an extra XML element present.
    * This can be used to override the default names of primitive
    * values, however it can also be used to wrap composite values. 
    * 
    * @return this returns the value XML element for each value
    */
   public String getValue() throws Exception {
      if(value == null) {
         return value;
      }
      if(isEmpty(value)) {
         value = null;
      }
      return value;
   }
   
   /**
    * This is used to provide a the name of the entry XML element 
    * that wraps the key and value elements. If specified the entry
    * value specified will be used instead of the default name of 
    * the element. This is used to ensure the resulting XML is 
    * configurable to the requirements of the generated XML. 
    * 
    * @return this returns the entry XML element for each entry
    */
   public String getEntry() throws Exception {
      if(entry == null) {
         return entry;
      }
      if(isEmpty(entry)) {
         entry = DEFAULT_NAME;
      }      
      return entry;
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
    * This provides a textual representation of the annotated field 
    * or method for the map. Providing a textual representation allows
    * exception messages to be reported with sufficient information.
    * 
    * @return this returns the textual representation of the label
    */
   public String toString() {
      return String.format("%s on %s", label, contact);
   }
}
