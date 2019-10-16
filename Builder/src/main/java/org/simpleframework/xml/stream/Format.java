/*
 * Format.java July 2006
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

import static org.simpleframework.xml.stream.Verbosity.HIGH;

/**
 * The <code>Format</code> object is used to provide information on 
 * how a generated XML document should be structured. The information
 * provided tells the formatter whether an XML prolog is required and
 * the number of spaces that should be used for indenting. The prolog
 * specified will be written directly before the XML document.
 * <p>
 * Should a <code>Format</code> be created with an indent of zero or
 * less then no indentation is done, and the generated XML will be on
 * the same line. The prolog can contain any legal XML heading, which
 * can domain a DTD declaration and XML comments if required.
 *
 * @author Niall Gallagher
 */ 
public class Format {

   /**
    * This is used to determine the verbosity preference of XML.
    */
   private final Verbosity verbosity;
   
   /**
    * Represents the prolog that appears in the generated XML.
    */         
   private final String prolog;
   
   /**
    * This is the style that is used internally by the format.
    */
   private final Style style;
         
   /**
    * Represents the indent size to use for the generated XML.
    */ 
   private final int indent;        

   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses an indent size of three.
    */ 
   public Format() {
      this(3);           
   }

   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified indent
    * size and a null prolog, which means no prolog is generated.
    *
    * @param indent this is the number of spaces used in the indent
    */ 
   public Format(int indent) {
      this(indent, null, new IdentityStyle());           
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified prolog 
    * that is to be inserted at the start of the XML document.
    *
    * @param prolog this is the prolog for the generated XML document
    */    
   public Format(String prolog) {
      this(3, prolog);          
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified indent
    * size and the text to use in the generated prolog.
    *
    * @param indent this is the number of spaces used in the indent
    * @param prolog this is the prolog for the generated XML document
    */    
   public Format(int indent, String prolog) {
      this(indent, prolog, new IdentityStyle());           
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified style
    * to style the attributes and elements of the XML document.
    * 
    * @param verbosity this indicates the verbosity of the format 
    */    
   public Format(Verbosity verbosity) {
      this(3, verbosity);         
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified style
    * to style the attributes and elements of the XML document.
    * 
    * @param indent this is the number of spaces used in the indent
    * @param verbosity this indicates the verbosity of the format 
    */    
   public Format(int indent, Verbosity verbosity) {
      this(indent, new IdentityStyle(), verbosity);         
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified style
    * to style the attributes and elements of the XML document.
    * 
    * @param style this is the style to apply to the format object
    */    
   public Format(Style style) {
      this(3, style);         
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified style
    * to style the attributes and elements of the XML document.
    * 
    * @param style this is the style to apply to the format object
    * @param verbosity this indicates the verbosity of the format 
    */    
   public Format(Style style, Verbosity verbosity) {
      this(3, style, verbosity);         
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified indent
    * size and the style provided to style the XML document.
    *
    * @param indent this is the number of spaces used in the indent
    * @param style this is the style to apply to the format object
    */    
   public Format(int indent, Style style) {
      this(indent, null, style);  
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified indent
    * size and the style provided to style the XML document.
    *
    * @param indent this is the number of spaces used in the indent
    * @param style this is the style to apply to the format object
    * @param verbosity this indicates the verbosity of the format  
    */    
   public Format(int indent, Style style, Verbosity verbosity) {
      this(indent, null, style, verbosity);  
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified indent
    * size and the text to use in the generated prolog.
    *
    * @param indent this is the number of spaces used in the indent
    * @param prolog this is the prolog for the generated XML document
    * @param style this is the style to apply to the format object
    */    
   public Format(int indent, String prolog, Style style) {
     this(indent, prolog, style, HIGH); 
   }
   
   /**
    * Constructor for the <code>Format</code> object. This creates an
    * object that is used to describe how the formatter should create
    * the XML document. This constructor uses the specified indent
    * size and the text to use in the generated prolog.
    *
    * @param indent this is the number of spaces used in the indent
    * @param prolog this is the prolog for the generated XML document
    * @param style this is the style to apply to the format object
    * @param verbosity this indicates the verbosity of the format
    */    
   public Format(int indent, String prolog, Style style, Verbosity verbosity) {
      this.verbosity = verbosity;
      this.prolog = prolog;           
      this.indent = indent;       
      this.style = style;
   }
   
   /**
    * This method returns the size of the indent to use for the XML
    * generated. The indent size represents the number of spaces that
    * are used for the indent, and indent of zero means no indenting.
    * 
    * @return returns the number of spaces to used for indenting
    */    
   public int getIndent() {
      return indent;            
   }   

   /**
    * This method returns the prolog that is to be used at the start
    * of the generated XML document. This allows a DTD or a version
    * to be specified at the start of a document. If this returns
    * null then no prolog is written to the start of the XML document.
    *
    * @return this returns the prolog for the start of the document    
    */ 
   public String getProlog() {
      return prolog;           
   }
   
   /**
    * This is used to acquire the <code>Style</code> for the format.
    * If no style has been set a default style is used, which does 
    * not modify the attributes and elements that are used to build
    * the resulting XML document.
    * 
    * @return this returns the style used for this format object
    */
   public Style getStyle() {
      return style;
   }
   
   /**
    * This method is used to indicate the preference of verbosity
    * for the resulting XML. This is typically used when default
    * serialization is used. It ensures that the various types
    * that are serialized are of either high or low verbosity.
    * 
    * @return this returns the verbosity preference for the XML
    */
   public Verbosity getVerbosity() {
      return verbosity;
   }
}