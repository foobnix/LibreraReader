/*
 * Composite.java July 2006
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

package org.simpleframework.xml.core;

import org.simpleframework.xml.Version;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NamespaceMap;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;
import org.simpleframework.xml.stream.Position;

/**
 * The <code>Composite</code> object is used to perform serialization
 * of objects that contain XML annotations. Composite objects are objects
 * that are not primitive and contain references to serializable fields.
 * This <code>Converter</code> will visit each field within the object
 * and deserialize or serialize that field depending on the requested
 * action. If a required field is not present when deserializing from
 * an XML element this terminates and deserialization reports the error.
 * <pre>
 * 
 *    &lt;element name="test" class="some.package.Type"&gt;
 *       &lt;text&gt;string value&lt;/text&gt;
 *       &lt;integer&gt;1234&lt;/integer&gt;
 *    &lt;/element&gt;
 * 
 * </pre>
 * To deserialize the above XML source this will attempt to match the
 * attribute name with an <code>Attribute</code> annotation from the
 * XML schema class, which is specified as "some.package.Type". This
 * type must also contain <code>Element</code> annotations for the
 * "text" and "integer" elements.
 * <p>
 * Serialization requires that contacts marked as required must have
 * values that are not null. This ensures that the serialized object
 * can be deserialized at a later stage using the same class schema.
 * If a required value is null the serialization terminates and an
 * exception is thrown.
 * 
 * @author Niall Gallagher
 */
class Composite implements Converter {
  
   /**
    * This factory creates instances of the deserialized object.
    */
   private final ObjectFactory factory;
   
   /**
    * This is used to convert any primitive values that are needed.
    */
   private final Primitive primitive;
   
   /**
    * This is used to store objects so that they can be read again.
    */
   private final Criteria criteria;
   
   /**
    * This is the current revision of this composite converter.
    */
   private final Revision revision; 
   
   /**
    * This is the source object for the instance of serialization.
    */
   private final Context context;
   
   /**
    * This is the type that this composite produces instances of.
    */
   private final Type type;
   
   /**
    * Constructor for the <code>Composite</code> object. This creates 
    * a converter object capable of serializing and deserializing root
    * objects labeled with XML annotations. The XML schema class must 
    * be given to the instance in order to perform deserialization.
    *  
    * @param context the source object used to perform serialization
    * @param type this is the XML schema type to use for this
    */
   public Composite(Context context, Type type) {
      this(context, type, null);
   }
        
   /**
    * Constructor for the <code>Composite</code> object. This creates 
    * a converter object capable of serializing and deserializing root
    * objects labeled with XML annotations. The XML schema class must 
    * be given to the instance in order to perform deserialization.
    *  
    * @param context the source object used to perform serialization
    * @param type this is the XML schema type to use for this
    * @param override this is the override type declared for this
    */
   public Composite(Context context, Type type, Class override) {
      this.factory = new ObjectFactory(context, type, override);  
      this.primitive = new Primitive(context, type);
      this.criteria = new Collector();
      this.revision = new Revision();
      this.context = context;
      this.type = type;
   }

   /**
    * This <code>read</code> method performs deserialization of the XML
    * schema class type by traversing the contacts and instantiating them
    * using details from the provided XML element. Because this will
    * convert a non-primitive value it delegates to other converters to
    * perform deserialization of lists and primitives.
    * <p>
    * If any of the required contacts are not present within the provided
    * XML element this will terminate deserialization and throw an
    * exception. The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are deserialized from
    * 
    * @return this returns the fully deserialized object graph
    */
   public Object read(InputNode node) throws Exception {
      Instance value = factory.getInstance(node); 
      Class type = value.getType(); 
      
      if(value.isReference()) {      
         return value.getInstance(); 
      }
      if(context.isPrimitive(type)) { 
         return readPrimitive(node, value);
      }
      return read(node, value, type);
   }
   
   /**
    * This <code>read</code> method performs deserialization of the XML
    * schema class type by traversing the contacts and instantiating them
    * using details from the provided XML element. Because this will
    * convert a non-primitive value it delegates to other converters to
    * perform deserialization of lists and primitives.
    * <p>
    * If any of the required contacts are not present within the provided
    * XML element this will terminate deserialization and throw an
    * exception. The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are deserialized from
    * @param source the object whose contacts are to be deserialized
    * 
    * @return this returns the fully deserialized object graph 
    */
   public Object read(InputNode node, Object source) throws Exception {
      Class type = source.getClass();
      Schema schema = context.getSchema(type);
      Caller caller = schema.getCaller();
      
      read(node, source, schema); 
      criteria.commit(source);
      caller.validate(source);
      caller.commit(source);
      
      return readResolve(node, source, caller);
   }
   
   /**
    * This <code>read</code> method performs deserialization of the XML
    * schema class type by traversing the contacts and instantiating them
    * using details from the provided XML element. Because this will
    * convert a non-primitive value it delegates to other converters to
    * perform deserialization of lists and primitives.
    * <p>
    * If any of the required contacts are not present within the provided
    * XML element this will terminate deserialization and throw an
    * exception. The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are deserialized from
    * @param value this is the instance for the object within the graph
    * @param real this is the real type that is to be evaluated
    * 
    * @return this returns the fully deserialized object graph
    */
   private Object read(InputNode node, Instance value, Class real) throws Exception {
      Schema schema = context.getSchema(real);
      Caller caller = schema.getCaller();
      Builder builder = read(schema, value);
      Object source = builder.read(node);
      
      caller.validate(source); 
      caller.commit(source); 
      value.setInstance(source);
      
      return readResolve(node, source, caller);    
   }
   
