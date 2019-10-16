/*
 * Resolver.java February 2001
 *
 * Copyright (C) 2001, Niall Gallagher <niallg@users.sf.net>
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
 
package org.simpleframework.xml.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is used to store <code>Match</code> objects, which can then be
 * retrieved using a string by comparing that string to the pattern of
 * the <code>Match</code> objects. Patterns consist of characters
 * with either the '*' or '?' characters as wild characters. The '*'
 * character is completely wild meaning that is will match nothing or
 * a long sequence of characters. The '?' character matches a single
 * character.
 * <p>
 * If the '?' character immediately follows the '*' character then the
 * match is made as any sequence of characters up to the first match 
 * of the next character. For example "/*?/index.jsp" will match all 
 * files preceeded by only a single path. So "/pub/index.jsp" will
 * match, however "/pub/bin/index.jsp" will not, as it has two paths.
 * So, in effect the '*?' sequence will match anything or nothing up
 * to the first occurence of the next character in the pattern.
 * <p>
 * A design goal of the <code>Resolver</code> was to make it capable
 * of  high performance. In order to achieve a high performance the 
 * <code>Resolver</code> can cache the resolutions it makes so that if
 * the same text is given to the <code>Resolver.resolve</code> method 
 * a cached result can be retrived quickly which will decrease the 
 * length of time and work required to perform the match.
 * <p>
 * The semantics of the resolver are such that the last pattern added
 * with a wild string is the first one checked for a match. This means
 * that if a sequence of insertions like <code>add(x)</code> followed
 * by <code>add(y)</code> is made, then a <code>resolve(z)</code> will
 * result in a comparison to y first and then x, if z matches y then 
 * it is given as the result and if z does not match y and matches x 
 * then x is returned, remember if z matches both x and y then y will 
 * be the result due to the fact that is was the last pattern added.
 *
 * @author Niall Gallagher
 */
public class Resolver<M extends Match> extends AbstractSet<M> {

   /**
    * Caches the text resolutions made to reduce the work required.
    */        
   protected final Cache cache;

   /**
    * Stores the matches added to the resolver in resolution order.
    */ 
   protected final Stack stack;

   /**
    * The default constructor will create a <code>Resolver</code>
    * without a large cache size. This is intended for use when 
    * the requests for <code>resolve</code> tend to use strings
    * that are reasonably similar. If the strings issued to this
    * instance are dramatically different then the cache tends 
    * to be an overhead rather than a bonus.
    */
   public Resolver(){
      this.stack = new Stack();
      this.cache = new Cache(); 
   }

   /**
    * This will search the patterns in this <code>Resolver</code> to
    * see if there is a pattern in it that matches the string given.
    * This will search the patterns from the last entered pattern to
    * the first entered. So that the last entered patterns are the
    * most searched patterns and will resolve it first if it matches.
    *
    * @param text this is the string that is to be matched by this
    *
    * @return this will return the first match within the resolver
    */
   public M resolve(String text){
      List<M> list = cache.get(text);
      
      if(list == null) {
         list = resolveAll(text);
      }
      if(list.isEmpty()) {
         return null;
      }
      return list.get(0);
   }
   
   /**
    * This will search the patterns in this <code>Resolver</code> to
    * see if there is a pattern in it that matches the string given.
    * This will search the patterns from the last entered pattern to
    * the first entered. So that the last entered patterns are the
    * most searched patterns and will resolve it first if it matches.
    *
    * @param text this is the string that is to be matched by this
    *
    * @return this will return all of the matches within the resolver
    */
   public List<M> resolveAll(String text){
      List<M> list = cache.get(text);
      
      if(list != null) {
         return list;
      }
      char[] array = text.toCharArray();
  
      if(array == null) {
         return null;
      }
      return resolveAll(text, array);
   }
   
