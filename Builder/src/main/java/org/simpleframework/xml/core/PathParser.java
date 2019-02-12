/*
 * PathParser.java November 2010
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

package org.simpleframework.xml.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.Style;
import org.simpleframework.xml.util.Cache;
import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>PathParser</code> object is used to parse XPath paths.
 * This will parse a subset of the XPath expression syntax, such
 * that the path can be used to identify and navigate various XML
 * structures. Example paths that this can parse are as follows.
 * <pre>
 * 
 *    ./example/path
 *    ./example[2]/path/
 *    example/path
 *    example/path/@attribute
 *    ./path/@attribute 
 *    
 * </pre>
 * If the parsed path does not match an XPath expression similar to
 * the above then an exception is thrown. Once parsed the segments
 * of the path can be used to traverse data structures modelled on
 * an XML document or fragment.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.ExpressionBuilder
 */
class PathParser implements Expression {
   
   /**
    * This is used to cache the attributes created by this path.
    */
   protected Cache<String> attributes;
   
   /**
    * This is used to cache the elements created by this path.
    */
   protected Cache<String> elements;
   
   /**
    * This contains a list of the indexes for each path segment.
    */
   protected List<Integer> indexes;   
   
   /**
    * This is used to store the path prefixes for the parsed path.
    */
   protected List<String> prefixes;
   
   /**
    * This contains a list of the path segments that were parsed.
    */
   protected List<String> names;
   
   /**
    * This is used to build a fully qualified path expression.
    */
   protected StringBuilder builder;
   
   /**
    * This is the fully qualified path expression for this.
    */
   protected String location;
   
   /**
    * This is the the cached canonical representation of the path.
    */
   protected String cache;
   
   /**
    * This is a cache of the canonical path representation.
    */
   protected String path;
   
   
   /**
    * This is the format used to style the path segments.
    */
   protected Style style;
   
   /**
    * This is the type the expressions are to be parsed for.
    */
   protected Type type;
   
   /**
    * This is used to determine if the path is an attribute.
    */
   protected boolean attribute;
   
   /**
    * This is a copy of the source data that is to be parsed.
    */
   protected char[] data;
   
   /**
    * This represents the number of characters in the source path.
    */
   protected int count;
   
   /**
    * This is the start offset that skips any root references.
    */
   protected int start;
 
   /**
    * This is the current seek position for the parser.
    */
   protected int off;

   /**
    * Constructor for the <code>PathParser</code> object. This must
    * be given a valid XPath expression. Currently only a subset of
    * the XPath syntax is supported by this parser. Once finished
    * the parser will contain all the extracted path segments.
    * 
    * @param path this is the XPath expression to be parsed
    * @param type this is the type the expressions are parsed for
    * @param format this is the format used to style the path
    */
   public PathParser(String path, Type type, Format format) throws Exception {
      this.attributes = new ConcurrentCache<String>();
      this.elements = new ConcurrentCache<String>();
      this.indexes = new ArrayList<Integer>();
      this.prefixes = new ArrayList<String>();
      this.names = new ArrayList<String>();
      this.builder = new StringBuilder();
      this.style = format.getStyle();
      this.type = type;
      this.path = path;
      this.parse(path);
   }
   
   /**
    * This method is used to determine if this expression is an
    * empty path. An empty path can be represented by a single
    * period, '.'. It identifies the current path.
    * 
    * @return returns true if this represents an empty path
    */
   public boolean isEmpty() {
      return isEmpty(location);
   }
   
   /**
    * This is used to determine if the expression is a path. An
    * expression represents a path if it contains more than one
    * segment. If only one segment exists it is an element name.
    * 
    * @return true if this contains more than one segment
    */
   public boolean isPath() {
      return names.size() > 1;
   }
   
   /**
    * This is used to determine if the expression points to an
    * attribute value. An attribute value contains an '@' character
    * before the last segment name. Such expressions distinguish
    * element references from attribute references.
    * 
    * @return this returns true if the path has an attribute
    */
   public boolean isAttribute() {
      return attribute;
   }
   
   /**
    * If the first path segment contains an index it is provided
    * by this method. There may be several indexes within a 
    * path, however only the index at the first segment is issued
    * by this method. If there is no index this will return 1.
    * 
    * @return this returns the index of this path expression
    */
   public int getIndex() {
      return indexes.get(0);
   }
   