   /**
    * This <code>read</code> method performs deserialization of the XML
    * schema class type by traversing the contacts and instantiating them
    * using details from the provided XML element. Because this will
    * convert a non-primitive value it delegates to other converters to
    * perform deserialization of lists and primitives.
    * <p>
    * If any of the required contacts are not present within the provided
    * XML element this will terminate deserialization and throw an
    * exception. The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are deserialized from
    * @param schema this is the schema for the class to be deserialized
    * @param value this is the value used for the deserialization
    * 
    * @return this returns the fully deserialized object graph
    */
   private Builder read(Schema schema, Instance value) throws Exception {
      Instantiator creator = schema.getInstantiator();
      
      if(creator.isDefault()) {
         return new Builder(this, criteria, schema, value);
      } 
      return new Injector(this, criteria, schema, value);
   }

   /**
    * This <code>readPrimitive</code> method will extract the text value
    * from the node and replace any template variables before converting
    * it to a primitive value. This uses a <code>Primitive</code> object
    * to convert the node text to the resulting string. This will also
    * respect all references on the node so cycle can be followed.
    *
    * @param node this is the node to be converted to a primitive
    * @param value this is the type for the object within the graph
    *
    * @return this returns the primitive that has been deserialized
    */ 
   private Object readPrimitive(InputNode node, Instance value) throws Exception {
      Class type = value.getType();
      Object result = primitive.read(node, type);

      if(type != null) {
         value.setInstance(result);
      }
      return result;
   }
   
   /**
    * The <code>readResolve</code> method is used to determine if there 
    * is a resolution method which can be used to substitute the object
    * deserialized. The resolve method is used when an object wishes 
    * to provide a substitute within the deserialized object graph.
    * This acts as an equivalent to the Java Object Serialization
    * <code>readResolve</code> method for the object deserialization.
    * 
    * @param node the XML element object provided as a replacement
    * @param source the type of the object that is being deserialized
    * @param caller this is used to invoke the callback methods
    * 
    * @return this returns a replacement for the deserialized object
    */
   private Object readResolve(InputNode node, Object source, Caller caller) throws Exception {
      if(source != null) {
         Position line = node.getPosition();
         Object value = caller.resolve(source);
         Class expect = type.getType();
         Class real = value.getClass();
      
         if(!expect.isAssignableFrom(real)) {
            throw new ElementException("Type %s does not match %s at %s", real, expect, line);              
         }
         return value;
      }
      return source;
   }
   
   /**
    * This <code>read</code> method performs deserialization of the XML
    * schema class type by traversing the contacts and instantiating them
    * using details from the provided XML element. Because this will
    * convert a non-primitive value it delegates to other converters to
    * perform deserialization of lists and primitives.
    * <p>
    * If any of the required contacts are not present within the provided
    * XML element this will terminate deserialization and throw an
    * exception. The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are deserialized from
    * @param source this type of the object that is to be deserialized
    * @param schema this object visits the objects contacts
    */
   private void read(InputNode node, Object source, Schema schema) throws Exception {
      Section section = schema.getSection();
      
      readVersion(node, source, schema);
      readSection(node, source, section);
   }   
   
   /**
    * This <code>readSection</code> method performs deserialization of a
    * schema class type by traversing the contacts and instantiating them
    * using details from the provided XML element. Because this will
    * convert a non-primitive value it delegates to other converters to
    * perform deserialization of lists and primitives.
    * <p>
    * If any of the required contacts are not present within the provided
    * XML element this will terminate deserialization and throw an
    * exception. The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are deserialized from
    * @param source this type of the object that is to be deserialized
    * @param section this is the XML section that contains the structure
    */
   private void readSection(InputNode node, Object source, Section section) throws Exception {
      readText(node, source, section);
      readAttributes(node, source, section);
      readElements(node, source, section);
   }
   
   /**
    * This method is used to read the version from the provided input
    * node. Once the version has been read it is used to determine how
    * to deserialize the object. If the version is not the initial
    * version then it is read in a manner that ignores excessive XML
    * elements and attributes. Also none of the annotated fields or
    * methods are required if the version is not the initial version.
    * 
    * @param node the XML element contact values are deserialized from
    * @param source this object whose contacts are to be deserialized
    * @param schema this object visits the objects contacts
    */
   private void readVersion(InputNode node, Object source, Schema schema) throws Exception {
      Label label = schema.getVersion();
      Class expect = type.getType();
      
      if(label != null) {
         String name = label.getName();
         NodeMap<InputNode> map = node.getAttributes();
         InputNode value = map.remove(name);
         
         if(value != null) {
            readVersion(value, source, label);
         } else {
            Version version = context.getVersion(expect);
            Double start = revision.getDefault();
            Double expected = version.revision();
            
            criteria.set(label, start);
            revision.compare(expected, start);
         }
      }
   }
   
   /**
    * This method is used to read the version from the provided input
    * node. Once the version has been read it is used to determine how
    * to deserialize the object. If the version is not the initial
    * version then it is read in a manner that ignores excessive XML
    * elements and attributes. Also none of the annotated fields or
    * methods are required if the version is not the initial version.
    * 
    * @param node the XML element contact values are deserialized from
    * @param source the type of the object that is being deserialized
    * @param label this is the label used to read the version attribute
    */
   private void readVersion(InputNode node, Object source, Label label) throws Exception {
      Object value = readInstance(node, source, label);
      Class expect = type.getType();
     
      if(value != null) {
         Version version = context.getVersion(expect);
         Double actual = version.revision();
         
         if(!value.equals(revision)) {
            revision.compare(actual, value);
         }
      } 
   }

