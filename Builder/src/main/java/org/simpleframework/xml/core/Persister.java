/*
 * Persister.java July 2006
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.filter.Filter;
import org.simpleframework.xml.filter.PlatformFilter;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.strategy.TreeStrategy;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeBuilder;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.transform.Matcher;

/**
 * The <code>Persister</code> object is used to provide an implementation
 * of a serializer. This implements the <code>Serializer</code> interface
 * and enables objects to be persisted and loaded from various sources. 
 * This implementation makes use of <code>Filter</code> objects to
 * replace template variables within the source XML document. It is fully
 * thread safe and can be shared by multiple threads without concerns.
 * <p>
 * Deserialization is performed by passing an XML schema class into one
 * of the <code>read</code> methods along with the source of an XML stream.
 * The read method then reads the contents of the XML stream and builds
 * the object using annotations within the XML schema class.
 * <p>
 * Serialization is performed by passing an object and an XML stream into
 * one of the <code>write</code> methods. The serialization process will
 * use the class of the provided object as the schema class. The object
 * is traversed and all fields are marshalled to the result stream.
 *
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.Serializer
 */ 
public class Persister implements Serializer {
   
   /**
    * This is used to manage the serialization sessions created/
    */
   private final SessionManager manager;
   
   /**
    * This is the strategy object used to load and resolve classes.
    */ 
   private final Strategy strategy;

   /**
    * This support is used to convert the strings encountered.
    */
   private final Support support;
   
