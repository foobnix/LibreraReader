/*
 * PrimitiveArray.java July 2006
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

import java.lang.reflect.Array;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.stream.Position;

/**
 * The <code>PrimitiveArray</code> object is used to convert a list of
 * elements to an array of object entries. This in effect performs a 
 * serialization and deserialization of primitive elements for the
 * array object. On serialization each primitive type must be checked 
 * against the array component type so that it is serialized in a form 
 * that can be deserialized dynamically. 
 * <pre>
 *
 *    &lt;array&gt;
 *       &lt;entry&gt;example text one&lt;/entry&gt;
 *       &lt;entry&gt;example text two&lt;/entry&gt;
 *       &lt;entry&gt;example text three&lt;/entry&gt;
 *    &lt;/array&gt;
 * 
 * </pre>
 * For the above XML element list the element <code>entry</code> is
 * contained within the array. Each entry element is deserialized as 
 * a from a parent XML element, which is specified in the annotation.
 * For serialization the reverse is done, each element taken from the 
 * array is written into an element created from the parent element.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.core.Primitive
 * @see org.simpleframework.xml.ElementArray
 */ 
class PrimitiveArray implements Converter {

   /**
    * This factory is used to create an array for the contact.
    */
   private final ArrayFactory factory;

   /**
    * This performs the serialization of the primitive element.
    */ 
   private final Primitive root;
      
   /**
    * This is the name that each array element is wrapped with.
    */
   private final String parent;
   
   /**
    * This is the type of object that will be held in the list.
    */
   private final Type entry;
   
   /**
    * This represents the actual field or method for the array.
    */
   private final Type type;
   
   /**
    * Constructor for the <code>PrimitiveArray</code> object. This is
    * given the array type for the contact that is to be converted. An
    * array of the specified type is used to hold the deserialized
    * elements and will be the same length as the number of elements.
    *
    * @param context this is the context object used for serialization
    * @param type this is the actual field type from the schema
    * @param entry the entry type to be stored within the array
    * @param parent this is the name to wrap the array element with     
    */    
   public PrimitiveArray(Context context, Type type, Type entry, String parent) {
      this.factory = new ArrayFactory(context, type); 
      this.root = new Primitive(context, entry);          
      this.parent = parent;
      this.entry = entry;
      this.type = type;
   }

   /**
    * This <code>read</code> method will read the XML element list from
    * the provided node and deserialize its children as entry types.
    * This will deserialize each entry type as a primitive value. In
    * order to do this the parent string provided forms the element.
    * 
    * @param node this is the XML element that is to be deserialized
    * 
    * @return this returns the item to attach to the object contact
    */ 
   public Object read(InputNode node) throws Exception{
      Instance type = factory.getInstance(node);
      Object list = type.getInstance();
      
      if(!type.isReference()) {
         return read(node, list);
      }
      return list;
   }

   /**
    * This <code>read</code> method will read the XML element list from
    * the provided node and deserialize its children as entry types.
    * This will deserialize each entry type as a primitive value. In
    * order to do this the parent string provided forms the element.
    * 
    * @param node this is the XML element that is to be deserialized
    * @param list this is the array to read the array values in to
    * 
    * @return this returns the item to attach to the object contact
    */ 
   public Object read(InputNode node, Object list) throws Exception{
      int length = Array.getLength(list);

      for(int pos = 0; true; pos++) {
         Position line = node.getPosition();
         InputNode next = node.getNext();
    
         if(next == null) {
            return list;            
         }
         if(pos >= length){
            throw new ElementException("Array length missing or incorrect for %s at %s", type, line);
         }
         Array.set(list, pos, root.read(next));
      } 
   }    
   
   /**
    * This <code>validate</code> method will validate the XML element list 
    * from the provided node and validate its children as entry types.
    * This will validate each entry type as a primitive value. In order 
    * to do this the parent string provided forms the element.
    * 
    * @param node this is the XML element that is to be validated
    * 
    * @return true if the element matches the XML schema class given 
    */ 
   public boolean validate(InputNode node) throws Exception{
      Instance value = factory.getInstance(node);
      
      if(!value.isReference()) {
         Object result = value.setInstance(null);
         Class expect = value.getType();
            
         return validate(node, expect);
      }
      return true; 
   }

   /**
    * This <code>validate</code> method will validate the XML element list 
    * from the provided node and validate its children as entry types.
    * This will validate each entry type as a primitive value. In order 
    * to do this the parent string provided forms the element.
    * 
    * @param node this is the XML element that is to be validated
    * @param type this is the array type used to create the array
    * 
    * @return true if the element matches the XML schema class given 
    */ 
   private boolean validate(InputNode node, Class type) throws Exception{
      while(true) {
         InputNode next = node.getNext();
    
         if(next == null) {
            return true;            
         }
         root.validate(next);
      } 
   }    

   /**
    * This <code>write</code> method will write the specified object
    * to the given XML element as as array entries. Each entry within
    * the given array must be assignable to the array component type.
    * This will deserialize each entry type as a primitive value. In
    * order to do this the parent string provided forms the element.
    * 
    * @param source this is the source object array to be serialized 
    * @param node this is the XML element container to be populated
    */ 
   public void write(OutputNode node, Object source) throws Exception {
      int size = Array.getLength(source);
      
      for(int i = 0; i < size; i++) {
         OutputNode child = node.getChild(parent);
         
         if(child == null) {
            break;
         }
         write(child, source, i);
      }
   }   

   /**
    * This <code>write</code> method will write the specified object
    * to the given XML element as as array entries. Each entry within
    * the given array must be assignable to the array component type.
    * This will deserialize each entry type as a primitive value. In
    * order to do this the parent string provided forms the element.
    * 
    * @param source this is the source object array to be serialized 
    * @param node this is the XML element container to be populated
    * @param index this is the position in the array to set the item
    */ 
   private void write(OutputNode node, Object source, int index) throws Exception {   
      Object item = Array.get(source, index);         
      
      if(item != null) {         
         if(!isOverridden(node, item)) {
            root.write(node, item);
         }
      }      
   }
   
   /**
    * This is used to determine whether the specified value has been
    * overridden by the strategy. If the item has been overridden
    * then no more serialization is require for that value, this is
    * effectively telling the serialization process to stop writing.
    * 
    * @param node the node that a potential override is written to
    * @param value this is the object instance to be serialized
    * 
    * @return returns true if the strategy overrides the object
    */
   private boolean isOverridden(OutputNode node, Object value) throws Exception{
      return factory.setOverride(entry, value, node);
   }
}
