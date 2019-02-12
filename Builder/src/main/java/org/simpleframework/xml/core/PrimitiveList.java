/*
 * PrimitiveList.java July 2006
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

import java.util.Collection;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * The <code>PrimitiveList</code> object is used to convert an element
 * list to a collection of element entries. This in effect performs a 
 * serialization and deserialization of primitive entry elements for 
 * the collection object. On serialization each objects type must be 
 * checked against the XML annotation entry so that it is serialized
 * in a form that can be deserialized. 
 * <pre>
 *
 *    &lt;list&gt;
 *       &lt;entry&gt;example one&lt;/entry&gt;
 *       &lt;entry&gt;example two&lt;/entry&gt;
 *       &lt;entry&gt;example three&lt;/entry&gt;
 *       &lt;entry&gt;example four&lt;/entry&gt;
 *    &lt;/list&gt;
 * 
 * </pre>
 * For the above XML element list the element <code>entry</code> is
 * used to wrap the primitive string value. This wrapping XML element 
 * is configurable and defaults to the lower case string for the name
 * of the class it represents. So, for example, if the primitive type
 * is an <code>int</code> the enclosing element will be called int.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.core.Primitive
 * @see org.simpleframework.xml.ElementList
 */ 
class PrimitiveList implements Converter {

   /**
    * This factory is used to create a suitable collection list.
    */         
   private final CollectionFactory factory;

   /**
    * This performs the serialization of the primitive element.
    */ 
   private final Primitive root;
      
   /**
    * This is the name that each array element is wrapped with.
    */
   private final String parent;
   
   /**
    * This is the type of object that will be held within the list.
    */
   private final Type entry;
                 
   /**
    * Constructor for the <code>PrimitiveList</code> object. This is
    * given the list type and entry type to be used. The list type is
    * the <code>Collection</code> implementation that deserialized
    * entry objects are inserted into. 
    * 
    * @param context this is the context object used for serialization
    * @param type this is the collection type for the list used
    * @param entry the primitive type to be stored within the list
    * @param parent this is the name to wrap the list element with 
    */    
   public PrimitiveList(Context context, Type type, Type entry, String parent) {
      this.factory = new CollectionFactory(context, type); 
      this.root = new Primitive(context, entry);          
      this.parent = parent;
      this.entry = entry;
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
         return populate(node, list);
      }
      return list;
   }
   
   /**
    * This <code>read</code> method will read the XML element map from
    * the provided node and deserialize its children as entry types.
    * Each entry type must contain a key and value so that the entry 
    * can be inserted in to the map as a pair. If either the key or 
    * value is composite it is read as a root object, which means its
    * <code>Root</code> annotation must be present and the name of the
    * object element must match that root element name.
    * 
    * @param node this is the XML element that is to be deserialized
    * @param result this is the map object that is to be populated
    * 
    * @return this returns the item to attach to the object contact
    */
   public Object read(InputNode node, Object result) throws Exception {
      Instance type = factory.getInstance(node);
      
      if(type.isReference()) {
         return type.getInstance();
      }
      type.setInstance(result);
      
      if(result != null) {
         return populate(node, result);
      }
      return result;
   }
   
   /**
    * This <code>populate</code> method wll read the XML element list 
    * from the provided node and deserialize its children as entry types.
    * This will deserialize each entry type as a primitive value. In
    * order to do this the parent string provided forms the element.
    * 
    * @param node this is the XML element that is to be deserialized
    * @param result this is the collection that is to be populated
    * 
    * @return this returns the item to attach to the object contact
    */ 
   private Object populate(InputNode node, Object result) throws Exception {
      Collection list = (Collection) result;                 
      
      while(true) {
         InputNode next = node.getNext();
        
         if(next == null) {
            return list;
         }
         list.add(root.read(next));
      }
   }
   
   /**
    * This <code>validate</code> method wll validate the XML element list 
    * from the provided node and validate its children as entry types.
    * This will validate each entry type as a primitive value. In order 
    * to do this the parent string provided forms the element.
    * 
    * @param node this is the XML element that is to be deserialized
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
    * @param node this is the XML element that is to be deserialized
    * @param type this is the type to validate against the input node
    * 
    * @return true if the element matches the XML schema class given 
    */ 
   private boolean validate(InputNode node, Class type) throws Exception {
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
    * to the given XML element as as list entries. Each entry within
    * the given list must be assignable to the given primitive type.
    * This will deserialize each entry type as a primitive value. In
    * order to do this the parent string provided forms the element.
    * 
    * @param source this is the source object array to be serialized 
    * @param node this is the XML element container to be populated
    */ 
   public void write(OutputNode node, Object source) throws Exception {
      Collection list = (Collection) source;                
      
      for(Object item : list) {
         if(item != null) {
            OutputNode child = node.getChild(parent);         

            if(!isOverridden(child, item)) { 
               root.write(child, item);
            }
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