   /**
    * This <code>readAttributes</code> method reads the attributes from
    * the provided XML element. This will iterate over all attributes
    * within the element and convert those attributes as primitives to
    * contact values within the source object.
    * <p>
    * Once all attributes within the XML element have been evaluated
    * the <code>Schema</code> is checked to ensure that there are no
    * required contacts annotated with the <code>Attribute</code> that
    * remain. If any required attribute remains an exception is thrown. 
    * 
    * @param node this is the XML element to be evaluated
    * @param source the type of the object that is being deserialized
    * @param section this is the XML section that contains the structure
    */
   private void readAttributes(InputNode node, Object source, Section section) throws Exception {
      NodeMap<InputNode> list = node.getAttributes();
      LabelMap map = section.getAttributes();

      for(String name : list) {         
         InputNode value = node.getAttribute(name);
         
         if(value != null) {
            readAttribute(value, source, section, map);
         }
      }  
      validate(node, map, source);
   }

   /**
    * This <code>readElements</code> method reads the elements from 
    * the provided XML element. This will iterate over all elements
    * within the element and convert those elements to primitives or
    * composite objects depending on the contact annotation.
    * <p>
    * Once all elements within the XML element have been evaluated
    * the <code>Schema</code> is checked to ensure that there are no
    * required contacts annotated with the <code>Element</code> that
    * remain. If any required element remains an exception is thrown. 
    * 
    * @param node this is the XML element to be evaluated
    * @param source the type of the object that is being deserialized
    * @param section the XML section that contains the structure
    */
   private void readElements(InputNode node, Object source, Section section) throws Exception {
      LabelMap map = section.getElements();
      InputNode child = node.getNext();
      
      while(child != null) {         
         String name = child.getName();
         Section block = section.getSection(name);         
         
         if(block != null) {
            readSection(child, source, block);
         } else { 
            readElement(child, source, section, map);
         }
         child = node.getNext();
      } 
      validate(node, map, source);
   }
   
   /**
    * This <code>readText</code> method is used to read the text value
    * from the XML element node specified. This will check the class
    * schema to determine if a <code>Text</code> annotation was
    * specified. If one was specified then the text within the XML
    * element input node is used to populate the contact value.
    * 
    * @param node this is the XML element to acquire the text from
    * @param source the type of the object that is being deserialized
    * @param section this is used to visit the element contacts
    */
   private void readText(InputNode node, Object source, Section section) throws Exception {
      Label label = section.getText();
      
      if(label != null) {
         readInstance(node, source, label);
      }
   }
   
   /**
    * This <code>readAttribute</code> method is used for deserialization
    * of the provided node object using a delegate converter. This is
    * typically another <code>Composite</code> converter, or if the
    * node is an attribute a <code>Primitive</code> converter. When
    * the delegate converter has completed the deserialized value is
    * assigned to the contact.
    * 
    * @param node this is the node that contains the contact value
    * @param source the type of the object that is being deserialized
    * @param section this is the section to read the attribute from
    * @param map this is the map that contains the label objects
    */
   private void readAttribute(InputNode node, Object source, Section section, LabelMap map) throws Exception {
      String name = node.getName();
      String path = section.getAttribute(name);
      Label label = map.getLabel(path);
      
      if(label == null) {
         Position line = node.getPosition();
         Class expect = context.getType(type, source);

         if(map.isStrict(context) && revision.isEqual()) {              
            throw new AttributeException("Attribute '%s' does not have a match in %s at %s", path, expect, line);
         }            
      } else {
         readInstance(node, source, label);
      }         
   }

   /**
    * This <code>readElement</code> method is used for deserialization
    * of the provided node object using a delegate converter. This is
    * typically another <code>Composite</code> converter, or if the
    * node is an attribute a <code>Primitive</code> converter. When
    * the delegate converter has completed the deserialized value is
    * assigned to the contact.
    * 
    * @param node this is the node that contains the contact value
    * @param source the type of the object that is being deserialized
    * @param section this is the section to read the element from
    * @param map this is the map that contains the label objects
    */
   private void readElement(InputNode node, Object source, Section section, LabelMap map) throws Exception {
      String name = node.getName();
      String path = section.getPath(name); 
      Label label = map.getLabel(path);      

      if(label == null) {
         label = criteria.resolve(path);
      }
      if(label == null) {
         Position line = node.getPosition();
         Class expect = context.getType(type, source);
         
         if(map.isStrict(context) && revision.isEqual()) {              
            throw new ElementException("Element '%s' does not have a match in %s at %s", path, expect, line);
         } else {
            node.skip();                 
         }
      } else {
         readUnion(node, source, map, label);
      }         
   }
   
   /**
    * The <code>readUnion</code> method is determine the unions 
    * for a particular label and set the value of that union to
    * the same value as the label. This helps the deserialization 
    * process by ensuring once a union is read it is not
    * replaced. This is also required when reading inline lists.
    * 
    * @param node this is the XML element to read the elements from
    * @param source this is the instance to read the unions from
    * @param map this is the label map associated with the label
    * @param label this is the label used to define the XML element
    */
   private void readUnion(InputNode node, Object source, LabelMap map, Label label) throws Exception {
      Object value = readInstance(node, source, label);
      String[] list = label.getPaths();
      
      for(String key : list) {
         map.getLabel(key);
      }
      if(label.isInline()) {
         criteria.set(label, value);
      }
   }

