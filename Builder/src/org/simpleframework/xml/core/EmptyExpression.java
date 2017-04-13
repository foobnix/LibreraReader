/*
 * EmptyExpression May 2007
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.Style;

/**
 * The <code>EmptyExpression</code> object is used to represent a path
 * that represents the current context. This is a much more lightweight
 * alternative to parsing "." with the <code>PathParser</code> object
 * as it does not require the allocation of collections or buffers.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.PathParser
 */
class EmptyExpression implements Expression {

   /**
    * This is a list that is used to create an empty iterator.
    */
   private final List<String> list;
   
   /**
    * This is the style that is used to style any paths created. 
    */
   private final Style style;

   /**
    * Constructor for <code>EmptyExpression</code> object. This is
    * used to create an expression for an empty path. An empty path
    * is basically the root element ".". 
    * 
    * @param format the format used to style the paths created
    */
   public EmptyExpression(Format format) {
      this.list = new LinkedList<String>();
      this.style = format.getStyle();
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
      return list.iterator();
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
      return 0;
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
      return null;
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
      return null;
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
      return null;
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
      return "";
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
      return style.getElement(name);
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
      return style.getAttribute(name);
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
      return null;
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
      return null;
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
      return false;
   }

   /**
    * This is used to determine if the expression is a path. An
    * expression represents a path if it contains more than one
    * segment. If only one segment exists it is an element name.
    * 
    * @return true if this contains more than one segment
    */
   public boolean isPath() {
      return false;
   }

   /**
    * This method is used to determine if this expression is an
    * empty path. An empty path can be represented by a single
    * period, '.'. It identifies the current path.
    * 
    * @return returns true if this represents an empty path
    */
   public boolean isEmpty() {
      return true;
   }
}
