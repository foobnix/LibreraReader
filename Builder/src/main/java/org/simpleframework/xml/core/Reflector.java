/*
 * Reflector.java April 2007
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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * The <code>Reflector</code> object is used to determine the type
 * of a generic type. This is used when the type of an XML annotation
 * is not explicitly and the schema scanner needs to determine via
 * reflection what the generic parameters are of a specific type. In
 * particular this is used to determine the parameters within a list
 * annotated with the <code>ElementList</code> annotation. This also
 * has special handling for arrays within generic collections.
 * 
 * @author Niall Gallagher
 */
final class Reflector {
   
   /**
    * This method is used to acquire a generic parameter dependent 
    * from the specified field. This will acquire the field class and
    * attempt to extract the first generic parameter type from that  
    * field. If there is a generic parameter then the class of that 
    * parameter is returned from this method.
    * 
    * @param field this is the field to acquire the dependent class
    * 
    * @return this returns the generic parameter class declared
    */
   public static Class getDependent(Field field) {
      ParameterizedType type = getType(field);
      
      if(type != null) {
         return getClass(type);
      }
      return Object.class;
   }
   
   /**
    * This method is used to acquire generic parameter dependents 
    * from the specified field. This will acquire the field class and
    * attempt to extract all of the generic parameter types from that  
    * field. If there is a generic parameter then the class of that 
    * parameter is returned from this method.
    * 
    * @param field this is the field to acquire the dependent types
    * 
    * @return this returns the generic parameter classes declared
    */
   public static Class[] getDependents(Field field) {
      ParameterizedType type = getType(field);
      
      if(type != null) {
         return getClasses(type);
      }
      return new Class[]{};
   }
   
   /**
    * This is used to acquire the parameterized types from the given
    * field. If the field class has been parameterized then this will
    * return the parameters that have been declared on that class.
    * 
    * @param field this is the field to acquire the parameters from
    * 
    * @return this will return the parameterized types for the field
    */
   private static ParameterizedType getType(Field field) {
      Type type = field.getGenericType();
         
      if(type instanceof ParameterizedType) {
         return (ParameterizedType) type;
      }
      return null;      
   }
   
   /**
    * This method is used to acquire a generic parameter dependent 
    * from the method return type. This will acquire the return type
    * and attempt to extract the first generic parameter type from 
    * that type. If there is a generic parameter then the class of 
    * that parameter is returned from this method.
    * 
    * @param method this is the method to acquire the dependent of   
    * 
    * @return this returns the generic parameter class declared
    */   
   public static Class getReturnDependent(Method method) {
      ParameterizedType type = getReturnType(method);
      
      if(type != null) {
         return getClass(type);
      }
      return Object.class;
   }
   
   /**
    * This method is used to acquire a generic parameter dependent 
    * from the method return type. This will acquire the return type
    * and attempt to extract the first generic parameter type from 
    * that type. If there is a generic parameter then the class of 
    * that parameter is returned from this method.
    * 
    * @param method this is the method to acquire the dependent of   
    * 
    * @return this returns the generic parameter class declared
    */   
   public static Class[] getReturnDependents(Method method) {
      ParameterizedType type = getReturnType(method);
      
      if(type != null) {
         return getClasses(type);
      }
      return new Class[]{};
   }
   
   /**
    * This is used to acquire the parameterized types from the given
    * methods return class. If the return type class is parameterized
    * then this will return the parameters that have been declared on
    * that class, otherwise null is returned.
    * 
    * @param method this is the method to acquire the parameters from
    * 
    * @return this  returns the parameterized types for the method
    */
   private static ParameterizedType getReturnType(Method method) {
      Type type = method.getGenericReturnType();
      
      if(type instanceof ParameterizedType) {
         return (ParameterizedType) type;
      }
      return null;
   }
   