   /**
    * This <code>read</code> method is used to perform deserialization
    * of the provided node object using a delegate converter. This is
    * typically another <code>Composite</code> converter, or if the
    * node is an attribute a <code>Primitive</code> converter. When
    * the delegate converter has completed the deserialized value is
    * assigned to the contact.
    * 
    * @param node this is the node that contains the contact value
    * @param source the type of the object that is being deserialized
    * @param label this is the label used to create the converter
    */
   private Object readInstance(InputNode node, Object source, Label label) throws Exception {    
      Object object = readVariable(node, source, label);
    
      if(object == null) {     
         Position line = node.getPosition();
         Class expect = context.getType(type, source);
         
         if(label.isRequired() && revision.isEqual()) {              
            throw new ValueRequiredException("Empty value for %s in %s at %s", label, expect, line);
         }
      } else {
         if(object != label.getEmpty(context)) {  
            criteria.set(label, object);
         }
      }
      return object;
   }  
   
   /**
    * This <code>readObject</code> method is used to perform the
    * deserialization of the XML in to any original value. If there
    * is no original value then this will do a read and instantiate
    * a new value to deserialize in to. Reading in to the original
    * ensures that existing lists or maps can be read in to.
    * 
    * @param node this is the node that contains the contact value
    * @param source the source object to assign the contact value to
    * @param label this is the label used to create the converter
    * 
    * @return this returns the original value deserialized in to
    */
   private Object readVariable(InputNode node, Object source, Label label) throws Exception {    
      Converter reader = label.getConverter(context);   
      
      if(label.isCollection()) {
         Variable variable = criteria.get(label);
         Contact contact = label.getContact();
         
         if(variable != null) {
            Object value = variable.getValue();

            return reader.read(node, value);
         }
         if(source != null) {
            Object value = contact.get(source);
         
            if(value != null) {
               return reader.read(node, value);
            }
         }
      }
      return reader.read(node);
   }

   /**
    * This method checks to see if there are any <code>Label</code>
    * objects remaining in the provided map that are required. This is
    * used when deserialization is performed to ensure the the XML
    * element deserialized contains sufficient details to satisfy the
    * XML schema class annotations. If there is a required label that
    * remains it is reported within the exception thrown.
    * 
    * @param node this is the node that contains the contact value
    * @param map this is the map to check for remaining labels
    * @param source this is the object that has been deserialized 
    */
   private void validate(InputNode node, LabelMap map, Object source) throws Exception {
      Class expect = context.getType(type, source);
      Position line = node.getPosition();

      for(Label label : map) {
         if(label.isRequired() && revision.isEqual()) {
            throw new ValueRequiredException("Unable to satisfy %s for %s at %s", label, expect, line);
         }
         Object value = label.getEmpty(context);
         
         if(value != null) {
            criteria.set(label, value);
         }
      }      
   }
   
   /**
    * This <code>validate</code> method performs validation of the XML
    * schema class type by traversing the contacts and validating them
    * using details from the provided XML element. Because this will
    * validate a non-primitive value it delegates to other converters 
    * to perform validation of lists, maps, and primitives.
    * <p>
    * If any of the required contacts are not present within the given
    * XML element this will terminate validation and throw an exception
    * The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are validated from
    * 
    * @return true if the XML element matches the XML schema class given 
    */
   public boolean validate(InputNode node) throws Exception {      
      Instance value = factory.getInstance(node);
      
      if(!value.isReference()) {
         Object result = value.setInstance(null);
         Class type = value.getType();
            
         return validate(node, type);
      }
      return true;  
   }
   
   /**
    * This <code>validate</code> method performs validation of the XML
    * schema class type by traversing the contacts and validating them
    * using details from the provided XML element. Because this will
    * validate a non-primitive value it delegates to other converters 
    * to perform validation of lists, maps, and primitives.
    * <p>
    * If any of the required contacts are not present within the given
    * XML element this will terminate validation and throw an exception
    * The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are validated from
    * @param type this is the type to validate against the input node
    */
   private boolean validate(InputNode node, Class type) throws Exception {
      Schema schema = context.getSchema(type);
      Section section = schema.getSection();
      
      validateText(node, schema);
      validateSection(node, section);
      
      return node.isElement();
   }
   
   /**
    * This <code>validateSection</code> method performs validation of a
    * schema class type by traversing the contacts and validating them
    * using details from the provided XML element. Because this will
    * validate a non-primitive value it delegates to other converters 
    * to perform validation of lists, maps, and primitives.
    * <p>
    * If any of the required contacts are not present within the given
    * XML element this will terminate validation and throw an exception
    * The annotation missing is reported in the exception.
    * 
    * @param node the XML element contact values are validated from
    * @param section this is the section that defines the XML structure
    */
   private void validateSection(InputNode node, Section section) throws Exception {
      validateAttributes(node, section);
      validateElements(node, section);
   }

   /**
    * This <code>validateAttributes</code> method validates the attributes 
    * from the provided XML element. This will iterate over all attributes
    * within the element and validate those attributes as primitives to
    * contact values within the source object.
    * <p>
    * Once all attributes within the XML element have been evaluated the
    * <code>Schema</code> is checked to ensure that there are no required 
    * contacts annotated with the <code>Attribute</code> that remain. If 
    * any required attribute remains an exception is thrown. 
    * 
    * @param node this is the XML element to be validated
    * @param section this is the section that defines the XML structure
    */
   private void validateAttributes(InputNode node, Section section) throws Exception {
      NodeMap<InputNode> list = node.getAttributes();
      LabelMap map = section.getAttributes();

      for(String name : list) {         
         InputNode value = node.getAttribute(name);
         
         if(value != null) {
            validateAttribute(value, section, map);
         }
      }  
      validate(node, map);
   }

