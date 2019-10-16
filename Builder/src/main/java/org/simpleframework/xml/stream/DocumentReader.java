/*
 * DocumentReader.java January 2010
 *
 * Copyright (C) 2010, Niall Gallagher <niallg@users.sf.net>
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

import static org.w3c.dom.Node.ELEMENT_NODE;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * The <code>DocumentReader</code> object provides an implementation
 * for reading XML events using DOM. This reader flattens a document
 * in to a series of nodes, and provides these nodes as events as
 * they are encountered. Essentially what this does is adapt the 
 * document approach to navigating the XML and provides a streaming
 * approach. Having an implementation based on DOM ensures that the
 * library can be used on a wider variety of platforms. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.stream.DocumentProvider
 */
class DocumentReader implements EventReader {
   
   /**
    * Any attribute beginning with this string has been reserved.
    */
   private static final String RESERVED = "xml";
   
   /**
    * This is used to extract the nodes from the provided document.
    */
   private NodeExtractor queue;
   
   /**
    * This is used to keep track of which elements are in context.
    */
   private NodeStack stack;
   
   /**
    * This is used to keep track of any events that were peeked.
    */
   private EventNode peek;
   
   /**
    * Constructor for the <code>DocumentReader</code> object. This
    * makes use of a DOM document to extract events and provide them
    * to the core framework. All nodes will be extracted from the
    * document and queued for extraction as they are requested. This
    * will ignore any comment nodes as they should not be considered.
    * 
    * @param document this is the document that is to be read
    */
   public DocumentReader(Document document) {
      this.queue = new NodeExtractor(document);
      this.stack = new NodeStack();
      this.stack.push(document);
   }
   
   /**
    * This is used to peek at the node from the document. This will
    * scan through the document, ignoring any comments to find the
    * next relevant XML event to acquire. Typically events will be
    * the start and end of an element, as well as any text nodes.
    * 
    * @return this returns the next event taken from the document
    */
   public EventNode peek() throws Exception {
      if(peek == null) {
         peek = next();
      }
      return peek;
   }   
   
   /**
    * This is used to take the next node from the document. This will
    * scan through the document, ignoring any comments to find the
    * next relevant XML event to acquire. Typically events will be
    * the start and end of an element, as well as any text nodes.
    * 
    * @return this returns the next event taken from the document
    */
   public EventNode next() throws Exception {
      EventNode next = peek;
      
      if(next == null) {
         next = read();
      } else {
         peek = null;
      }
      return next;
   }

   /**
    * This is used to read the next node from the document. This will
    * scan through the document, ignoring any comments to find the
    * next relevant XML event to acquire. Typically events will be
    * the start and end of an element, as well as any text nodes.
    * 
    * @return this returns the next event taken from the document 
    */
   private EventNode read() throws Exception {
      Node node = queue.peek();
      
      if(node == null) {
         return end();
      }
      return read(node);
   }
   
   /**
    * This is used to read the next node from the document. This will
    * scan through the document, ignoring any comments to find the
    * next relevant XML event to acquire. Typically events will be
    * the start and end of an element, as well as any text nodes.
    * 
    * @param node this is the XML node that has been read
    * 
    * @return this returns the next event taken from the document 
    */
   private EventNode read(Node node) throws Exception {
      Node parent = node.getParentNode();
      Node top = stack.top();
      
      if(parent != top) {
         if(top != null) {
            stack.pop();
         }
         return end();
      }
      if(node != null) {
         queue.poll();
      }
      return convert(node);
   }
   
   /**
    * This is used to convert the provided node in to an event. The
    * conversion process ensures the node can be digested by the core
    * reader and used to provide an <code>InputNode</code> that can
    * be used to represent the XML elements or attributes. If the
    * provided node is not an element then it is considered text.
    * 
    * @param node the node that is to be converted to an event
    *
    * @return this returns an event created from the given node
    */
   private EventNode convert(Node node) throws Exception{
      short type = node.getNodeType();
      
      if(type == ELEMENT_NODE) {    
         if(node != null) {
            stack.push(node);
         }
         return start(node);
      }
      return text(node);
   }
   
   /**
    * This is used to convert the provided node to a start event. The
    * conversion process ensures the node can be digested by the core
    * reader and used to provide an <code>InputNode</code> that can
    * be used to represent an XML elements within the source document.
    * 
    * @param node the node that is to be converted to a start event
    *
    * @return this returns a start event created from the given node
    */
   private Start start(Node node) {
      Start event = new Start(node);

      if(event.isEmpty()) {
         return build(event);
      }
      return event;
   }
   