   /**
    * This object is used to format the the generated XML document.
    */ 
   private final Format format;
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use an empty filter.
    * This means that template variables will remain unchanged within
    * the XML document parsed when an object is deserialized.
    */
   public Persister() {
      this(new HashMap());           
   }
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided format
    * instructions. The persister uses the <code>Format</code> object
    * to structure the generated XML. It determines the indent size
    * of the document and whether it should contain a prolog.
    * 
    * @param format this is used to structure the generated XML
    */
   public Persister(Format format) {
      this(new TreeStrategy(), format);
   } 

   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use a platform filter
    * object using the overrides within the provided map. This means
    * that template variables will be replaced firstly with mappings
    * from within the provided map, followed by system properties. 
    * 
    * @param filter this is the map that contains the overrides
    */
   public Persister(Map filter) {
      this(new PlatformFilter(filter));           
   }
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use a platform filter
    * object using the overrides within the provided map. This means
    * that template variables will be replaced firstly with mappings
    * from within the provided map, followed by system properties. 
    * 
    * @param filter this is the map that contains the overrides
    * @param format this is the format used to format the documents
    */
   public Persister(Map filter, Format format) {
      this(new PlatformFilter(filter));           
   }
        
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided filter.
    * This persister will replace all variables encountered when
    * deserializing an object with mappings found in the filter.
    * 
    * @param filter the filter used to replace template variables
    */
   public Persister(Filter filter) {
      this(new TreeStrategy(), filter);
   }     

   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided filter.
    * This persister will replace all variables encountered when
    * deserializing an object with mappings found in the filter.
    * 
    * @param filter the filter used to replace template variables
    * @param format this is used to structure the generated XML
    */
   public Persister(Filter filter, Format format) {
      this(new TreeStrategy(), filter, format);
   }
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided matcher
    * for customizable transformations. The <code>Matcher</code> will
    * enable the persister to determine the correct way to transform
    * the types that are not annotated and considered primitives.
    * 
    * @param matcher this is used to customize the transformations
    */
   public Persister(Matcher matcher) {
      this(new TreeStrategy(), matcher);
   }  
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided matcher
    * for customizable transformations. The <code>Matcher</code> will
    * enable the persister to determine the correct way to transform
    * the types that are not annotated and considered primitives.
    * 
    * @param matcher this is used to customize the transformations
    * @param format this is used to structure the generated XML
    */
   public Persister(Matcher matcher, Format format) {
      this(new TreeStrategy(), matcher, format);
   }  

   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use a strategy object. 
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes
    */
   public Persister(Strategy strategy) {
      this(strategy, new HashMap());
   }      

   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use a strategy object. 
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes
    * @param format this is used to structure the generated XML
    */
   public Persister(Strategy strategy, Format format) {
      this(strategy, new HashMap(), format);           
   }
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided filter.
    * This persister will replace all variables encountered when
    * deserializing an object with mappings found in the filter.
    * 
    * @param filter the filter used to replace template variables
    * @param matcher this is used to customize the transformations
    */
   public Persister(Filter filter, Matcher matcher) {
      this(new TreeStrategy(), filter, matcher);
   }     

   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided filter.
    * This persister will replace all variables encountered when
    * deserializing an object with mappings found in the filter.
    * 
    * @param filter the filter used to replace template variables
    * @param matcher this is used to customize the transformations
    * @param format this is used to structure the generated XML
    */
   public Persister(Filter filter, Matcher matcher, Format format) {
      this(new TreeStrategy(), filter, matcher, format);
   }

   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use a platform filter
    * object using the overrides within the provided map. This means
    * that template variables will be replaced firstly with mappings
    * from within the provided map, followed by system properties. 
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param data this is the map that contains the overrides
    */
   public Persister(Strategy strategy, Map data) {
      this(strategy, new PlatformFilter(data));           
   }
 
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided filter.
    * This persister will replace all variables encountered when
    * deserializing an object with mappings found in the filter.
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param data the filter data used to replace template variables
    * @param format this is used to format the generated XML document
    */
   public Persister(Strategy strategy, Map data, Format format) {
      this(strategy, new PlatformFilter(data), format);
   }  
        
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided filter.
    * This persister will replace all variables encountered when
    * deserializing an object with mappings found in the filter.
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param filter the filter used to replace template variables
    */
   public Persister(Strategy strategy, Filter filter) {
      this(strategy, filter, new Format());
   }   
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided filter.
    * This persister will replace all variables encountered when
    * deserializing an object with mappings found in the filter.
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param filter the filter used to replace template variables
    * @param format this is used to format the generated XML document
    */
   public Persister(Strategy strategy, Filter filter, Format format) {
      this(strategy, filter, new EmptyMatcher(), format);
   }  
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided matcher
    * for customizable transformations. The <code>Matcher</code> will
    * enable the persister to determine the correct way to transform
    * the types that are not annotated and considered primitives.
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param matcher this is used to customize the transformations
    */
   public Persister(Strategy strategy, Matcher matcher) {
      this(strategy, new PlatformFilter(), matcher);
   }  
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided matcher
    * for customizable transformations. The <code>Matcher</code> will
    * enable the persister to determine the correct way to transform
    * the types that are not annotated and considered primitives.
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param matcher this is used to customize the transformations
    * @param format this is used to format the generated XML document
    */
   public Persister(Strategy strategy, Matcher matcher, Format format) {
      this(strategy, new PlatformFilter(), matcher, format);
   } 
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided matcher
    * for customizable transformations. The <code>Matcher</code> will
    * enable the persister to determine the correct way to transform
    * the types that are not annotated and considered primitives.
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param matcher this is used to customize the transformations
    * @param filter the filter used to replace template variables
    */
   public Persister(Strategy strategy, Filter filter, Matcher matcher) {
      this(strategy, filter, matcher, new Format());
   } 
   