   /**
    * This method is used to acquire a generic parameter dependent 
    * from the specified parameter type. This will acquire the type
    * for the parameter at the specified index and attempt to extract
    * the first generic parameter type from that type. If there is a
    * generic parameter then the class of that parameter is returned
    * from this method, otherwise null is returned.
    * 
    * @param method this is the method to acquire the dependent of
    * @param index this is the index to acquire the parameter from    
    * 
    * @return this returns the generic parameter class declared
    */
   public static Class getParameterDependent(Method method, int index) {
      ParameterizedType type = getParameterType(method, index);
      
      if(type != null) {
         return getClass(type);
      }
      return Object.class;
   }
   
   /**
    * This method is used to acquire a generic parameter dependent 
    * from the specified parameter type. This will acquire the type
    * for the parameter at the specified index and attempt to extract
    * the first generic parameter type from that type. If there is a
    * generic parameter then the class of that parameter is returned
    * from this method, otherwise null is returned.
    * 
    * @param method this is the method to acquire the dependent of
    * @param index this is the index to acquire the parameter from    
    * 
    * @return this returns the generic parameter class declared
    */
   public static Class[] getParameterDependents(Method method, int index) {
      ParameterizedType type = getParameterType(method, index);
      
      if(type != null) {
         return getClasses(type);
      }
      return new Class[]{};
   }
   
   /**
    * This method is used to acquire a generic parameter dependent 
    * from the specified parameter type. This will acquire the type
    * for the parameter at the specified index and attempt to extract
    * the first generic parameter type from that type. If there is a
    * generic parameter then the class of that parameter is returned
    * from this method, otherwise null is returned.
    * 
    * @param factory this is the constructor to acquire the dependent
    * @param index this is the index to acquire the parameter from    
    * 
    * @return this returns the generic parameter class declared
    */
   public static Class getParameterDependent(Constructor factory, int index) {
      ParameterizedType type = getParameterType(factory, index);
      
      if(type != null) {
         return getClass(type);
      }
      return Object.class;
   }
   
   /**
    * This method is used to acquire a generic parameter dependent 
    * from the specified parameter type. This will acquire the type
    * for the parameter at the specified index and attempt to extract
    * the first generic parameter type from that type. If there is a
    * generic parameter then the class of that parameter is returned
    * from this method, otherwise null is returned.
    * 
    * @param factory this is the constructor to acquire the dependent 
    * @param index this is the index to acquire the parameter from    
    * 
    * @return this returns the generic parameter class declared
    */
   public static Class[] getParameterDependents(Constructor factory, int index) {
      ParameterizedType type = getParameterType(factory, index);
      
      if(type != null) {
         return getClasses(type);
      }
      return new Class[]{};
   }
   
   /**
    * This is used to acquire the parameterized types from the given
    * methods parameter class at the specified index position. If the
    * parameter class is parameterized this returns the parameters 
    * that have been declared on that class.
    * 
    * @param method this is the method to acquire the parameters from
    * @param index this is the index to acquire the parameter from     
    * 
    * @return this  returns the parameterized types for the method
    */
   private static ParameterizedType getParameterType(Method method, int index) {
      Type[] list = method.getGenericParameterTypes();
         
      if(list.length > index) {         
         Type type = list[index];
         
         if(type instanceof ParameterizedType) {
            return (ParameterizedType) type;
         }
      }
      return null;
   }
   
   /**
    * This is used to acquire the parameterized types from the given
    * constructors parameter class at the specified index position. If 
    * the parameter class is parameterized this returns the parameters 
    * that have been declared on that class.
    * 
    * @param factory this is constructor method to acquire the parameters 
    * @param index this is the index to acquire the parameter from     
    * 
    * @return this  returns the parameterized types for the method
    */
   private static ParameterizedType getParameterType(Constructor factory, int index) {
      Type[] list = factory.getGenericParameterTypes();
         
      if(list.length > index) {         
         Type type = list[index];
         
         if(type instanceof ParameterizedType) {
            return (ParameterizedType) type;
         }
      }
      return null;
   }
   
   /**
    * This is used to extract the class from the specified type. If
    * there are no actual generic type arguments to the specified
    * type then this will return null. Otherwise this will return 
    * the actual class, regardless of whether the class is an array.
    *  
    * @param type this is the type to extract the class from
    *  
    * @return this returns the class type from the first parameter
    */
   private static Class getClass(ParameterizedType type) {
      Type[] list = type.getActualTypeArguments();
      
      if(list.length > 0) {
         return getClass(list[0]);
      }      
      return null;      
   }
   