   /**
    * This is used to extract a namespace prefix from the path
    * expression. A prefix is used to qualify the XML element name
    * and does not form part of the actual path structure. This
    * can be used to add the namespace in addition to the name.
    * 
    * @return this returns the prefix for the path expression
    */
   public String getPrefix(){
      return prefixes.get(0);
   }
   
   /**
    * This can be used to acquire the first path segment within
    * the expression. The first segment represents the parent XML
    * element of the path. All segments returned do not contain
    * any slashes and so represents the real element name.
    * 
    * @return this returns the parent element for the path
    */
   public String getFirst() {
      return names.get(0);
   }
   
   /**
    * This can be used to acquire the last path segment within
    * the expression. The last segment represents the leaf XML
    * element of the path. All segments returned do not contain
    * any slashes and so represents the real element name.
    * 
    * @return this returns the leaf element for the path
    */ 
   public String getLast() {
      int count = names.size();
      int index = count - 1;
      
      return names.get(index);
   }

   /**
    * This location contains the full path expression with all
    * of the indexes explicitly shown for each path segment. This
    * is used to create a uniform representation that can be used
    * for comparisons of different path expressions. 
    * 
    * @return this returns an expanded version of the path
    */
   public String getPath() {
      return location;
   }
   
   /**
    * This is used to acquire the element path using this XPath
    * expression. The element path is simply the fully qualified
    * path for this expression with the provided name appended.
    * If this is an empty path, the provided name is returned.
    * 
    * @param name this is the name of the element to be used
    * 
    * @return a fully qualified path for the specified name
    */
   public String getElement(String name) {
      if(!isEmpty(location)) {
         String path = elements.fetch(name); 
         
         if(path == null) {
            path = getElementPath(location, name);
            
            if(path != null) {
               elements.cache(name, path);
            }
         }
         return path;
      }
      return style.getElement(name);
   }
   
   /**
    * This is used to acquire the element path using this XPath
    * expression. The element path is simply the fully qualified
    * path for this expression with the provided name appended.
    * If this is an empty path, the provided name is returned.
    * 
    * @param path this is the path expression to be used
    * @param name this is the name of the element to be used
    * 
    * @return a fully qualified path for the specified name
    */
   protected String getElementPath(String path, String name) {
      String element = style.getElement(name);
      
      if(isEmpty(element)) {
         return path;
      }
      if(isEmpty(path)) {
         return element;
      }
      return path + "/"+ element+"[1]";
   }
   
   /**
    * This is used to acquire the attribute path using this XPath
    * expression. The attribute path is simply the fully qualified
    * path for this expression with the provided name appended.
    * If this is an empty path, the provided name is returned.
    * 
    * @param name this is the name of the attribute to be used
    * 
    * @return a fully qualified path for the specified name
    */
   public String getAttribute(String name) {
      if(!isEmpty(location)) {
         String path = attributes.fetch(name); 
         
         if(path == null) {
            path = getAttributePath(location, name);
            
            if(path != null) {
               attributes.cache(name, path);
            }
         }
         return path;
      }
      return style.getAttribute(name);
   }
   
   /**
    * This is used to acquire the attribute path using this XPath
    * expression. The attribute path is simply the fully qualified
    * path for this expression with the provided name appended.
    * If this is an empty path, the provided name is returned.
    * 
    * @param path this is the path expression to be used
    * @param name this is the name of the attribute to be used
    * 
    * @return a fully qualified path for the specified name
    */
   protected String getAttributePath(String path, String name) {
      String attribute = style.getAttribute(name);
            
      if(isEmpty(path)) { 
         return attribute;
      }
      return path +"/@" +attribute;
   }

   /**
    * This is used to iterate over the path segments that have 
    * been extracted from the source XPath expression. Iteration
    * over the segments is done in the order they were parsed
    * from the source path.
    * 
    * @return this returns an iterator for the path segments
    */
   public Iterator<String> iterator() {
      return names.iterator();
   }
   
   /**
    * This allows an expression to be extracted from the current
    * context. Extracting expressions in this manner makes it 
    * more convenient for navigating structures representing
    * the XML document. If an expression can not be extracted
    * with the given criteria an exception will be thrown.
    * 
    * @param from this is the number of segments to skip to
    * 
    * @return this returns an expression from this one
    */
   public Expression getPath(int from) {  
      return getPath(from, 0);
   }
   
