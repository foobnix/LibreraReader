/*
 * Expression.java November 2010
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

/**
 * The <code>Expression</code> interface is used to represent an XPath
 * expression. Any element or attribute may be defined as having an
 * XPath expression so that it may be located within an XML document.
 * This provides a convenient interface to navigating structures
 * based on an XPath expression. The below formats are supported.
 * <pre>
 * 
 *    ./example/path
 *    ./example[2]/path/
 *    example/path
 *    example/path/@attribute
 *    ./path/@attribute              
 *    
 * </pre>
 * As can be seen only a subset of the XPath syntax is supported by
 * this. For convenience this provides a means to acquire paths
 * from within a path, which makes a single expression more useful
 * when navigating structures representing the XML document.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.ExpressionBuilder
 */
interface Expression extends Iterable<String> {
	
   /**
    * If the first path segment contains an index it is provided
    * by this method. There may be several indexes within a 
    * path, however only the index at the first segment is issued
    * by this method. If there is no index this will return 1.
    * 
    * @return this returns the index of this path expression
    */
   int getIndex();
   
   /**
    * This is used to extract a namespace prefix from the path
    * expression. A prefix is used to qualify the XML element name
    * and does not form part of the actual path structure. This
    * can be used to add the namespace in addition to the name.
    * 
    * @return this returns the prefix for the path expression
    */
   String getPrefix();
   
   /**
    * This can be used to acquire the first path segment within
    * the expression. The first segment represents the parent XML
    * element of the path. All segments returned do not contain
    * any slashes and so represents the real element name.
    * 
    * @return this returns the parent element for the path
    */
   String getFirst();

   /**
    * This can be used to acquire the last path segment within
    * the expression. The last segment represents the leaf XML
    * element of the path. All segments returned do not contain
    * any slashes and so represents the real element name.
    * 
    * @return this returns the leaf element for the path
    */   
   String getLast();
   
   /**
    * This location contains the full path expression with all
    * of the indexes explicitly shown for each path segment. This
    * is used to create a uniform representation that can be used
    * for comparisons of different path expressions. 
    * 
    * @return this returns an expanded version of the path
    */
   String getPath();
   
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
   String getElement(String name);
   
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
   String getAttribute(String name);
   
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
   Expression getPath(int from);
   
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
   Expression getPath(int from, int trim);
   
   /**
    * This is used to determine if the expression points to an
    * attribute value. An attribute value contains an '@' character
    * before the last segment name. Such expressions distinguish
    * element references from attribute references.
    * 
    * @return this returns true if the path has an attribute
    */
   boolean isAttribute();
   
   /**
    * This is used to determine if the expression is a path. An
    * expression represents a path if it contains more than one
    * segment. If only one segment exists it is an element name.
    * 
    * @return true if this contains more than one segment
    */
   boolean isPath();
   
   /**
    * This method is used to determine if this expression is an
    * empty path. An empty path can be represented by a single
    * period, '.'. It identifies the current path.
    * 
    * @return returns true if this represents an empty path
    */
   boolean isEmpty();

   /**
    * Provides a canonical XPath expression. This is used for both
    * debugging and reporting. The path returned represents the 
    * original path that has been parsed to form the expression.
    * 
    * @return this returns the string format for the XPath
    */
   String toString();
}