   /**
    * This <code>validateElements</code> method validates the elements 
    * from the provided XML element. This will iterate over all elements
    * within the element and validate those elements as primitives or
    * composite objects depending on the contact annotation.
    * <p>
    * Once all elements within the XML element have been evaluated
    * the <code>Schema</code> is checked to ensure that there are no
    * required contacts annotated with the <code>Element</code> that
    * remain. If any required element remains an exception is thrown.
    * 
    * @param node this is the XML element to be evaluated
    * @param section this is the section that defines the XML structure
    */
   private void validateElements(InputNode node, Section section) throws Exception {
      LabelMap map = section.getElements();
      InputNode next = node.getNext();
      
      while(next != null) {         
         String name = next.getName();
         Section child = section.getSection(name);         
         
         if(child != null) {
            validateSection(next, child);
         } else {         
            validateElement(next, section, map);
         }
         next = node.getNext();
      } 
      validate(node, map);
   }
   
   /**
    * This <code>validateText</code> method validates the text value 
    * from the XML element node specified. This will check the class
    * schema to determine if a <code>Text</code> annotation was used. 
    * If one was specified then the text within the XML element input 
    * node is checked to determine if it is a valid entry.
    * 
    * @param node this is the XML element to acquire the text from
    * @param schema this is used to visit the element contacts
    */
   private void validateText(InputNode node, Schema schema) throws Exception {
      Label label = schema.getText();
      
      if(label != null) {
         validate(node, label);
      }
   }
   
   /**
    * This <code>validateAttribute</code> method performs a validation
    * of the provided node object using a delegate converter. This is
    * typically another <code>Composite</code> converter, or if the
    * node is an attribute a <code>Primitive</code> converter. If this
    * fails validation then an exception is thrown to report the issue.
    * 
    * @param node this is the node that contains the contact value
    * @param section this is the section to validate this attribute in
    * @param map this is the map that contains the label objects
    */
   private void validateAttribute(InputNode node, Section section, LabelMap map) throws Exception {
      Position line = node.getPosition();
      String name = node.getName();
      String path = section.getAttribute(name);
      Label label = map.getLabel(path);
      
      if(label == null) {
         Class expect = type.getType();
         
         if(map.isStrict(context) && revision.isEqual()) {              
            throw new AttributeException("Attribute '%s' does not exist for %s at %s", path, expect, line);
         }            
      } else {
         validate(node, label);
      }         
   }

   /**
    * This <code>validateElement</code> method performs a validation
    * of the provided node object using a delegate converter. This is
    * typically another <code>Composite</code> converter, or if the
    * node is an attribute a <code>Primitive</code> converter. If this
    * fails validation then an exception is thrown to report the issue.
    * 
    * @param node this is the node that contains the contact value
    * @param section this is the section to validate this element in
    * @param map this is the map that contains the label objects
    */
   private void validateElement(InputNode node, Section section, LabelMap map) throws Exception {
      String name = node.getName();
      String path = section.getPath(name);
      Label label = map.getLabel(path);      

      if(label == null) {
         label = criteria.resolve(path);
      }
      if(label == null) {
         Position line = node.getPosition();
         Class expect = type.getType();
         
         if(map.isStrict(context) && revision.isEqual()) {              
            throw new ElementException("Element '%s' does not exist for %s at %s", path, expect, line);
         } else {
            node.skip();                 
         }
      } else {
         validateUnion(node, map, label);
      }         
   }
   
   /**
    * The <code>validateUnion</code> method is determine the unions 
    * for a particular label and set the value of that union to
    * the same value as the label. This helps the deserialization 
    * process by ensuring once a union is validated it is not
    * replaced. This is also required when validating inline lists.
    * 
    * @param node this is the XML element to read the elements from
    * @param map this is the label map associated with the label
    * @param label this is the label used to define the XML element
    */
   private void validateUnion(InputNode node, LabelMap map, Label label) throws Exception {
      String[] list = label.getPaths();
      
      for(String key : list) {
         map.getLabel(key);
      }
      if(label.isInline()) {
         criteria.set(label, null);
      }
      validate(node, label);
   }
   
   /**
    * This <code>validate</code> method is used to perform validation
    * of the provided node object using a delegate converter. This is
    * typically another <code>Composite</code> converter, or if the
    * node is an attribute a <code>Primitive</code> converter. If this
    * fails validation then an exception is thrown to report the issue.
    * 
    * @param node this is the node that contains the contact value
    * @param label this is the label used to create the converter
    */
   private void validate(InputNode node, Label label) throws Exception {    
      Converter reader = label.getConverter(context);      
      Position line = node.getPosition();
      Class expect = type.getType();
      boolean valid = reader.validate(node);
    
      if(valid == false) {     
        throw new PersistenceException("Invalid value for %s in %s at %s", label, expect, line);
      }
      criteria.set(label, null);
   }

   /**
    * This method checks to see if there are any <code>Label</code>
    * objects remaining in the provided map that are required. This is
    * used when validation is performed to ensure the the XML element 
    * validated contains sufficient details to satisfy the XML schema 
    * class annotations. If there is a required label that remains it 
    * is reported within the exception thrown.
    * 
    * @param node this is the node that contains the composite data
    * @param map this contains the converters to perform validation
    */
   private void validate(InputNode node, LabelMap map) throws Exception {     
      Position line = node.getPosition();

      for(Label label : map) {
         Class expect = type.getType();
         
         if(label.isRequired() && revision.isEqual()) {
            throw new ValueRequiredException("Unable to satisfy %s for %s at %s", label, expect, line);
         }
      }      
   }
   
