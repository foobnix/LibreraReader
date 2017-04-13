/*
 * SignatureBuilder.java July 2009
 *
 * Copyright (C) 2009, Niall Gallagher <niallg@users.sf.net>
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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * The <code>SignatureBuilder</code> is used to build all permutations
 * of signatures a constructor contains. Permutations are calculated
 * by determining the number of annotations a parameter contains and
 * ensuring a signature is created with one of each. This is useful 
 * when a constructor is annotated with a union annotation.  
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Signature
 */
class SignatureBuilder {
   
   /**
    * This contains each parameter and annotation pair found.
    */
   private final ParameterTable table;
   
   /**
    * this is the constructor that this signature builder is for.
    */
   private final Constructor factory;
   
   /**
    * Constructor for the <code>SignatureBuilder</code> object. This
    * requires the constructor that the signatures will be built for.
    * If the constructor contains no annotations then no signatures
    * will be built, unless this is the default no-arg constructor.
    * 
    * @param factory this is the constructor to build for
    */
   public SignatureBuilder(Constructor factory) {
      this.table = new ParameterTable();
      this.factory = factory;
   }
   
   /**
    * This validates the builder by checking that the width of the
    * table is the same as the count of parameters in the constructor.
    * If there table width and parameter count does not match then 
    * this means the constructor is not fully annotated.
    * 
    * @return true if the constructor has been properly annotated
    */
   public boolean isValid() {
      Class[] types = factory.getParameterTypes();
      int width = table.width();
      
      return types.length == width;
   }

   /**
    * This will add a a parameter at the specified column in the
    * table. The parameter is typically added to the table at an
    * index mirroring the index it appears within the constructor.
    * 
    * @param value this is the parameter to be added in the table
    * @param index this is the index to added the parameter to
    */
   public void insert(Parameter value, int index) {
      table.insert(value, index);
   }
   
   /**
    * This method will build all the signatures for the constructor.
    * If a union annotation was used this may result in several 
    * signatures being created. Also if this builder represents
    * the default constructor then this returns a single value.
    * 
    * @return this returns the list of signatures to be built
    */
   public List<Signature> build() throws Exception {
      return build(new ParameterTable());
   }
   
   /**
    * This method will build all the signatures for the constructor.
    * If a union annotation was used this may result in several 
    * signatures being created. Also if this builder represents
    * the default constructor then this returns a single value.
    * 
    * @param matrix this is the matrix of parameters to collect
    * 
    * @return this returns the list of signatures to be built
    */
   private List<Signature> build(ParameterTable matrix) throws Exception {
      if(table.isEmpty()) {
         return create();
      } 
      build(matrix, 0);
      return create(matrix);
   }
   
   /**
    * This is used to create a list of signatures that represent 
    * the permutations of parameter and annotation pairs that
    * exist for a single constructor. The list may be empty.
    * 
    * @return this returns the list of signatures that exist
    */
   private List<Signature> create() throws Exception {
      List<Signature> list = new ArrayList<Signature>();
      Signature signature = new Signature(factory);
      
      if(isValid()) {
         list.add(signature);
      }
      return list;
   }
   
   /**
    * This is used to create a list of signatures that represent the
    * permutations of parameter and annotation pairs that exist. All
    * permutations are taken from the provided matrix. When building
    * the list of signature the parameter paths are validated.
    * 
    * @param matrix this contains the permutations of parameters
    * 
    * @return this returns the list of signatures for a constructor
    */
   private List<Signature> create(ParameterTable matrix) throws Exception {
      List<Signature> list = new ArrayList<Signature>();
      int height = matrix.height();
      int width = matrix.width();

      for(int i = 0; i < height; i++) {
         Signature signature = new Signature(factory);
         
         for(int j = 0; j < width; j++) {
            Parameter parameter = matrix.get(j, i);
            String path = parameter.getPath();
            Object key = parameter.getKey();
            
            if(signature.contains(key)) {
               throw new ConstructorException("Parameter '%s' is a duplicate in %s", path, factory);
            }
            signature.add(parameter);
         }
         list.add(signature);
      }
      return list;
   }
   
   /**
    * This is used to build all permutations of parameters that exist
    * within the constructor. By building a matrix of the permutations
    * all possible signatures can be created allowing for a better 
    * way to perform dependency injection for the objects.
    * 
    * @param matrix this is the matrix to hold the permutations
    * @param index this is the particular index to evaluate
    */
   private void build(ParameterTable matrix, int index) {
      build(matrix, new ParameterList(), index);
   }
   
