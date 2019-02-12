/*
 * CompositeList.java July 2006
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
 * The <code>CompositeList</code> object is used to convert an element
 * list to a collection of element entries. This in effect performs a 
 * root serialization and deserialization of entry elements for the
 * collection object. On serialization each objects type must be 
 * checked against the XML annotation entry so that it is serialized
 * in a form that can be deserialized. 
 * <pre>
 *
 *    &lt;list&gt;
 *       &lt;entry attribute="value"&gt;
 *          &lt;text&gt;example text value&lt;/text&gt;
 *       &lt;/entry&gt;
 *       &lt;entry attribute="demo"&gt;
 *          &lt;text&gt;some other example&lt;/text&gt;
 *       &lt;/entry&gt;
 *    &lt;/list&gt;
 * 
 * </pre>
 * For the above XML element list the element <code>entry</code> is
 * contained within the list. Each entry element is thus deserialized
 * as a root element and then inserted into the list. This enables
 * lists to be composed from XML documents. For serialization the
 * reverse is done, each element taken from the collection is written
 * as a root element to the owning element to create the list. 
 * Entry objects do not need to be of the same type.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.xml.core.Traverser
 * @see org.simpleframework.xml.ElementList
 */ 
class CompositeList implements Converter {

   /**
    * This factory is used to create a suitable collection list.
    */         
   private final CollectionFactory factory;

   /**
    * This performs the traversal used for object serialization.
    */ 
   private final Traverser root;
   
   /**
    * This represents the name of the entry elements to write.
    */
   private final String name;
   
   /**
    * This is the entry type for elements within the list.
    */   
   private final Type entry;
   
   /**
    * This is the field or method that has been annotated.
    */
   private final Type type;

   /**
    * Constructor for the <code>CompositeList</code> object. This is
    * given the list type and entry type to be used. The list type is
    * the <code>Collection</code> implementation that deserialized
    * entry objects are inserted into. 
    *
    * @param context this is the context object used for serialization
    * @param type this is the collection type for the list used
    * @param entry the entry type to be stored within the list
    */    
   public CompositeList(Context context, Type type, Type entry, String name) {
      this.factory = new CollectionFactory(context, type); 
      this.root = new Traverser(context);      
      this.entry = entry;
      this.type = type;
      this.name = name;
   }

   /**
    * This <code>read</code> method will read the XML element list from
    * the provided node and deserialize its children as entry types.
    * This will each entry type is deserialized as a root type, that 
    * is, its <code>Root</code> annotation must be present and the
    * name of the entry element must match that root element name.
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
    * This will each entry type is deserialized as a root type, that 
    * is, its <code>Root</code> annotation must be present and the
    * name of the entry element must match that root element name.
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
         Class expect = entry.getType();
        
         if(next == null) {
            return list;
         }
         list.add(root.read(next, expect));
      }
   }      
   
   /**
    * This <code>validate</code> method will validate the XML element 
    * list from the provided node and deserialize its children as entry 
    * types. This takes each entry type and validates it as a root type, 
    * that is, its <code>Root</code> annotation must be present and the
    * name of the entry element must match that root element name.
    * 
    * @param node this is the XML element that is to be validated
    * 
    * @return true if the element matches the XML schema class given 
    */ 
   public boolean validate(InputNode node) throws Exception{
      Instance value = factory.getInstance(node);
      
      if(!value.isReference()) {
         Object result = value.setInstance(null);
         Class type = value.getType();
            
         return validate(node, type);
      }
      return true; 
   }
   
   /**
    * This <code>validate</code> method will validate the XML element 
    * list from the provided node and deserialize its children as entry 
    * types. This takes each entry type and validates it as a root type, 
    * that is, its <code>Root</code> annotation must be present and the
    * name of the entry element must match that root element name.
    * 
    * @param node this is the XML element that is to be validated
    * @param type this is the type to validate against the input node
    * 
    * @return true if the element matches the XML schema class given 
    */ 
   private boolean validate(InputNode node, Class type) throws Exception {
      while(true) {
         InputNode next = node.getNext();
         Class expect = entry.getType();
         
         if(next == null) {
            return true;
         }
         root.validate(next, expect);
      }
   }     

   /**
    * This <code>write</code> method will write the specified object
    * to the given XML element as as list entries. Each entry within
    * the given collection must be assignable from the annotated 
    * type specified within the <code>ElementList</code> annotation.
    * Each entry is serialized as a root element, that is, its
    * <code>Root</code> annotation is used to extract the name. 
    * 
    * @param source this is the source collection to be serialized 
    * @param node this is the XML element container to be populated
    */ 
   public void write(OutputNode node, Object source) throws Exception {
      Collection list = (Collection) source;                
      
      for(Object item : list) {
         if(item != null) {
            Class expect = entry.getType();
            Class actual = item.getClass();

            if(!expect.isAssignableFrom(actual)) {
               throw new PersistenceException("Entry %s does not match %s for %s", actual, entry, type);                     
            }
            root.write(node, item, expect, name);
         }
      }
   }
}
