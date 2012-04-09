/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.apis;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

/**
 * 
 * @author Jeremy Whitlock <jwhitlock@apache.org>
 */
@Test(groups = "unit")
public abstract class BaseApiMetadataTest {

   private final ApiMetadata toTest;
   private final ApiType expectedType;

   public BaseApiMetadataTest(ApiMetadata toTest, ApiType expectedType) {
      this.toTest = toTest;
      this.expectedType = expectedType;
   }

   @Test
   public void testWithId() {
      ApiMetadata apiMetadata = Apis.withId(toTest.getId());

      assertEquals(toTest, apiMetadata);
   }

   // it is ok to have multiple services in the same classpath (ex. ec2 vs elb)
   @Test
   public void testOfTypeContains() {
      ImmutableSet<ApiMetadata> ofType = ImmutableSet.copyOf(Apis.ofType(expectedType));
      assert ofType.contains(toTest) : String.format("%s not found in %s", toTest, ofType);
   }

   @Test
   public void testAllContains() {
      ImmutableSet<ApiMetadata> all = ImmutableSet.copyOf(Apis.all());
      assert all.contains(toTest) : String.format("%s not found in %s", toTest, all);
   }

}