   /**
    * This <code>write</code> method is used to perform serialization of
    * the given source object. Serialization is performed by appending
    * elements and attributes from the source object to the provided XML
    * element object. How the objects contacts are serialized is 
    * determined by the XML schema class that the source object is an
    * instance of. If a required contact is null an exception is thrown.
    * 
    * @param source this is the source object to be serialized
    * @param node the XML element the object is to be serialized to
    */
   public void write(OutputNode node, Object source) throws Exception {
      Class type = source.getClass();
      Schema schema = context.getSchema(type);
      Caller caller = schema.getCaller();
      
      try { 
         if(schema.isPrimitive()) {
            primitive.write(node, source);
         } else {
            caller.persist(source); 
            write(node, source, schema);
         }
      } finally {
         caller.complete(source);
      }
   }
   
   /**
    * This <code>write</code> method is used to perform serialization of
    * the given source object. Serialization is performed by appending
    * elements and attributes from the source object to the provided XML
    * element object. How the objects contacts are serialized is 
    * determined by the XML schema class that the source object is an
    * instance of. If a required contact is null an exception is thrown.
    * 
    * @param source this is the source object to be serialized
    * @param node the XML element the object is to be serialized to
    * @param schema this is used to track the referenced contacts 
    */
   private void write(OutputNode node, Object source, Schema schema) throws Exception {
      Section section = schema.getSection();
      
      writeVersion(node, source, schema);
      writeSection(node, source, section);
   }
   
   /**
    * This <code>writeSection</code> method is used to perform serialization 
    * of the given source object. Serialization is performed by appending
    * elements and attributes from the source object to the provided XML
    * element object. How the objects contacts are serialized is 
    * determined by the XML schema class that the source object is an
    * instance of. If a required contact is null an exception is thrown.
    * 
    * @param source this is the source object to be serialized
    * @param node the XML element the object is to be serialized to
    * @param section this is the section that defines the XML structure
    */
   private void writeSection(OutputNode node, Object source, Section section) throws Exception {
      NamespaceMap scope = node.getNamespaces();
      String prefix = section.getPrefix();

      if(prefix != null) {
         String reference = scope.getReference(prefix);
         
         if(reference == null) {     
            throw new ElementException("Namespace prefix '%s' in %s is not in scope", prefix, type);
         } else {
            node.setReference(reference);  
         }
      }
      writeAttributes(node, source, section);
      writeElements(node, source, section);
      writeText(node, source, section);
   }

   /**
    * This method is used to write the version attribute. A version is
    * written only if it is not the initial version or if it required.
    * The version is used to determine how to deserialize the XML. If
    * the version is different from the expected version then it allows
    * the object to be deserialized in a manner that does not require
    * any attributes or elements, and unmatched nodes are ignored. 
    * 
    * @param node this is the node to read the version attribute from
    * @param source this is the source object that is to be written
    * @param schema this is the schema that contains the version
    */
   private void writeVersion(OutputNode node, Object source, Schema schema) throws Exception {
      Version version = schema.getRevision();
      Label label = schema.getVersion();
      
      if(version != null) {
         Double start = revision.getDefault();
         Double value = version.revision();
     
         if(revision.compare(value, start)) {
            if(label.isRequired()) {
               writeAttribute(node, value, label);
            }
         } else {
            writeAttribute(node, value, label);
         }
      }
   }

   /**
    * This write method is used to write all the attribute contacts from
    * the provided source object to the XML element. This visits all
    * the contacts marked with the <code>Attribute</code> annotation in
    * the source object. All annotated contacts are written as attributes
    * to the XML element. This will throw an exception if a required
    * contact within the source object is null. 
    * 
    * @param source this is the source object to be serialized
    * @param node this is the XML element to write attributes to
    * @param section this is the section that defines the XML structure
    */
   private void writeAttributes(OutputNode node, Object source, Section section) throws Exception {
      LabelMap attributes = section.getAttributes();

      for(Label label : attributes) {
         Contact contact = label.getContact();         
         Object value = contact.get(source);
         Class expect = context.getType(type, source);
         
         if(value == null) {
            value = label.getEmpty(context);
         }
         if(value == null && label.isRequired()) {
            throw new AttributeException("Value for %s is null in %s", label, expect);
         }
         writeAttribute(node, value, label);              
      }      
   }

   /**
    * This write method is used to write all the element contacts from
    * the provided source object to the XML element. This visits all
    * the contacts marked with the <code>Element</code> annotation in
    * the source object. All annotated contacts are written as children
    * to the XML element. This will throw an exception if a required
    * contact within the source object is null. 
    * 
    * @param source this is the source object to be serialized
    * @param node this is the XML element to write elements to
    * @param section this is the section that defines the XML structure
    */
   private void writeElements(OutputNode node, Object source, Section section) throws Exception {
       for(String name : section) {
         Section child = section.getSection(name);
         
         if(child != null) {
            OutputNode next = node.getChild(name);

            writeSection(next, source, child);
         } else {
            String path = section.getPath(name);
            Label label = section.getElement(path);
            Class expect = context.getType(type, source);
            Object value = criteria.get(label);
            
            if(value == null) {
               if(label == null) {
                 throw new ElementException("Element '%s' not defined in %s", name, expect);
               }
               writeUnion(node, source, section, label);
            }
         }            
      }
   }
   