   /**
    * This allows an expression to be extracted from the current
    * context. Extracting expressions in this manner makes it 
    * more convenient for navigating structures representing
    * the XML document. If an expression can not be extracted
    * with the given criteria an exception will be thrown.
    * 
    * @param from this is the number of segments to skip to
    * @param trim the number of segments to trim from the end
    * 
    * @return this returns an expression from this one
    */
   public Expression getPath(int from, int trim) {
      int last = names.size() - 1;
      
      if(last- trim >= from) {       
         return new PathSection(from, last -trim);
      }
      return new PathSection(from, from);
   }

   /**
    * This method is used to parse the provided XPath expression.
    * When parsing the expression this will trim any references
    * to the root context, also any trailing slashes are removed.
    * An exception is thrown if the path is invalid.
    * 
    * @param path this is the XPath expression to be parsed
    */
   private void parse(String path) throws Exception {
      if(path != null) {
         count = path.length();
         data = new char[count];
         path.getChars(0, count, data, 0);
      }
      path();
   }
   
   /**
    * This method is used to parse the provided XPath expression.
    * When parsing the expression this will trim any references
    * to the root context, also any trailing slashes are removed.
    * An exception is thrown if the path is invalid.
    */ 
   private void path() throws Exception {
      if(data[off] == '/') {
         throw new PathException("Path '%s' in %s references document root", path, type);
      }
      if(data[off] == '.') {
         skip();
      }
      while (off < count) {
         if(attribute) {
            throw new PathException("Path '%s' in %s references an invalid attribute", path, type);
         }
         segment();
      }  
      truncate();
      build();
   }
   
   /**
    * This method is used to build a fully qualified path that has
    * each segment index. Building a path in this manner ensures
    * that a parsed path can have a unique string that identifies 
    * the exact XML element the expression points to. 
    */
   private void build() {
      int count = names.size();
      int last = count - 1;
      
      for(int i = 0; i < count; i++) {
         String prefix = prefixes.get(i);
         String segment = names.get(i);
         int index = indexes.get(i);
         
         if(i > 0) {
            builder.append('/');
         } 
         if(attribute && i == last) {
            builder.append('@');
            builder.append(segment);           
         } else {
            if(prefix != null) {
               builder.append(prefix);
               builder.append(':');
            }
            builder.append(segment);
            builder.append('[');
            builder.append(index);
            builder.append(']');
         }
      }
      location = builder.toString();
   }

   /**
    * This is used to skip any root prefix for the path. Skipping 
    * the root prefix ensures that it is not considered as a valid
    * path segment and so is not returned as part of the iterator
    * nor is it considered with building a string representation.
    */
   private void skip() throws Exception {
      if (data.length > 1) {
         if (data[off + 1] != '/') {
            throw new PathException("Path '%s' in %s has an illegal syntax", path, type);
         }
         off++;
      }      
      start = ++off;
   }

   /**
    * This method is used to extract a path segment from the source
    * expression. Before extracting the segment this validates the
    * input to ensure it represents a valid path. If the path is
    * not valid then this will thrown an exception.
    */
   private void segment() throws Exception {
      char first = data[off];

      if(first == '/') {
         throw new PathException("Invalid path expression '%s' in %s", path, type);
      }
      if(first == '@') {
         attribute();
      } else {
         element();
      }
      align();
   }
   
   /**
    * This is used to extract an element from the path expression. 
    * An element value is one that contains only alphanumeric values
    * or any special characters allowed within an XML element name.
    * If an illegal character is found an exception is thrown.
    */
   private void element() throws Exception { 
      int mark = off;
      int size = 0;
      
      while(off < count) {         
         char value = data[off++];
         
         if(!isValid(value)) {
            if(value == '@') {
               off--;
               break;
            } else if(value == '[') {
               index();            
               break;
            } else if(value != '/') { 
               throw new PathException("Illegal character '%s' in element for '%s' in %s", value, path, type);
            }         
            break;
         }
         size++;         
      }
      element(mark, size);
   }
   