   /**
    * This is used to build the attributes that are to be used to 
    * populate the start event. Populating the start event with the
    * attributes it contains is required so that each element will
    * contain its associated attributes. Only attributes that are
    * not reserved will be added to the start event.
    * 
    * @param event this is the start event that is to be populated
    * 
    * @return this returns a start event with its attributes
    */
   private Start build(Start event) {
      NamedNodeMap list = event.getAttributes();
      int length = list.getLength();

      for (int i = 0; i < length; i++) {
         Node node = list.item(i);
         Attribute value = attribute(node);
         
         if(!value.isReserved()) {
            event.add(value);
         }
      }
      return event;
   }
   
   /**
    * This is used to convert the provided node to an attribute. The
    * conversion process ensures the node can be digested by the core
    * reader and used to provide an <code>InputNode</code> that can
    * be used to represent an XML attribute within the source document.
    * 
    * @param node the node that is to be converted to an attribute
    *
    * @return this returns an attribute created from the given node
    */
   private Entry attribute(Node node) {
      return new Entry(node);
   }
   
   /**
    * This is used to convert the provided node to a text event. The
    * conversion process ensures the node can be digested by the core
    * reader and used to provide an <code>InputNode</code> that can
    * be used to represent an XML attribute within the source document.
    * 
    * @param node the node that is to be converted to a text event
    *
    * @return this returns the text event created from the given node
    */
   private Text text(Node node) {
      return new Text(node);
   }
   
   /**
    * This is used to create a node event to signify that an element
    * has just ended. End events are important as they allow the core
    * reader to determine if a node is still in context. This provides
    * a more convenient way to use <code>InputNode</code> objects as
    * they should only ever be able to extract their children. 
    * 
    * @return this returns an end event to signify an element close
    */
   private End end() {
      return new End();
   }
   
   /**
    * The <code>Entry</code> object is used to represent an attribute
    * within a start element. This holds the name and value of the
    * attribute as well as the namespace prefix and reference. These
    * details can be used to represent the attribute so that should
    * the core reader require these details they can be acquired.
    * 
    * @author Niall Gallagher
    */
   private static class Entry extends EventAttribute {
      
      /**
       * This is the node that is to be represented as an attribute.
       */
      private final Node node;
      
      /**
       * Constructor for the <code>Entry</code> object. This creates
       * an attribute object that is used to extract the name, value
       * namespace prefix, and namespace reference from the provided
       * node. This is used to populate any start events created.
       * 
       * @param node this is the node that represents the attribute
       */
      public Entry(Node node) {
         this.node = node;
      }
      
      /**
       * This provides the name of the attribute. This will be the
       * name of the XML attribute without any namespace prefix. If
       * the name begins with "xml" then this attribute is reserved.
       * according to the namespaces for XML 1.0 specification.
       * 
       * @return this returns the name of this attribute object
       */
      public String getName() {
         return node.getLocalName();
      }
      
      /**
       * This returns the value of the event. This will be the value
       * that the attribute contains. If the attribute does not have
       * a value then this returns null or an empty string.
       * 
       * @return this returns the value represented by this object
       */
      public String getValue() {
         return node.getNodeValue();
      }
      
      /**
       * This is used to acquire the namespace prefix associated with
       * this attribute. A prefix is used to qualify the attribute
       * within a namespace. So, if this has a prefix then it should
       * have a reference associated with it.
       * 
       * @return this returns the namespace prefix for the attribute
       */
      public String getPrefix() {
         return node.getPrefix();
      }
      
      /**
       * This is used to acquire the namespace reference that this 
       * attribute is in. A namespace is normally associated with an
       * attribute if that attribute is prefixed with a known token.
       * If there is no prefix then this will return null.
       * 
       * @return this provides the associated namespace reference
       */
      public String getReference() {
         return node.getNamespaceURI();
      }
      
      /**
       * This returns true if the attribute is reserved. An attribute
       * is considered reserved if it begins with "xml" according to 
       * the namespaces in XML 1.0 specification. Such attributes are
       * used for namespaces and other such details.
       *
       * @return this returns true if the attribute is reserved
       */
      public boolean isReserved() {
         String prefix = getPrefix();
         String name = getName();
         
         if(prefix != null) {
            return prefix.startsWith(RESERVED);
         }
         return name.startsWith(RESERVED);
      }
      
