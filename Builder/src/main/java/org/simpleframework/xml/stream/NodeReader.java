/*
 * NodeReader.java July 2006
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

package org.simpleframework.xml.stream;

/**
 * The <code>NodeReader</code> object is used to read elements from
 * the specified XML event reader. This reads input node objects
 * that represent elements within the source XML document. This will
 * allow details to be read using input node objects, as long as
 * the end elements for those input nodes have not been ended.
 * <p>
 * For example, if an input node represented the root element of a
 * document then that input node could read all elements within the
 * document. However, if the input node represented a child element
 * then it would only be able to read its children.
 *
 * @author Niall Gallagher
 */ 
class NodeReader {

   /**
    * This is used to collect the text between the element tags.
    */
   private final StringBuilder text;
   
   /**
    * Represents the XML event reader used to read all elements.
    */ 
   private final EventReader reader;     
   
   /**
    * This stack enables the reader to keep track of elements.
    */ 
   private final InputStack stack;
   
   /**
    * Constructor for the <code>NodeReader</code> object. This is used
    * to read XML events a input node objects from the event reader.
    *
    * @param reader this is the event reader for the XML document
    */ 
   public NodeReader(EventReader reader) {
      this.text = new StringBuilder();
      this.stack = new InputStack();
      this.reader = reader;            
   }        
   
   /**
    * This method is used to determine if this node is the root 
    * node for the XML document. The root node is the first node
    * in the document and has no sibling nodes. This is false
    * if the node has a parent node or a sibling node.
    * 
    * @return true if this is the root node within the document
    */
   public boolean isRoot(InputNode node) {
      return stack.bottom() == node;        
   }
   
   /**
    * Returns the root input node for the document. This is returned
    * only if no other elements have been read. Once the root element
    * has been read from the event reader this will return null.
    *
    * @return this returns the root input node for the document
    */ 
   public InputNode readRoot() throws Exception {
      if(stack.isEmpty()) {
         InputNode node = readElement(null);
         
         if(node == null) {
            throw new NodeException("Document has no root element"); 
         }
         return node;
      }
      return null;
   }
   
   /**
    * Returns the next input node from the XML document, if it is a
    * child element of the specified input node. This essentially
    * determines whether the end tag has been read for the specified
    * node, if so then null is returned. If however the specified
    * node has not had its end tag read then this returns the next
    * element, if that element is a child of the that node.
    *
    * @param from this is the input node to read with 
    *
    * @return this returns the next input node from the document
    */ 
   public InputNode readElement(InputNode from) throws Exception {
      if(!stack.isRelevant(from)) {         
         return null; 
      }
      EventNode event = reader.next();
      
      while(event != null) {
         if(event.isEnd()) {
            if(stack.pop() == from) {
               return null;
            }               
         } else if(event.isStart()) {
            return readStart(from, event);                 
         }
         event = reader.next();
      }
      return null;
   }
   
   /**
    * Returns the next input node from the XML document, if it is a
    * child element of the specified input node. This essentially
    * the same as the <code>readElement(InputNode)</code> object 
    * except that this will not read the element if it does not have
    * the name specified. This essentially acts as a peak function.
    *
    * @param from this is the input node to read with 
    * @param name this is the name expected from the next element
    *
    * @return this returns the next input node from the document
    */ 
   public InputNode readElement(InputNode from, String name) throws Exception {
      if(!stack.isRelevant(from)) {        
         return null; 
     }
     EventNode event = reader.peek();
          
     while(event != null) {
        if(event.isText()) {
           fillText(from);
        } else if(event.isEnd()) { 
           if(stack.top() == from) {
              return null;
           } else {
              stack.pop();
           }
        } else if(event.isStart()) {
           if(isName(event, name)) {
              return readElement(from);
           }   
           break;
        }
        event = reader.next();
        event = reader.peek();
     }
     return null;
   }
   
