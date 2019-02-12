/*
 * Scanner.java January 2010
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

/**
 * The <code>Scanner</code> interface is used to scan a class for a
 * given annotation. A scanner will cache all previous lookups to
 * ensure the look time is reduced. Caches include misses, so if a
 * class does not contain an annotation when scanned it will not
 * be scanned again.
 * 
 * @author Niall Gallagher
 */
interface Scanner {

   /**
    * This method will scan a class for the specified annotation. 
    * If the annotation is found on the class, or on one of the super
    * types then it is returned. All scans should be cached to ensure
    * scanning is only performed once.
    * 
    * @param type this is the annotation type to be scanned for
    * 
    * @return this will return the annotation if it is found
    */
   <T extends Annotation> T scan(Class<T> type);
}