   /**
    * This is used to build all permutations of parameters that exist
    * within the constructor. By building a matrix of the permutations
    * all possible signatures can be created allowing for a better 
    * way to perform dependency injection for the objects.
    * 
    * @param matrix this is the matrix to hold the permutations
    * @param signature the row to perform a permutation with
    * @param index this is the particular index to evaluate
    */
   private void build(ParameterTable matrix, ParameterList signature, int index) {
      ParameterList column = table.get(index);
      int height = column.size();
      int width = table.width();
      
      if(width - 1 > index) {
         for(int i = 0; i < height; i++) {
            ParameterList extended = new ParameterList(signature);
            
            if(signature != null) {
               Parameter parameter = column.get(i);
               
               extended.add(parameter);
               build(matrix, extended, index + 1);
            }
         }
      } else {
         populate(matrix, signature, index);
      }
   }
   
   /**
    * This is the final leg of building a permutation where a signature
    * is given to permutate on the last column. Once finished the
    * matrix will have a new row of parameters added which represents 
    * a new set of permutations to create signatures from.
    * 
    * @param matrix this is the matrix to hold the permutations
    * @param signature the row to perform a permutation with
    * @param index this is the particular index to evaluate
    */
   private void populate(ParameterTable matrix, ParameterList signature, int index) {
      ParameterList column = table.get(index);
      int width = signature.size();
      int height = column.size();

      for(int i = 0; i < height; i++) {
         for(int j = 0; j < width; j++) {
            ParameterList list = matrix.get(j);
            Parameter parameter = signature.get(j);
            
            list.add(parameter);
         }
         ParameterList list = matrix.get(index); 
         Parameter parameter = column.get(i);
         
         list.add(parameter);
      }
   }
   
   /**
    * The <code>ParameterTable</code> is used to build a table of 
    * parameters to represent a constructor. Each column of parameters
    * mirrors the index of the parameter in the constructor with each
    * parameter containing its type and the annotation it uses.
    * 
    * @author Niall Gallagher
    */
   private static class ParameterTable extends ArrayList<ParameterList> {
      
      /**
       * Constructor for the <code>ParameterTable</code> object. This
       * creates a table of parameters that can be used to represent
       * each permutation of parameter and annotation pairs.
       */
      public ParameterTable() {
         super();
      }
      
      /**
       * This represents the number of parameters for each index in
       * the table. This is determined from the first column within
       * the table, if the table is empty this returns zero.
       * 
       * @return the height of the table using the first column
       */
      private int height() {
         int width = width();
         
         if(width > 0) {
            return get(0).size();
         }
         return 0;
      }
      
      /**
       * This is used to determine the width of this table. The width
       * is the number of columns the table contains. Each column in
       *  represents a parameter at its index in the constructor.
       * 
       * @return this returns the width of the table 
       */
      private int width() {
         return size();
      }
      
      /**
       * This will add a a parameter at the specified column in the
       * table. The parameter is typically added to the table at an
       * index mirroring the index it appears within the constructor.
       * 
       * @param value this is the parameter to be added in the table
       * @param column this is the index to added the parameter to
       */
      public void insert(Parameter value, int column) {
         ParameterList list = get(column);
         
         if(list != null) {
            list.add(value);
         }
      }
      
      /**
       * This is used to acquire the column of parameters from the
       * table. If no column exists at the specified index then one
       * is created and added to the table at the column index.
       * 
       * @param column this is the column to acquire from the table
       * 
       * @return the column of parameters from the table
       */
      public ParameterList get(int column) {
         int size = size();
         
         for(int i = size; i <= column; i++) {
            ParameterList list = new ParameterList();
            add(list);
         }
         return super.get(column);
      }
      
      /**
       * This is used to get a <code>Parameter</code> at the row and
       * column specified. This if the parameter does not exist the
       * an index out of bounds exception is thrown.
       * 
       * @param column the column to acquire the parameter for
       * @param row the row to acquire the parameter for
       * 
       * @return this returns the parameter at the specified cell
       */
      public Parameter get(int column, int row) {
         return get(column).get(row);
      }
   }
   
   /**
    * The <code>ParameterList</code> object is used to represent a 
    * column of parameters within a table. A column will contain each
    * parameter and annotation pair extracted from an index in the
    * constructor, a permutation can come from a union annotation,
    * 
    * @author Niall Gallagher
    */
   private static class ParameterList extends ArrayList<Parameter> {
      
      /**
       * Constructor for the <code>ParameterList</code> object. 
       * This creates a default list for building a column for
       * the parameter table it is added to.
       */
      public ParameterList() {
         super();
      }
      
      /**
       * Constructor for the <code>ParameterList</code> object. 
       * This creates a list of parameters by using the provided
       * list of parameters, each parameter will be added in order.
       */
      public ParameterList(ParameterList list) {
         super(list);
      }
   }
}