   /**
    * Constructor for the <code>Persister</code> object. This is used
    * to create a serializer object that will use the provided matcher
    * for customizable transformations. The <code>Matcher</code> will
    * enable the persister to determine the correct way to transform
    * the types that are not annotated and considered primitives.
    * <p>
    * This persister will use the provided <code>Strategy</code> to
    * intercept the XML elements in order to read and write persistent
    * data, such as the class name or version of the document.
    * 
    * @param strategy this is the strategy used to resolve classes 
    * @param matcher this is used to customize the transformations
    * @param filter the filter used to replace template variables
    */
   public Persister(Strategy strategy, Filter filter, Matcher matcher, Format format) {
      this.support = new Support(filter, matcher, format);
      this.manager = new SessionManager();
      this.strategy = strategy;
      this.format = format;
   } 
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, String source) throws Exception {
      return read(type, source, true);            
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, File source) throws Exception {
      return read(type, source, true);
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, InputStream source) throws Exception {
      return read(type, source, true);           
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, Reader source) throws Exception {
      return read(type, source, true);
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, InputNode source) throws Exception {
      return read(type, source, true);
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, String source, boolean strict) throws Exception {
      return read(type, new StringReader(source), strict);            
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, File source, boolean strict) throws Exception {
      InputStream file = new FileInputStream(source);
      
      try {
         return read(type, file, strict);
      } finally {
         file.close();          
      }
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, InputStream source, boolean strict) throws Exception {
      return read(type, NodeBuilder.read(source), strict);           
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, Reader source, boolean strict) throws Exception {
      return read(type, NodeBuilder.read(source), strict);
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and convert it into an object
    * of the specified type. If the XML source cannot be deserialized
    * or there is a problem building the object graph an exception
    * is thrown. The instance deserialized is returned.
    * 
    * @param type this is the class type to be deserialized from XML
    * @param node this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the object deserialized from the XML document 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(Class<? extends T> type, InputNode node, boolean strict) throws Exception {
      Session session = manager.open(strict);
      
      try {
         return read(type, node, session);
      } finally {
         manager.close();
      }
   }    
      
   /**
    * This <code>read</code> method will read the contents of the XML
    * document provided and convert it to an object of the specified
    * type. If the XML document cannot be deserialized or there is a
    * problem building the object graph an exception is thrown. The
    * object graph deserialized is returned.
    * 
    * @param type this is the XML schema class to be deserialized
    * @param node this provides the source of the XML document
    * @param session this is the session used for deserialization
    * 
    * @return the object deserialized from the XML document given
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   private <T> T read(Class<? extends T> type, InputNode node, Session session) throws Exception {
      return read(type, node, new Source(strategy, support, session));
   }
        
   /**
    * This <code>read</code> method will read the contents of the XML
    * document provided and convert it to an object of the specified
    * type. If the XML document cannot be deserialized or there is a
    * problem building the object graph an exception is thrown. The
    * object graph deserialized is returned.
    * 
    * @param type this is the XML schema class to be deserialized
    * @param node this provides the source of the XML document
    * @param context the contextual object used for deserialization 
    * 
    * @return the object deserialized from the XML document given
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   private <T> T read(Class<? extends T> type, InputNode node, Context context) throws Exception {
      return (T)new Traverser(context).read(node, type);
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * 
    * @return the same instance provided is returned when finished  
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(T value, String source) throws Exception{
      return read(value, source, true);            
   }
        
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(T value, File source) throws Exception{
      return read(value, source, true);
   }

   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(T value, InputStream source) throws Exception{
      return read(value, source, true);           
   }

   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */   
   public <T> T read(T value, Reader source) throws Exception{
      return read(value, source, true);
   }   
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */ 
   public <T> T read(T value, InputNode source) throws Exception {
      return read(value, source, true);
   }
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the same instance provided is returned when finished  
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(T value, String source, boolean strict) throws Exception{
      return read(value, new StringReader(source), strict);            
   }
        
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(T value, File source, boolean strict) throws Exception{
      InputStream file = new FileInputStream(source);
      
      try {
         return read(value, file, strict);
      }finally {
         file.close();           
      }
   }

   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   public <T> T read(T value, InputStream source, boolean strict) throws Exception{
      return read(value, NodeBuilder.read(source), strict);           
   }

   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */   
   public <T> T read(T value, Reader source, boolean strict) throws Exception{
      return read(value, NodeBuilder.read(source), strict);
   }   
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param node this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */ 
   public <T> T read(T value, InputNode node, boolean strict) throws Exception {
      Session session = manager.open(strict);
      
      try {
         return read(value, node, session);
      }finally {
         manager.close();
      }
   } 
   
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param node this provides the source of the XML document
    * @param session this is the session used for the deserialization
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */ 
   private <T> T read(T value, InputNode node, Session session) throws Exception {
      return read(value, node, new Source(strategy, support, session));
   }
           
   /**
    * This <code>read</code> method will read the contents of the XML
    * document from the provided source and populate the object with
    * the values deserialized. This is used as a means of injecting an
    * object with values deserialized from an XML document. If the
    * XML source cannot be deserialized or there is a problem building
    * the object graph an exception is thrown.
    * 
    * @param value this is the object to deserialize the XML in to
    * @param node this provides the source of the XML document
    * @param context the contextual object used for deserialization
    * 
    * @return the same instance provided is returned when finished 
    * 
    * @throws Exception if the object cannot be fully deserialized
    */
   private <T> T read(T value, InputNode node, Context context) throws Exception {
      return (T)new Traverser(context).read(node, value);
   }   
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, String source) throws Exception {
      return validate(type, source, true);            
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, File source) throws Exception {
      return validate(type, source, true);
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, InputStream source) throws Exception {
      return validate(type, source, true);           
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, Reader source) throws Exception {
      return validate(type, source, true);
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, InputNode source) throws Exception {
      return validate(type, source, true);
   }         
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, String source, boolean strict) throws Exception {
      return validate(type, new StringReader(source), strict);            
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, File source, boolean strict) throws Exception {
      InputStream file = new FileInputStream(source);
      
      try {
         return validate(type, file, strict);
      } finally {
         file.close();          
      }
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, InputStream source, boolean strict) throws Exception {
      return validate(type, NodeBuilder.read(source), strict);           
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param source this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, Reader source, boolean strict) throws Exception {
      return validate(type, NodeBuilder.read(source), strict);
   }
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param node this provides the source of the XML document
    * @param strict this determines whether to read in strict mode
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   public boolean validate(Class type, InputNode node, boolean strict) throws Exception {
      Session session = manager.open(strict);
      
      try {
         return validate(type, node, session);
      }finally {
         manager.close();
      }
   } 
   
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param node this provides the source of the XML document
    * @param session this is the session that is used for validation
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   private boolean validate(Class type, InputNode node, Session session) throws Exception {
      return validate(type, node, new Source(strategy, support, session));
   } 
           
   /**
    * This <code>validate</code> method will validate the contents of
    * the XML document against the specified XML class schema. This is
    * used to perform a read traversal of the class schema such that 
    * the document can be tested against it. This is preferred to
    * reading the document as it does not instantiate the objects or
    * invoke any callback methods, thus making it a safe validation.
    * 
    * @param type this is the class type to be validated against XML
    * @param node this provides the source of the XML document
    * @param context the contextual object used for deserialization  
    * 
    * @return true if the document matches the class XML schema 
    * 
    * @throws Exception if the class XML schema does not fully match
    */
   private boolean validate(Class type, InputNode node, Context context) throws Exception {
      return new Traverser(context).validate(node, type);
   }

   /**
    * This <code>write</code> method will traverse the provided object
    * checking for field annotations in order to compose the XML data.
    * This uses the <code>getClass</code> method on the object to
    * determine the class file that will be used to compose the schema.
    * If there is no <code>Root</code> annotation for the class then
    * this will throw an exception. The root annotation is the only
    * annotation required for an object to be serialized.  
    * 
    * @param source this is the object that is to be serialized
    * @param root this is where the serialized XML is written to
    * 
    * @throws Exception if the schema for the object is not valid
    */
   public void write(Object source, OutputNode root) throws Exception {
      Session session = manager.open();
      
      try {
         write(source, root, session);
      } finally {
         manager.close();
      }
   }

   /**
    * This <code>write</code> method will traverse the provided object
    * checking for field annotations in order to compose the XML data.
    * This uses the <code>getClass</code> method on the object to
    * determine the class file that will be used to compose the schema.
    * If there is no <code>Root</code> annotation for the class then
    * this will throw an exception. The root annotation is the only
    * annotation required for an object to be serialized.  
    * 
    * @param source this is the object that is to be serialized
    * @param root this is where the serialized XML is written to
    * @param session this is the session used for serialization
    * 
    * @throws Exception if the schema for the object is not valid
    */   
   private void write(Object source, OutputNode root, Session session)throws Exception {   
      write(source, root, new Source(strategy, support, session));
   }

   /**
    * This <code>write</code> method will traverse the provided object
    * checking for field annotations in order to compose the XML data.
    * This uses the <code>getClass</code> method on the object to
    * determine the class file that will be used to compose the schema.
    * If there is no <code>Root</code> annotation for the class then
    * this will throw an exception. The root annotation is the only
    * annotation required for an object to be serialized.  
    * 
    * @param source this is the object that is to be serialized
    * @param context this is a contextual object used for serialization
    * 
    * @throws Exception if the schema for the object is not valid
    */     
   private void write(Object source, OutputNode node, Context context) throws Exception {
      new Traverser(context).write(node, source);
   }
   
   /**
    * This <code>write</code> method will traverse the provided object
    * checking for field annotations in order to compose the XML data.
    * This uses the <code>getClass</code> method on the object to
    * determine the class file that will be used to compose the schema.
    * If there is no <code>Root</code> annotation for the class then
    * this will throw an exception. The root annotation is the only
    * annotation required for an object to be serialized.  
    * 
    * @param source this is the object that is to be serialized
    * @param out this is where the serialized XML is written to
    * 
    * @throws Exception if the schema for the object is not valid
    */  
   public void write(Object source, File out) throws Exception {
      OutputStream file = new FileOutputStream(out);
      
      try {
         write(source, file);
      }finally {
         file.close();
      }
   }
   
   /**
    * This <code>write</code> method will traverse the provided object
    * checking for field annotations in order to compose the XML data.
    * This uses the <code>getClass</code> method on the object to
    * determine the class file that will be used to compose the schema.
    * If there is no <code>Root</code> annotation for the class then
    * this will throw an exception. The root annotation is the only
    * annotation required for an object to be serialized.  
    * 
    * @param source this is the object that is to be serialized
    * @param out this is where the serialized XML is written to
    * 
    * @throws Exception if the schema for the object is not valid
    */   
   public void write(Object source, OutputStream out) throws Exception {
      write(source, out, "utf-8");
   }
   
   /**
    * This <code>write</code> method will traverse the provided object
    * checking for field annotations in order to compose the XML data.
    * This uses the <code>getClass</code> method on the object to
    * determine the class file that will be used to compose the schema.
    * If there is no <code>Root</code> annotation for the class then
    * this will throw an exception. The root annotation is the only
    * annotation required for an object to be serialized.  
    * 
    * @param source this is the object that is to be serialized
    * @param out this is where the serialized XML is written to
    * @param charset this is the character encoding to be used
    * 
    * @throws Exception if the schema for the object is not valid
    */  
   public void write(Object source, OutputStream out, String charset) throws Exception {
      write(source, new OutputStreamWriter(out, charset));
   }
   
   /**
    * This <code>write</code> method will traverse the provided object
    * checking for field annotations in order to compose the XML data.
    * This uses the <code>getClass</code> method on the object to
    * determine the class file that will be used to compose the schema.
    * If there is no <code>Root</code> annotation for the class then
    * this will throw an exception. The root annotation is the only
    * annotation required for an object to be serialized.  
    * 
    * @param source this is the object that is to be serialized
    * @param out this is where the serialized XML is written to
    * 
    * @throws Exception if the schema for the object is not valid
    */   
   public void write(Object source, Writer out) throws Exception {
      write(source, NodeBuilder.write(out, format));
   }
}
