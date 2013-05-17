/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.collect;

import java.util.Iterator;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

/**
 * An iterator which forwards all its method calls to another iterator. Subclasses should override
 * one or more methods to modify the behavior of the backing iterable as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 * 
 * @author Adrian Cole
 */
@Beta
public abstract class ForwardingIterableWithMarker<T> extends IterableWithMarker<T> {

   /** Constructor for use by subclasses. */
   protected ForwardingIterableWithMarker() {
   }

   protected abstract IterableWithMarker<T> delegate();

   @Override
   public Iterator<T> iterator() {
      return delegate().iterator();
   }

   @Override
   public Optional<Object> nextMarker() {
      return delegate().nextMarker();
   }

}