   /**
    * This is used to extract an attribute from the path expression. 
    * An attribute value is one that contains only alphanumeric values
    * or any special characters allowed within an XML attribute name.
    * If an illegal character is found an exception is thrown.
    */
   private void attribute() throws Exception {
      int mark = ++off;      
      
      while(off < count) {
         char value = data[off++];
         
         if(!isValid(value)) {
            throw new PathException("Illegal character '%s' in attribute for '%s' in %s", value, path, type);
         }         
      }
      if(off <= mark) {
         throw new PathException("Attribute reference in '%s' for %s is empty", path, type);
      } else {
         attribute = true;
      }
      attribute(mark, off - mark);
   }
   
   
   /**
    * This is used to extract an index from an element. An index is
    * a numerical value that identifies the position of the path
    * within the XML document. If the index can not be extracted
    * from the expression an exception is thrown.
    */
   private void index() throws Exception {
      int value = 0;

      if(data[off-1] == '[') {
         while(off < count) {
            char digit = data[off++];
            
            if(!isDigit(digit)){
               break;
            }
            value *= 10;
            value += digit;
            value -= '0';  
         }
      }
      if(data[off++ - 1] != ']') {
         throw new PathException("Invalid index for path '%s' in %s", path, type);
      }
      indexes.add(value);
   }
   
   
   /**
    * This method is used to trim any trailing characters at the
    * end of the path. Trimming will remove any trailing legal
    * characters at the end of the path that we do not want in a
    * canonical string representation of the path expression. 
    */
   private void truncate() throws Exception {
      if(off - 1 >= data.length) {
         off--;
      } else if(data[off-1] == '/'){
         off--;
      }
   }
   
   /**
    * This is used to add a default index to a segment or attribute
    * extracted from the source expression. In the event that a
    * segment does not contain an index, the default index of 1 is
    * assigned to the element for consistency.
    */
   private void align() throws Exception {
      int require = names.size();
      int size = indexes.size();
      
      if(require > size) {
         indexes.add(1);
      }
   }
   
   /**
    * This is used to determine if a string is empty. A string is
    * considered empty if it is null or of zero length. 
    * 
    * @param text this is the text to check if it is empty
    * 
    * @return this returns true if the string is empty or null
    */
   private boolean isEmpty(String text) {
      return text == null || text.length() == 0;
   }
   
   /** 
    * This is used to determine if the provided character is a digit.
    * Only digits can be used within a segment index, so this is used
    * when parsing the index to ensure all characters are valid.     
    * 
    * @param value this is the value of the character
    * 
    * @return this returns true if the provide character is a digit
    */
   private boolean isDigit(char value) {
      return Character.isDigit(value);
   }
   
   /**
    * This is used to determine if the provided character is a legal
    * XML element character. This is used to ensure all extracted
    * element names conform to legal element names.
    * 
    * @param value this is the value of the character
    * 
    * @return this returns true if the provided character is legal
    */
   private boolean isValid(char value) {
      return isLetter(value) || isSpecial(value);
   }
   
   /**
    * This is used to determine if the provided character is a legal
    * XML element character. This is used to ensure all extracted
    * element and attribute names conform to the XML specification.
    * 
    * @param value this is the value of the character
    * 
    * @return this returns true if the provided character is legal
    */
   private boolean isSpecial(char value) {
      return value == '_' || value == '-' || value == ':';
   }
   
   /**
    * This is used to determine if the provided character is an
    * alpha numeric character. This is used to ensure all extracted
    * element and attribute names conform to the XML specification.
    * 
    * @param value this is the value of the character
    * 
    * @return this returns true if the provided character is legal
    */
   private boolean isLetter(char value) {
      return Character.isLetterOrDigit(value);
   }

   /**
    * This will add a path segment to the list of segments. A path
    * segment is added only if it has at least one character. All
    * segments can be iterated over when parsing has completed.
    * 
    * @param start this is the start offset for the path segment
    * @param count this is the number of characters in the segment
    */
   private void element(int start, int count) {
      String segment = new String(data, start, count);

      if(count > 0) {
         element(segment);
      }
   }
   
   /**
    * This will add a path segment to the list of segments. A path
    * segment is added only if it has at least one character. All
    * segments can be iterated over when parsing has completed.
    * 
    * @param start this is the start offset for the path segment
    * @param count this is the number of characters in the segment
    */
   private void attribute(int start, int count) {
      String segment = new String(data, start, count);

      if(count > 0) {
         attribute(segment);
      }
   }
   
