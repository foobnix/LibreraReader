/*
 * ScannerBuilder.java January 2010
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

package org.simpleframework.xml.convert;

import java.lang.annotation.Annotation;

import org.simpleframework.xml.util.ConcurrentCache;

/**
 * The <code>ScannerBuilder</code> is used to build and cache each
 * scanner requested. Building and caching scanners ensures that 
 * annotations can be acquired from a class quickly as a scan only 
 * needs to be performed once. Each scanner built scans the class 
 * provided as well as all the classes in the hierarchy.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.convert.ConverterScanner
 */
class ScannerBuilder extends ConcurrentCache<Scanner> {

   /**
    * Constructor for the <code>ScannerBuilder</code> object. This
    * will create a builder for annotation scanners. Each of the
    * scanners build will be cached internally to ensure that any
    * further requests for the scanner are quicker.
    */
   public ScannerBuilder() {
      super();
   }
   
   /**
    * This is used to build <code>Scanner</code> objects that are
    * used to scan the provided class for annotations. Each scanner
    * instance is cached once created to ensure it does not need to
    * be built twice, which improves the performance.
    * 
    * @param type this is the type to build a scanner object for
    * 
    * @return this will return a scanner instance for the given type
    */
   public Scanner build(Class<?> type) {
      Scanner scanner = get(type);
      
      if(scanner == null) {
         scanner = new Entry(type);
         put(type, scanner);
      }
      return scanner;
   }
    
   /**
    * The <code>Entry</code> object represents a scanner that is
    * used to scan a specified type for annotations. All annotations
    * scanned from the type are cached so that they do not need to
    * be looked up twice. This ensures scanning is much quicker.
    * 
    * @author Niall Gallagher
    */
   private static class Entry extends ConcurrentCache<Annotation> implements Scanner {
      
      /**
       * This class is the subject for all annotation scans performed.
       */
      private final Class root;
      
      /**
       * Constructor for the <code>Entry</code> object is used to 
       * create a scanner that will scan the specified type. All
       * annotations that are scanned are cached to ensure that they
       * do not need to be looked up twice. This ensures that scans
       * are quicker including ones that result in null.
       * 
       * @param root this is the root class that is to be scanned
       */
      public Entry(Class root) {
         this.root = root;
      }
      
      /**
       * This method will scan a class for the specified annotation. 
       * If the annotation is found on the class, or on one of the 
       * super types then it is returned. All scans will be cached 
       * to ensure scanning is only performed once.
       * 
       * @param type this is the annotation type to be scanned for
       * 
       * @return this will return the annotation if it is found
       */
      public <T extends Annotation> T scan(Class<T> type) {
         if(!contains(type)) {
            T value = find(type);
            
            if(type != null && value != null) {
               put(type, value);
            }
         }
         return (T)get(type);
      }
      
      /**
       * This method will scan a class for the specified annotation. 
       * If the annotation is found on the class, or on one of the 
       * super types then it is returned. All scans will be cached 
       * to ensure scanning is only performed once.
       * 
       * @param label this is the annotation type to be scanned for
       * 
       * @return this will return the annotation if it is found
       */
      private <T extends Annotation> T find(Class<T> label) {
         Class<?> type = root;
         
         while(type != null) {
            T value = type.getAnnotation(label);
            
            if(value != null) {
               return value;
            }
            type = type.getSuperclass();
         }
         return null;
      }
   }
}