   /**
    * This will search the patterns in this <code>Resolver</code> to
    * see if there is a pattern in it that matches the string given.
    * This will search the patterns from the last entered pattern to
    * the first entered. So that the last entered patterns are the
    * most searched patterns and will resolve it first if it matches.
    *
    * @param text this is the string that is to be matched by this
    * @param array this is the character array of the text string
    *
    * @return this will return all of the matches within the resolver
    */
   private List<M> resolveAll(String text, char[] array){
      List<M> list = new ArrayList<M>();
      
      for(M match : stack) {
         String wild = match.getPattern();

         if(match(array, wild.toCharArray())){
            cache.put(text, list);
            list.add(match);
         }
      }
      return list;
   }

   /**
    * This inserts the <code>Match</code> implementation into the set
    * so that it can be used for resolutions. The last added match is
    * the first resolved. Because this changes the state of the 
    * resolver this clears the cache as it may affect resolutions.
    *
    * @param match this is the match that is to be inserted to this
    *
    * @return returns true if the addition succeeded, always true
    */ 
   public boolean add(M match) {
      stack.push(match);
      return true;
   }
   
   /**
    * This returns an <code>Iterator</code> that iterates over the
    * matches in insertion order. So the first match added is the
    * first retrieved from the <code>Iterator</code>. This order is
    * used to ensure that resolver can be serialized properly.
    *
    * @return returns an iterator for the sequence of insertion
    */ 
   public Iterator<M> iterator() {
      return stack.sequence();
   }

   /**
    * This is used to remove the <code>Match</code> implementation
    * from the resolver. This clears the cache as the removal of
    * a match may affect the resoultions existing in the cache. The
    * <code>equals</code> method of the match must be implemented.
    *
    * @param match this is the match that is to be removed
    *
    * @return true of the removal of the match was successful
    */ 
   public boolean remove(M match) {
      cache.clear();
      return stack.remove(match);
   }

   /**
    * Returns the number of matches that have been inserted into 
    * the <code>Resolver</code>. Although this is a set, it does 
    * not mean that matches cannot used the same pattern string.
    *
    * @return this returns the number of matches within the set
    */ 
   public int size() {
      return stack.size();           
   }
   
   /**
    * This is used to clear all matches from the set. This ensures
    * that the resolver contains no matches and that the resolution 
    * cache is cleared. This is used to that the set can be reused
    * and have new pattern matches inserted into it for resolution.
    */ 
   public void clear() {
      cache.clear();      
      stack.clear();
   }

   /**
    * This acts as a driver to the <code>match</code> method so that
    * the offsets can be used as zeros for the start of matching for 
    * the <code>match(char[],int,char[],int)</code>. method. This is
    * also used as the initializing driver for the recursive method.
    *
    * @param text this is the buffer that is to be resolved
    * @param wild this is the pattern that will be used
    */
   private boolean match(char[] text, char[] wild){
      return match(text, 0, wild, 0);
   }

   /**
    * This will be used to check to see if a certain buffer matches
    * the pattern if it does then it returns <code>true</code>. This
    * is a recursive method that will attempt to match the buffers 
    * based on the wild characters '?' and '*'. If there is a match
    * then this returns <code>true</code>.
    *
    * @param text this is the buffer that is to be resolved
    * @param off this is the read offset for the text buffer
    * @param wild this is the pattern that will be used
    * @param pos this is the read offset for the wild buffer
    */
   private boolean match(char[] text, int off, char[] wild, int pos){
      while(pos < wild.length && off < text.length){ /* examine chars */
         if(wild[pos] == '*'){
            while(wild[pos] == '*'){ /* totally wild */
               if(++pos >= wild.length) /* if finished */
                  return true;
            }
            if(wild[pos] == '?') { /* *? is special */
               if(++pos >= wild.length)                    
                  return true;
            }
            for(; off < text.length; off++){ /* find next matching char */
               if(text[off] == wild[pos] || wild[pos] == '?'){ /* match */
                  if(wild[pos - 1] != '?'){
                     if(match(text, off, wild, pos))
                        return true;
                  } else {
                     break;                          
                  }
               }
            }
            if(text.length == off)
               return false;
         }
         if(text[off++] != wild[pos++]){
            if(wild[pos-1] != '?')
               return false; /* if not equal */
         }
      }
      if(wild.length == pos){ /* if wild is finished */
          return text.length == off; /* is text finished */
      }
      while(wild[pos] == '*'){ /* ends in all stars */
         if(++pos >= wild.length) /* if finished */
            return true;
      }
      return false;
   }