   /**
    * This will insert the path segment provided. A path segment is
    * represented by an optional namespace prefix and an XML element
    * name. If there is no prefix then a null is entered this will
    * ensure that the names and segments are kept aligned by index.
    * 
    * @param segment this is the path segment to be inserted
    */
   private void element(String segment) {
      int index = segment.indexOf(':');
      String prefix = null;
      
      if(index > 0) {
         prefix = segment.substring(0, index);
         segment = segment.substring(index+1);
      }
      String element = style.getElement(segment);
      
      prefixes.add(prefix);
      names.add(element);
   }
   
   /**
    * This will insert the path segment provided. A path segment is
    * represented by an optional namespace prefix and an XML element
    * name. If there is no prefix then a null is entered this will
    * ensure that the names and segments are kept aligned by index.
    * 
    * @param segment this is the path segment to be inserted
    */
   private void attribute(String segment) {
      String attribute = style.getAttribute(segment);
      
      prefixes.add(null);
      names.add(attribute);
   }
   
   /**
    * Provides a canonical XPath expression. This is used for both
    * debugging and reporting. The path returned represents the 
    * original path that has been parsed to form the expression.
    * 
    * @return this returns the string format for the XPath
    */
   public String toString() {
      int size = off - start;
      
      if(cache == null) {
         cache = new String(data, start, size);
      }
      return cache;
   } 
   
   /**
    * The <code>PathSection</code> represents a section of a path 
    * that is extracted. Providing a section allows the expression
    * to be broken up in to smaller parts without having to parse
    * the path again. This is used primarily for better performance.
    * 
    * @author Niall Gallagher
    */
   private class PathSection implements Expression {
      
      /**
       * This contains a cache of the path segments of the section.
       */
      private List<String> cache;
      
      /**
       * This is the fragment of the original path this section uses.
       */
      private String section;
      
      /**
       * This contains a cache of the canonical path representation.
       */
      private String path;
      
      /**
       * This is the first section index for this path section.
       */
      private int begin;
      
      /**
       * This is the last section index for this path section.
       */
      private int end;
      
      /**
       * Constructor for the <code>PathSection</code> object. A path
       * section represents a section of an original path expression.
       * To create a section the first and last segment index needs
       * to be provided.
       * 
       * @param index this is the first path segment index 
       * @param end this is the last path segment index
       */
      public PathSection(int index, int end) {
         this.cache = new ArrayList<String>();
         this.begin = index;         
         this.end = end;
      }
      
      /**
       * This method is used to determine if this expression is an
       * empty path. An empty path can be represented by a single
       * period, '.'. It identifies the current path.
       * 
       * @return returns true if this represents an empty path
       */
      public boolean isEmpty() {
         return begin == end;
      }
      
      /**
       * This is used to determine if the expression is a path. An
       * expression represents a path if it contains more than one
       * segment. If only one segment exists it is an element name.
       * 
       * @return true if this contains more than one segment
       */
      public boolean isPath() {
         return end - begin >= 1;
      }
      
      /**
       * This is used to determine if the expression points to an
       * attribute value. An attribute value contains an '@' character
       * before the last segment name. Such expressions distinguish
       * element references from attribute references.
       * 
       * @return this returns true if the path has an attribute
       */
      public boolean isAttribute() {
         if(attribute) {
            return end >= names.size() - 1;
         }
         return false;
      }
      
      /**
       * This location contains the full path expression with all
       * of the indexes explicitly shown for each path segment. This
       * is used to create a uniform representation that can be used
       * for comparisons of different path expressions. 
       * 
       * @return this returns an expanded version of the path
       */
      public String getPath() {
         if(section == null) {
            section = getCanonicalPath();
         }
         return section;
      }
      
      /**
       * This is used to acquire the element path using this XPath
       * expression. The element path is simply the fully qualified
       * path for this expression with the provided name appended.
       * If this is an empty path, the provided name is returned.
       * 
       * @param name this is the name of the element to be used
       * 
       * @return a fully qualified path for the specified name
       */
      public String getElement(String name) {
         String path = getPath();
         
         if(path != null) {
            return getElementPath(path, name);
         }
         return name;
      }
      
      /**
       * This is used to acquire the attribute path using this XPath
       * expression. The attribute path is simply the fully qualified
       * path for this expression with the provided name appended.
       * If this is an empty path, the provided name is returned.
       * 
       * @param name this is the name of the attribute to be used
       * 
       * @return a fully qualified path for the specified name
       */
      public String getAttribute(String name) {
         String path = getPath();
         
         if(path != null) {
            return getAttributePath(path, name);
         }
         return name;
      }
      