   /**
    * The <code>writeUnion</code> method is determine the unions 
    * for a particular label and set the value of that union to
    * the same value as the label. This helps the serialization 
    * process by ensuring once a union is written it is not
    * replaced. This is also required when writing inline lists.
    * 
    * @param node this is the XML element to write elements to
    * @param source this is the source object to be serialized
    * @param section this is the section associated with the label
    * @param label this is the label used to define the XML element
    */
   private void writeUnion(OutputNode node, Object source, Section section, Label label) throws Exception {
      Contact contact = label.getContact();
      Object value = contact.get(source);
      Class expect = context.getType(type, source);
      
      if(value == null && label.isRequired()) {
         throw new ElementException("Value for %s is null in %s", label, expect);
      }
      Object replace = writeReplace(value);
      
      if(replace != null) {
         writeElement(node, replace, label);            
      }
      criteria.set(label, replace);
   }
   
   /**
    * The <code>writeReplace</code> method is used to replace an object
    * before it is serialized. This is used so that an object can give
    * a substitute to be written to the XML document in the event that
    * the actual object is not suitable or desired for serialization. 
    * This acts as an equivalent to the Java Object Serialization
    * <code>writeReplace</code> method for the object serialization.
    * 
    * @param source this is the source object that is to be replaced
    * 
    * @return this returns the object to use as a replacement value
    */
   private Object writeReplace(Object source) throws Exception {      
      if(source != null) {
         Class type = source.getClass();
         Caller caller = context.getCaller(type);
         
         return caller.replace(source);
      }
      return source;
   }
   
   
   /**
    * This write method is used to write the text contact from the 
    * provided source object to the XML element. This takes the text
    * value from the source object and writes it to the single contact
    * marked with the <code>Text</code> annotation. If the value is
    * null and the contact value is required an exception is thrown.
    * 
    * @param source this is the source object to be serialized
    * @param node this is the XML element to write text value to
    * @param section this is used to track the referenced elements
    */
   private void writeText(OutputNode node, Object source, Section section) throws Exception {
      Label label = section.getText();

      if(label != null) {
         Contact contact = label.getContact();
         Object value = contact.get(source);
         Class expect = context.getType(type, source);
         
         if(value == null) {
            value = label.getEmpty(context);
         }
         if(value == null && label.isRequired()) {
            throw new TextException("Value for %s is null in %s", label, expect);
         }
         writeText(node, value, label); 
      }         
   }
   
   /**
    * This write method is used to set the value of the provided object
    * as an attribute to the XML element. This will acquire the string
    * value of the object using <code>toString</code> only if the
    * object provided is not an enumerated type. If the object is an
    * enumerated type then the <code>Enum.name</code> method is used.
    * 
    * @param value this is the value to be set as an attribute
    * @param node this is the XML element to write the attribute to
    * @param label the label that contains the contact details
    */
   private void writeAttribute(OutputNode node, Object value, Label label) throws Exception {
      if(value != null) {         
         Decorator decorator = label.getDecorator();
         String name = label.getName();
         String text = factory.getText(value);
         OutputNode done = node.setAttribute(name, text);
         
         decorator.decorate(done);
      }
   }
   
   /**
    * This write method is used to append the provided object as an
    * element to the given XML element object. This will recursively
    * write the contacts from the provided object as elements. This is
    * done using the <code>Converter</code> acquired from the contact
    * label. If the type of the contact value is not of the same
    * type as the XML schema class a "class" attribute is appended.
    * <p>
    * If the element being written is inline, then this will not 
    * check to see if there is a "class" attribute specifying the
    * name of the class. This is because inline elements do not have
    * an outer class and thus could never have an override.
    * 
    * @param value this is the value to be set as an element
    * @param node this is the XML element to write the element to
    * @param label the label that contains the contact details
    */
   private void writeElement(OutputNode node, Object value, Label label) throws Exception {
      if(value != null) {
         Class real = value.getClass();
         Label match = label.getLabel(real);
         String name = match.getName();
         Type type = label.getType(real); 
         OutputNode next = node.getChild(name);

         if(!match.isInline()) {
            writeNamespaces(next, type, match);
         }
         if(match.isInline() || !isOverridden(next, value, type)) {
            Converter convert = match.getConverter(context);
            boolean data = match.isData();
            
            next.setData(data);
            writeElement(next, value, convert);
         }
      }
   }
   
   /**
    * This is used write the element specified using the specified
    * converter. Writing the value using the specified converter will
    * result in the node being populated with the elements, attributes,
    * and text values to the provided node. If there is a problem
    * writing the value using the converter an exception is thrown.
    * 
    * @param node this is the node that the value is to be written to
    * @param value this is the value that is to be written
    * @param convert this is the converter used to perform writing
    */
   private void writeElement(OutputNode node, Object value, Converter convert) throws Exception {
      convert.write(node, value);
   }
   
   /**
    * This is used to apply <code>Decorator</code> objects to the
    * provided node before it is written. Application of decorations
    * before the node is written allows namespaces and comments to be
    * applied to the node. Decorations such as this do not affect the
    * overall structure of the XML that is written.
    * 
    * @param node this is the node that decorations are applied to
    * @param type this is the type to acquire the decoration for
    * @param label this contains the primary decorator to be used
    */
   private void writeNamespaces(OutputNode node, Type type, Label label) throws Exception {
      Class expect = type.getType();
      Decorator primary = context.getDecorator(expect);
      Decorator decorator = label.getDecorator();
      
      decorator.decorate(node, primary);
   }
   