   /**
    * This is used to cache resolutions made so that the matches can
    * be acquired the next time without performing the resolution.
    * This is an LRU cache so regardless of the number of resolutions
    * made this will not result in a memory leak for the resolver.
    * 
    * @author Niall Gallagher
    */ 
   private class Cache extends LimitedCache<List<M>> {

      /**
       * Constructor for the <code>Cache</code> object. This is a
       * constructor that creates the linked hash map such that 
       * it will purge the entries that are oldest within the map.
       */ 
      public Cache() {      
         super(1024);              
      }
   }

   /**
    * This is used to store the <code>Match</code> implementations in
    * resolution order. Resolving the match objects is performed so
    * that the last inserted match object is the first used in the
    * resolution process. This gives priority to the last inserted.
    * 
    * @author Niall Gallagher
    */ 
   private class Stack extends LinkedList<M> {     

      /**
       * The <code>push</code> method is used to push the match to 
       * the top of the stack. This also ensures that the cache is
       * cleared so the semantics of the resolver are not affected.
       *
       * @param match this is the match to be inserted to the stack
       */            
      public void push(M match) {
         cache.clear();
         addFirst(match);                       
      }

      /**
       * The <code>purge</code> method is used to purge a match from
       * the provided position. This also ensures that the cache is
       * cleared so that the semantics of the resolver do not change.
       *
       * @param index the index of the match that is to be removed
       */ 
      public void purge(int index) {
         cache.clear(); 
         remove(index);         
      }

      /**
       * This is returned from the <code>Resolver.iterator</code> so
       * that matches can be iterated in insertion order. When a
       * match is removed from this iterator then it clears the cache
       * and removed the match from the <code>Stack</code> object.
       * 
       * @return returns an iterator to iterate in insertion order
       */ 
      public Iterator<M> sequence() {
         return new Sequence();              
      }

      /**
       * The is used to order the <code>Match</code> objects in the
       * insertion order. Iterating in insertion order allows the
       * resolver object to be serialized and deserialized to and
       * from an XML document without disruption resolution order.
       *
       * @author Niall Gallagher
       */
      private class Sequence implements Iterator<M> {

         /**
          * The cursor used to acquire objects from the stack.
          */               
         private int cursor;

         /**
          * Constructor for the <code>Sequence</code> object. This is
          * used to position the cursor at the end of the list so the
          * first inserted match is the first returned from this.
          */ 
         public Sequence() {
            this.cursor = size();                 
         }

         /**
          * This returns the <code>Match</code> object at the cursor
          * position. If the cursor has reached the start of the 
          * list then this returns null instead of the first match.
          * 
          * @return this returns the match from the cursor position
          */ 
         public M next() {
            if(hasNext()) {
                return get(--cursor);
            }           
            return null;     
         }    

         /**
          * This is used to determine if the cursor has reached the
          * start of the list. When the cursor reaches the start of
          * the list then this method returns false.
          * 
          * @return this returns true if there are more matches left
          */ 
         public boolean hasNext() {
            return cursor > 0;
         }

         /**
          * Removes the match from the cursor position. This also
          * ensures that the cache is cleared so that resolutions
          * made before the removal do not affect the semantics.
          */ 
         public void remove() {                    
            purge(cursor);                
         }        
      }
   } 
}
