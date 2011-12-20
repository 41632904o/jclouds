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
package org.jclouds.hpcloud.object.storage.config;

import javax.inject.Singleton;

import org.jclouds.hpcloud.object.storage.HPCloudObjectStorageAsyncClient;
import org.jclouds.hpcloud.object.storage.HPCloudObjectStorageClient;
import org.jclouds.http.RequiresHttp;
import org.jclouds.openstack.swift.CommonSwiftAsyncClient;
import org.jclouds.openstack.swift.CommonSwiftClient;
import org.jclouds.openstack.swift.config.BaseSwiftRestClientModule;
import org.jclouds.rest.ConfiguresRestClient;

import com.google.inject.Provides;

/**
 * 
 * @author Adrian Cole
 */
@ConfiguresRestClient
@RequiresHttp
public class HPCloudObjectStorageRestClientModule extends
         BaseSwiftRestClientModule<HPCloudObjectStorageClient, HPCloudObjectStorageAsyncClient> {

   public HPCloudObjectStorageRestClientModule() {
      super(HPCloudObjectStorageClient.class, HPCloudObjectStorageAsyncClient.class);
   }

   @Provides
   @Singleton
   CommonSwiftClient provideCommonSwiftClient(HPCloudObjectStorageClient in) {
      return in;
   }

   @Provides
   @Singleton
   CommonSwiftAsyncClient provideCommonSwiftClient(HPCloudObjectStorageAsyncClient in) {
      return in;
   }

}
