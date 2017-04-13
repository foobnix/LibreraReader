/*
 * SessionManager May 2010
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

/**
 * The <code>SessionManager</code> is used to manage the sessions that
 * are used during the serialization process. Sessions are stored in 
 * thread locals so that should a <code>Converter</code> delegate back
 * in to the same persister instance, it will acquire the same session
 * object. This ensures that converters can be used to resolve cycles
 * in the object graph with the <code>CycleStrategy</code> and also
 * ensures there is no overhead creating new session objects.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.xml.core.Session
 */
class SessionManager {
   
   /**
    * This is the thread local used to store the sessions objects.
    */
   private ThreadLocal<Reference> local;
   
   /**
    * Constructor for the <code>SessionManager</code> object. This 
    * is used to create a session manager that stores sessions in 
    * a thread local so that it can be reused on multiple invocations
    * to the <code>Persister</code> object. References are maintained
    * to each session created so that it is closed when there are
    * no longer any more references to the session.
    */
   public SessionManager() {
      this.local = new ThreadLocal<Reference>();
   }   
   
   /**
    * This is used to open a new <code>Session</code> object. If the
    * session exists within the thread local it is returned and a
    * reference count to the session is increased. This ensures that
    * the session is not disposed of until all references to it are
    * closed. By default this creates a strict session.
    * 
    * @return this returns a strict session from the manager    
    */
   public Session open() throws Exception {
      return open(true);
   }

   /**
    * This is used to open a new <code>Session</code> object. If the
    * session exists within the thread local it is returned and a
    * reference count to the session is increased. This ensures that
    * the session is not disposed of until all references to it are
    * closed. The strictness of the session can be specified.
    * 
    * @param strict this determines if the session is strict
    * 
    * @return this returns a session from the manager    
    */
   public Session open(boolean strict) throws Exception {
      Reference session = local.get();
      
      if(session != null) {
         return session.get();
      }
      return create(strict);
   }
   
   /**
    * This is used to create a new <code>Session</code> object. On
    * creation of the session it is placed on a thread local so 
    * that it can be acquired by the current thread when required.
    * 
    * @param strict this determines if the session is strict
    * 
    * @return this returns a new session from the manager    
    */
   private Session create(boolean strict) throws Exception {
      Reference session = new Reference(strict);
      
      if(session != null) {
         local.set(session);
      }
      return session.get();
   }
   
   /**
    * This is used to close the session on the thread local. If the
    * session still has references to it then the reference count
    * is decreased and the session remains open. This ensures that
    * we can make recursive calls in to the <code>Persister</code>
    * and still use the same session object.
    */
   public void close() throws Exception {
      Reference session = local.get();
      
      if(session == null) {
         throw new PersistenceException("Session does not exist");
      } 
      int reference = session.clear();
      
      if(reference == 0) {
         local.remove();
      }
   }
   
   /**
    * The <code>Reference</code> object is used to store sessions 
    * and count references to them. Counting references ensures that
    * no session is closed or disposed of until all references to 
    * it have been removed. Once references are removed the session
    * is discarded and can no longer be acquired.
    * 
    * @author Niall Gallagher
    */
   private static class Reference {

      /**
       * This is the session object that is maintained by this.
       */
      private Session session;
      
      /**
       * This is the count of the number of references to this.
       */
      private int count;

      /**
       * Constructor for the <code>Reference</code> object. This is
       * used during the serialization process to manage references 
       * to the sessions that are used by the serializer.
       * 
       * @param strict determines whether the session is strict
       */
      public Reference(boolean strict) {
         this.session = new Session(strict);
      }

      /**
       * This is used to acquire the session and increase the count
       * of references to the session. When the references are all
       * cleared then the reference counter can no longer be 
       * increased and the reference should be discarded.
       * 
       * @return this returns the session for this reference
       */
      public Session get() {
         if(count >= 0) {
            count++;
         }
         return session;
      }

      /**
       * This is used to clear the references to the session. A
       * reference is cleared when it is closed from the manager. 
       * The reference is disposed of when this returns zero or a
       * negative number indicating all references are gone.
       * 
       * @return this returns the number of references this has
       */
      public int clear() {
         return --count;
      }
   }
}