      /**
       * This is used to return the node for the attribute. Because 
       * this represents a DOM attribute the DOM node is returned.
       * Returning the node helps with certain debugging issues.
       * 
       * @return this will return the source object for this
       */
      public Object getSource() {
         return node;
      }
   }
   
   /**
    * The <code>Start</code> object is used to represent the start of
    * an XML element. This will hold the attributes associated with
    * the element and will provide the name, the namespace reference
    * and the namespace prefix. For debugging purposes the source XML
    * element is provided for this start event.
    * 
    * @author Niall Gallagher
    */
   private static class Start extends EventElement {
      
      /**
       * This is the element that is represented by this start event.
       */
      private final Element element;
      
      /**
       * Constructor for the <code>Start</code> object. This will 
       * wrap the provided node and expose the required details such
       * as the name, namespace prefix and namespace reference. The
       * provided element node can be acquired for debugging purposes.
       * 
       * @param element this is the element being wrapped by this
       */
      public Start(Node element) {
         this.element = (Element)element;
      }
      
      /**
       * This provides the name of the event. This will be the name 
       * of an XML element the event represents. If there is a prefix
       * associated with the element, this extracts that prefix.
       * 
       * @return this returns the name without the namespace prefix
       */
      public String getName() {
         return element.getLocalName();
      }
      
      /**
       * This is used to acquire the namespace prefix associated with
       * this node. A prefix is used to qualify an XML element or
       * attribute within a namespace. So, if this represents a text
       * event then a namespace prefix is not required.
       * 
       * @return this returns the namespace prefix for this event
       */
      public String getPrefix() {
         return element.getPrefix();
      }
      
      /**
       * This is used to acquire the namespace reference that this 
       * node is in. A namespace is normally associated with an XML
       * element or attribute, so text events and element close events
       * are not required to contain any namespace references. 
       * 
       * @return this will provide the associated namespace reference
       */
      public String getReference() {
         return element.getNamespaceURI();
      }
      
      /**
       * This is used to acquire the attributes associated with the
       * element. Providing the attributes in this format allows 
       * the reader to build a list of attributes for the event.
       * 
       * @return this returns the attributes associated with this
       */
      public NamedNodeMap getAttributes(){
         return element.getAttributes();
      }
      
      /**
       * This is used to return the node for the event. Because this
       * represents a DOM element node the DOM node will be returned.
       * Returning the node helps with certain debugging issues.
       * 
       * @return this will return the source object for this event
       */
      public Object getSource() {
         return element;
      }
   }
   
   /**
    * The <code>Text</code> object is used to represent a text event.
    * If wraps a node that holds text consumed from the document. 
    * These are used by <code>InputNode</code> objects to extract the
    * text values for elements For debugging this exposes the node.
    * 
    * @author Niall Gallagher
    */
   private static class Text extends EventToken {
      
      /**
       * This is the node that is used to represent the text value.
       */
      private final Node node;
      
      /**
       * Constructor for the <code>Text</code> object. This creates
       * an event that provides text to the core reader. Text can be
       * in the form of a CDATA section or a normal text entry.
       * 
       * @param node this is the node that represents the text value
       */
      public Text(Node node) {
         this.node = node;
      } 
      
      /**
       * This is true as this event represents a text token. Text 
       * tokens are required to provide a value only. So namespace
       * details and the node name will always return null.
       *  
       * @return this returns true as this event represents text  
       */
      public boolean isText() {
         return true;
      }
      
      /**
       * This returns the value of the event. This will return the
       * text value contained within the node. If there is no
       * text within the node this should return an empty string. 
       * 
       * @return this returns the value represented by this event
       */
      public String getValue(){
         return node.getNodeValue();
      }
      
      /**
       * This is used to return the node for the event. Because this
       * represents a DOM text value the DOM node will be returned.
       * Returning the node helps with certain debugging issues.
       * 
       * @return this will return the source object for this event
       */
      public Object getSource() {
         return node;
      }
   }
   
   /**
    * The <code>End</code> object is used to represent the end of an
    * element. It is used by the core reader to determine which nodes
    * are in context and which ones are out of context. This allows
    * the input nodes to determine if it can read any more children.
    * 
    * @author Niall Gallagher
    */
   private static class End extends EventToken {
 
      /**
       * This is true as this event represents an element end. Such
       * events are required by the core reader to determine if a 
       * node is still in context. This helps to determine if there
       * are any more children to be read from a specific node.
       * 
       * @return this returns true as this token represents an end
       */
      public boolean isEnd() {
         return true;
      }
   }
}