   /**
    * This is used to extract the class from the specified type. If
    * there are no actual generic type arguments to the specified
    * type then this will return null. Otherwise this will return 
    * the actual class, regardless of whether the class is an array.
    *  
    * @param type this is the type to extract the class from
    *  
    * @return this returns the class type from the first parameter
    */
   private static Class[] getClasses(ParameterizedType type) {
      Type[] list = type.getActualTypeArguments();
      Class[] types = new Class[list.length]; 
            
      for(int i = 0; i < list.length; i++) {
         types[i] = getClass(list[i]);
      }  
      return types;     
   }
   
   /**
    * This is used to extract the class from the specified type. If
    * there are no actual generic type arguments to the specified
    * type then this will return null. Otherwise this will return 
    * the actual class, regardless of whether the class is an array.
    *  
    * @param type this is the type to extract the class from
    *  
    * @return this returns the class type from the first parameter
    */
   private static Class getClass(Type type) {
      if(type instanceof Class) {
         return (Class) type;
      }
      return getGenericClass(type);
   }
   
   /**
    * This is used to extract the class from the specified type. If
    * there are no actual generic type arguments to the specified
    * type then this will return null. Otherwise this will return 
    * the actual class, regardless of whether the class is an array.
    *  
    * @param type this is the type to extract the class from
    *  
    * @return this returns the class type from the first parameter
    */
   private static Class getGenericClass(Type type) {
      if(type instanceof GenericArrayType) {
         return getArrayClass(type);
      }      
      return Object.class;
   }
   
   /**
    * This is used to extract an array class from the specified. If
    * a class can be extracted from the type then the array class 
    * is created by reflective creating a zero length array with 
    * the component type of the array and returning the array class.
    *  
    * @param type this is the type to extract the class from
    *  
    * @return this returns the class type from the array type
    */
   private static Class getArrayClass(Type type) {
      GenericArrayType generic = (GenericArrayType) type;
      Type array = generic.getGenericComponentType();
      Class entry = getClass(array);
      
      if(entry != null) {
         return Array.newInstance(entry, 0).getClass();
      }
      return null;
   }
   
   /**
    * This is used to acquire a bean name for a method or field name.
    * A bean name is the name of a method or field with the first
    * character decapitalized. An exception to this is when a method
    * or field starts with an acronym, in such a case the name will
    * remain unchanged from the original name.
    * 
    * @param name this is the name to convert to a bean name
    * 
    * @return this returns the bean value for the given name
    */
   public static String getName(String name) {
      int length = name.length();
      
      if(length > 0) {
         char[] array = name.toCharArray();
         char first = array[0];
      
         if(!isAcronym(array)) { 
            array[0] = toLowerCase(first);
         }
         return new String(array);
      }
      return name;
   }
   
   /**
    * This is used to determine if the provided array of characters
    * represents an acronym. The array of characters is considered
    * an acronym if the first and second characters are upper case.
    * 
    * @param array the array to evaluate whether it is an acronym
    * 
    * @return this returns true if the provided array is an acronym
    */
   private static boolean isAcronym(char[] array) {
      if(array.length < 2) {
         return false;
      }
      if(!isUpperCase(array[0])) {
         return false;
      }
      return isUpperCase(array[1]);
   }
   
   /**
    * This is used to convert the provided character to lower case.
    * The character conversion is done for all unicode characters. 
    * 
    * @param value this is the value that is to be converted
    * 
    * @return this returns the provided character in lower case
    */
   private static char toLowerCase(char value) {
      return Character.toLowerCase(value);
   }
   
   /**
    * This is used to determine if the provided character is an
    * upper case character. This can deal with unicode characters.
    * 
    * @param value this is the value that is to be evaluated
    * 
    * @return this returns true if the character is upper case
    */
   private static boolean isUpperCase(char value) {
      return Character.isUpperCase(value);
   }
}