      /**
       * If the first path segment contains an index it is provided
       * by this method. There may be several indexes within a 
       * path, however only the index at the first segment is issued
       * by this method. If there is no index this will return 1.
       * 
       * @return this returns the index of this path expression
       */
      public int getIndex() {
         return indexes.get(begin);
      }
      
      /**
       * This is used to extract a namespace prefix from the path
       * expression. A prefix is used to qualify the XML element name
       * and does not form part of the actual path structure. This
       * can be used to add the namespace in addition to the name.
       * 
       * @return this returns the prefix for the path expression
       */
      public String getPrefix() {
         return prefixes.get(begin);
      }
      
      /**
       * This can be used to acquire the first path segment within
       * the expression. The first segment represents the parent XML
       * element of the path. All segments returned do not contain
       * any slashes and so represents the real element name.
       * 
       * @return this returns the parent element for the path
       */
      public String getFirst() {
         return names.get(begin);
      }
      
      /**
       * This can be used to acquire the last path segment within
       * the expression. The last segment represents the leaf XML
       * element of the path. All segments returned do not contain
       * any slashes and so represents the real element name.
       * 
       * @return this returns the leaf element for the path
       */ 
      public String getLast() {
         return names.get(end);
      }
      
      /**
       * This allows an expression to be extracted from the current
       * context. Extracting expressions in this manner makes it 
       * more convenient for navigating structures representing
       * the XML document. If an expression can not be extracted
       * with the given criteria an exception will be thrown.
       * 
       * @param from this is the number of segments to skip to
       * 
       * @return this returns an expression from this one
       */
      public Expression getPath(int from) {     
         return getPath(from, 0);
      }
      
      /**
       * This allows an expression to be extracted from the current
       * context. Extracting expressions in this manner makes it 
       * more convenient for navigating structures representing
       * the XML document. If an expression can not be extracted
       * with the given criteria an exception will be thrown.
       * 
       * @param from this is the number of segments to skip to
       * @param trim the number of segments to trim from the end
       * 
       * @return this returns an expression from this one
       */
      public Expression getPath(int from, int trim) {
         return new PathSection(begin + from, end - trim);
      }
      
      /**
       * This is used to iterate over the path segments that have 
       * been extracted from the source XPath expression. Iteration
       * over the segments is done in the order they were parsed
       * from the source path.
       * 
       * @return this returns an iterator for the path segments
       */
      public Iterator<String> iterator() {
         if(cache.isEmpty()) {
            for(int i = begin; i <= end; i++) {
               String segment = names.get(i);
               
               if(segment != null) {
                  cache.add(segment);
               }
            }
         }
         return cache.iterator();         
      }      
      
      /**
       * This is used to acquire the path section that contains all
       * the segments in the section as well as the indexes for the
       * segments. This method basically gets a substring of the
       * primary path location from the first to the last segment.
       * 
       * @return this returns the section as a fully qualified path
       */
      private String getCanonicalPath() {
         int start = 0;
         int last = 0;
         int pos = 0;
         
         for(pos = 0; pos < begin; pos++) {
            start = location.indexOf('/', start + 1);
         }
         for(last = start; pos <= end; pos++) {
            last = location.indexOf('/', last + 1);
            if(last == -1) {
               last = location.length();
            }
         }
         return location.substring(start + 1, last);
      }
      
      /**
       * Provides a canonical XPath expression. This is used for both
       * debugging and reporting. The path returned represents the 
       * original path that has been parsed to form the expression.
       * 
       * @return this returns the string format for the XPath
       */
      private String getFragment() {        
         int last = start;
         int pos = 0; 
         
         for(int i = 0; i <= end;) {
            if(last >= count) {
               last++;
               break;
            }
            if(data[last++] == '/'){
               if(++i == begin) {                  
                  pos = last;
               }         
            }            
         }
         return new String(data, pos, --last -pos);         
      }
      
      /**
       * Provides a canonical XPath expression. This is used for both
       * debugging and reporting. The path returned represents the 
       * original path that has been parsed to form the expression.
       * 
       * @return this returns the string format for the XPath
       */
      public String toString() {
         if(path == null) {
            path = getFragment();
         }
         return path;
      }   
   }
}
