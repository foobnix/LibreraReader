/*
 * InputPosition.java July 2006
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
 * The <code>InputPosition</code> object is used to acquire the line
 * number within the XML document. This allows debugging to be done
 * when a problem occurs with the source document. This object can 
 * be converted to a string using the <code>toString</code> method.
 *
 * @author Niall Gallagher
 */ 
class InputPosition implements Position {

   /**
    * This is the XML event that the position is acquired for.
    */         
   private EventNode source;
        
   /**
    * Constructor for the <code>InputPosition</code> object. This is
    * used to create a position description if the provided event 
    * is not null. This will return -1 if the specified event does
    * not provide any location information.    
    *
    * @param source this is the XML event to get the position of
    */ 
   public InputPosition(EventNode source) {
      this.source = source;
   }

   /**
    * This is the actual line number within the read XML document. 
    * The line number allows any problems within the source XML
    * document to be debugged if it does not match the schema. 
    * This will return -1 if the line number cannot be determined.
    *
    * @return this returns the line number of an XML event 
    */ 
   public int getLine() {  
      return source.getLine();
   }

   /**
    * This provides a textual description of the position the 
    * read cursor is at within the XML document. This allows the
    * position to be embedded within the exception thrown.
    *
    * @return this returns a textual description of the position
    */    
   public String toString() {
      return String.format("line %s", getLine());           
   }
}