   /**
    * This write method is used to set the value of the provided object
    * as the text for the XML element. This will acquire the string
    * value of the object using <code>toString</code> only if the
    * object provided is not an enumerated type. If the object is an
    * enumerated type then the <code>Enum.name</code> method is used.
    * 
    * @param value this is the value to set as the XML element text
    * @param node this is the XML element to write the text value to
    * @param label the label that contains the contact details
    */
   private void writeText(OutputNode node, Object value, Label label) throws Exception {
      if(value != null && !label.isTextList()) {         
         String text = factory.getText(value); 
         boolean data = label.isData();
         
         node.setData(data);
         node.setValue(text);        
      }
   }   
   
   /**
    * This is used to determine whether the specified value has been
    * overridden by the strategy. If the item has been overridden
    * then no more serialization is require for that value, this is
    * effectively telling the serialization process to stop writing.
    * 
    * @param node the node that a potential override is written to
    * @param value this is the object instance to be serialized
    * @param type this is the type of the object to be serialized
    * 
    * @return returns true if the strategy overrides the object
    */
   private boolean isOverridden(OutputNode node, Object value, Type type) throws Exception{
      return factory.setOverride(type, value, node);
   }
   
   /**
    * This takes the approach that the object is instantiated first and
    * then the annotated fields and methods are deserialized from the 
    * XML elements and attributes. When all the details have be read 
    * they are set on the internal contacts of the object. This is used
    * for places where we have a default no argument constructor.
    *  
    * @author Niall Gallagher
    */
   private static class Builder {
      
      /**
       * This is the composite converter that the builder will use.
       */
      protected final Composite composite;
      
      /**
       * This is the criteria object used to collect the values.
       */
      protected final Criteria criteria;
      
      /**
       * This is the schema object that contains the XML definition.
       */
      protected final Schema schema;
      
      /**
       * This is the object instance created by the strategy object.
       */
      protected final Instance value;
      
      /**
       * Constructor for the <code>Builder</code> object. This creates
       * a builder object capable of instantiating a object using a
       * default no argument constructor. All fields are deserialized
       * after the object has been instantiated.
       * 
       * @param composite this is the composite object used by this
       * @param criteria this collects the objects being deserialized
       * @param schema this is the class schema used by this
       * @param value this is the instance created by the strategy
       */
      public Builder(Composite composite, Criteria criteria, Schema schema, Instance value) {
         this.composite = composite;
         this.criteria = criteria;
         this.schema = schema;
         this.value = value;
      }
      
      /**
       * This <code>read</code> method performs deserialization of the
       * XML schema class type by traversing the contacts and using
       * details from the provided XML element. Here an instance is
       * created first then the contacts are traversed, this is done
       * when a default constructor is the only option.
       * 
       * @param node the XML element that will be deserialized by this 
       * 
       * @return this returns the fully deserialized object graph
       */
      public Object read(InputNode node) throws Exception {
         Object source = value.getInstance();
         Section section = schema.getSection();
         
         value.setInstance(source);
         composite.readVersion(node, source, schema);
         composite.readText(node, source, section);
         composite.readAttributes(node, source, section);
         composite.readElements(node, source, section);
         criteria.commit(source);  
            
         return source;
      }
   }
   
   /**
    * This takes the approach that the objects are deserialized first
    * then the instance is created using a constructor. In order for
    * the best constructor to be found all the potential objects need
    * to be deserialized first, then based on what has been deserialized
    * a constructor is chosen and the parameters are injected in to it.
    *  
    * @author Niall Gallagher
    */
   private class Injector extends Builder {
      
      /**
       * Constructor for the <code>Injector</code> object. This creates
       * a builder object capable of instantiating a object using a
       * constructor. It injects the constructor parameters in to the
       * constructor by using the deserialized objects.
       * 
       * @param composite this is the composite object used by this
       * @param criteria this collects the objects being deserialized
       * @param schema this is the class schema used by this
       * @param value this is the instance created by the strategy
       */
      private Injector(Composite composite, Criteria criteria, Schema schema, Instance value) {
         super(composite, criteria, schema, value);
      }

      /**
       * This <code>read</code> method performs deserialization of the
       * XML schema class type by traversing the contacts and using
       * details from the provided XML element. Here an instance is
       * instantiated only after everything has been deserialized so
       * that the instances can be injected in to a constructor.
       * 
       * @param node the XML element that will be deserialized by this 
       * 
       * @return this returns the fully deserialized object graph
       */
      public Object read(InputNode node) throws Exception {
         Section section = schema.getSection();
         
         composite.readVersion(node, null, schema);
         composite.readText(node, null, section);
         composite.readAttributes(node, null, section);
         composite.readElements(node, null, section);
         
         return readInject(node);
      } 
      

      /**
       * This <code>readInject</code> method performs deserialization
       * of the XML schema class type by traversing the contacts and 
       * creating them using details from the provided XML element. 
       * Because this will convert a non-primitive value it delegates 
       * to other converters to perform deserialization of lists and 
       * primitives.
       * <p>
       * This takes the approach of reading the elements and attributes
       * before instantiating the object. Instantiation is performed 
       * using a declared constructor. The parameters are taken from 
       * the deserialized objects.
       * 
       * @param node the XML element that will be deserialized by this 
       * 
       * @return this returns the fully deserialized object graph
       */
      private Object readInject(InputNode node) throws Exception {
         Instantiator creator = schema.getInstantiator();
         Object source = creator.getInstance(criteria);
         
         value.setInstance(source);
         criteria.commit(source); 

         return source;
      }
   }
}

