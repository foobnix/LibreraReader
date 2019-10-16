/*
 * CompositeInlineList.java July 2006
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
 * The <code>CompositeInlineList</code> object is used to convert an 
 * group of elements in to a collection of element entries. This is
 * used when a containing element for a list is not required. It 
 * extracts the elements by matching elements to name of the type
 * that the annotated field or method requires. This enables these
 * element entries to exist as siblings to other objects within the
 * object.  One restriction is that the <code>Root</code> annotation
 * for each of the types within the list must be the same.
 * <pre> 
 *    
 *    &lt;entry attribute="one"&gt;
 *       &lt;text&gt;example text value&lt;/text&gt;
 *    &lt;/entry&gt;
 *    &lt;entry attribute="two"&gt;
 *       &lt;text&gt;some other example&lt;/text&gt;
 *    &lt;/entry&gt;  
 *    &lt;entry attribute="three"&gt;
 *       &lt;text&gt;yet another example&lt;/text&gt;
 *    &lt;/entry&gt;      
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
class CompositeInlineList implements Repeater {

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
    * This represents the actual method or field for the list.
    */
   private final Type type;

   /**
    * Constructor for the <code>CompositeInlineList</code> object. 
    * This is given the list type and entry type to be used. The list
    * type is the <code>Collection</code> implementation that is used 
    * to collect the deserialized entry objects from the XML source. 
    *
    * @param context this is the context object used for serialization
    * @param type this is the collection type for the list used
    * @param entry the entry type to be stored within the list
    * @param name this is the name of the entries used for this list
    */    
   public CompositeInlineList(Context context, Type type, Type entry, String name) {
      this.factory = new CollectionFactory(context, type);
      this.root = new Traverser(context);
      this.entry = entry;
      this.type = type;
      this.name = name;
   }

   /**
    * This <code>read</code> method wll read the XML element list from
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
      Object value = factory.getInstance();
      Collection list = (Collection) value;
      
      if(list != null) {
         return read(node, list);
      }
      return null;
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
   public Object read(InputNode node, Object value) throws Exception {
      Collection list = (Collection) value;
      
      if(list != null) {
         return read(node, list);
      }
      return read(node);
   }
   
   /**
    * This <code>read</code> method wll read the XML element list from
    * the provided node and deserialize its children as entry types.
    * This will each entry type is deserialized as a root type, that 
    * is, its <code>Root</code> annotation must be present and the
    * name of the entry element must match that root element name.
    * 
    * @param node this is the XML element that is to be deserialized
    * @param list this is the collection that is to be populated
    * 
    * @return this returns the item to attach to the object contact
    */ 
   private Object read(InputNode node, Collection list) throws Exception {              
      InputNode from = node.getParent();
      String name = node.getName();
      
      while(node != null) {
         Class type = entry.getType();
         Object item = read(node, type);
         
         if(item != null) {
            list.add(item);
         }      
         node = from.getNext(name);
      }
      return list;
   }     
   
   /**
    * This <code>read</code> method will read the XML element from the     
    * provided node. This checks to ensure that the deserialized type
    * is the same as the entry type provided. If the types are not 
    * the same then an exception is thrown. This is done to ensure
    * each node in the collection contain the same root annotation. 
    * 
    * @param node this is the XML element that is to be deserialized
    * @param expect this is the type expected of the deserialized type
    *      
    * @return this returns the item to attach to the object contact
    */ 
   private Object read(InputNode node, Class expect) throws Exception {
      Object item = root.read(node, expect);
      Class result = item.getClass();
      Class actual = entry.getType();
      
      if(!actual.isAssignableFrom(result)) {
         throw new PersistenceException("Entry %s does not match %s for %s", result, entry, type);
      }
      return item;      
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
   public boolean validate(InputNode node) throws Exception{
      InputNode from = node.getParent();
      Class type = entry.getType();
      String name = node.getName();
      
      while(node != null) {
         boolean valid = root.validate(node, type);
     
         if(valid == false) {
            return false;
         }
         node = from.getNext(name);
      }  
      return true;
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
      OutputNode parent = node.getParent();      
      
      if(!node.isCommitted()) {
         node.remove();
      }
      write(parent, list);
   }
   
   /**
    * This <code>write</code> method will write the specified object
    * to the given XML element as as list entries. Each entry within
    * the given collection must be assignable from the annotated 
    * type specified within the <code>ElementList</code> annotation.
    * Each entry is serialized as a root element, that is, its
    * <code>Root</code> annotation is used to extract the name. 
    * 
    * @param list this is the source collection to be serialized 
    * @param node this is the XML element container to be populated
    */ 
   public void write(OutputNode node, Collection list) throws Exception {  
      for(Object item : list) {
         if(item != null) {
            Class expect = entry.getType();
            Class actual = item.getClass();

            if(!expect.isAssignableFrom(actual)) {
               throw new PersistenceException("Entry %s does not match %s for %s", actual, expect, type);
            }
            root.write(node, item, expect, name);
         }
      }
   }
}
