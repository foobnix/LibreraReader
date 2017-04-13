/*
 * Stack.java July 2006
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

import java.util.ArrayList;

/**
 * The <code>Stack</code> object is used to provide a lightweight 
 * stack implementation. To ensure top performance this stack is not
 * synchronized and keeps track of elements using an array list. 
 * A null from either a <code>pop</code> or <code>top</code> means
 * that the stack is empty. This allows the stack to be peeked at
 * even if it has not been populated with anything yet.
 *
 * @author Niall Gallagher
 */ 
class Stack<T> extends ArrayList<T> {

   /**
    * Constructor for the <code>Stack</code> object. This is used 
    * to create a stack that can be used to keep track of values
    * in a first in last out manner. Typically this is used to 
    * determine if an XML element is in or out of context.
    * 
    * @param size this is the initial size of the stack to use
    */         
   public Stack(int size) {
      super(size);
   }

   /**
    * This is used to remove the element from the top of this 
    * stack. If the stack is empty then this will return null, as
    * such it is not advisable to push null elements on the stack.
    *
    * @return this returns the node element the top of the stack
    */ 
   public T pop() {
      int size = size();
      
      if(size <= 0) {
         return null;               
      }           
      return remove(size - 1);
   }
   
   /**
    * This is used to peek at the element from the top of this 
    * stack. If the stack is empty then this will return null, as
    * such it is not advisable to push null elements on the stack.
    *
    * @return this returns the node element the top of the stack
    */  
   public T top() {
      int size = size();
      
      if(size <= 0) {
         return null;              
      }           
      return get(size - 1);
   }
   
   /**
    * This is used to acquire the node from the bottom of the stack.
    * If the stack is empty then this will return null, as such it
    * is not advisable to push null elements on the stack.
    *
    * @return this returns the element from the bottom of the stack
    */ 
   public T bottom() {
      int size = size();
      
      if(size <= 0) {
         return null;              
      }           
      return get(0);           
   }
   
   /**
    * This method is used to add an element to the top of the stack. 
    * Although it is possible to add a null element to the stack it 
    * is not advisable, as null is returned when the stack is empty.
    *
    * @param value this is the element to add to the stack
    * 
    * @return this returns the actual node that has just been added
    */ 
   public T push(T value) {
      add(value);
      return value;
   }
}