   /**
    * This is used to convert the start element to an input node.
    * This will push the created input node on to the stack. The
    * input node created contains a reference to this reader. so
    * that it can be used to read child elements and values.
    * 
    * @param from this is the parent element for the start event
    * @param event this is the start element to be wrapped
    *
    * @return this returns an input node for the given element
    */    
   private InputNode readStart(InputNode from, EventNode event) throws Exception {
      InputElement input = new InputElement(from, this, event);
       
      if(text.length() > 0) {
         text.setLength(0);
      }
      if(event.isStart()) {
         return stack.push(input);
      }
      return input;
   }

   /**
    * This is used to determine the name of the node specified. The
    * name of the node is determined to be the name of the element
    * if that element is converts to a valid StAX start element.
    * 
    * @param node this is the StAX node to acquire the name from
    * @param name this is the name of the node to check against
    * 
    * @return true if the specified node has the given local name
    */
   private boolean isName(EventNode node, String name) {
      String local = node.getName();
      
      if(local == null) {
         return false;
      }
      return local.equals(name);
   }
   
   /**
    * Read the contents of the characters between the specified XML
    * element tags, if the read is currently at that element. This 
    * allows characters associated with the element to be used. If
    * the specified node is not the current node, null is returned.
    *
    * @param from this is the input node to read the value from
    *
    * @return this returns the characters from the specified node
    */ 
   public String readValue(InputNode from) throws Exception {
      if(!stack.isRelevant(from)) { 
         return null;
      }
      int length = text.length();
      
      if(length <= 0) {
         EventNode event = reader.peek();
         
         if(event.isEnd()) { 
            if(stack.top() == from) {
               return null;
            } else {
               stack.pop();
            }
            event = reader.next();
         }
      }
      return readText(from);
   } 
   
   /**
    * Read the contents of the characters between the specified XML
    * element tags, if the read is currently at that element. This 
    * allows characters associated with the element to be used. If
    * the specified node is not the current node, null is returned.
    *
    * @param from this is the input node to read the value from
    *
    * @return this returns the characters from the specified node
    */ 
   private String readText(InputNode from) throws Exception {
      EventNode event = reader.peek();
      
      while(stack.top() == from) {   
         if(event.isText()) {
            fillText(from);
         } else {
            break;
         }
         event = reader.next();
         event = reader.peek();
      }
      return readBuffer(from);
   }
   
   /**
    * This is used to read the text between element tags. If there
    * is any text held in the buffer then this will return that
    * text and clear the buffer. Clearing the buffer in this
    * way means that the text can only ever be read once.
    * 
    * @param from this is the node to read the text from
    * 
    * @return this returns the string within the buffer if any
    */
   private String readBuffer(InputNode from) throws Exception {
      int length = text.length();
      
      if(length > 0) {
         String value = text.toString();
         
         text.setLength(0);
         return value;
      }
      return null;
   }
   
   /**
    * Read the contents of the characters between the specified XML
    * element tags, if the read is currently at that element. This 
    * allows characters associated with the element to be used. If
    * the specified node is not the current node, null is returned.
    *
    * @param from this is the input node to read the value from
    *
    * @return this returns the characters from the specified node
    */ 
   private void fillText(InputNode from) throws Exception {      
      EventNode event = reader.peek();
      
      if(event.isText()) {
         String data = event.getValue();
         
         text.append(data); 
      }
   }  
   
   /**
    * This is used to determine if this input node is empty. An
    * empty node is one with no attributes or children. This can
    * be used to determine if a given node represents an empty
    * entity, with which no extra data can be extracted.
    * 
    * @param from this is the input node to read the value from
    * 
    * @return this returns true if the node is an empty element
    * 
    * @throws Exception thrown if there was a parse error
    */
   public boolean isEmpty(InputNode from) throws Exception {
      if(stack.top() == from) {         
         EventNode event = reader.peek();

         if(event.isEnd()) {
            return true;
         }
      }
      return false;
   }

   /**
    * This method is used to skip an element within the XML document.
    * This will simply read each element from the document until
    * the specified element is at the top of the stack. When the
    * specified element is at the top of the stack this returns.
    *
    * @param from this is the element to skip from the XML document
    */ 
   public void skipElement(InputNode from) throws Exception {
      while(readElement(from) != null);           
   }  
}

