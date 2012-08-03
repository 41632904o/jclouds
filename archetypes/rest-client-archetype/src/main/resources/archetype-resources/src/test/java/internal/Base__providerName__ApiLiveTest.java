#set( $lcaseProviderName = ${providerName.toLowerCase()} )
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
package ${package}.internal;

import org.jclouds.apis.BaseContextLiveTest;
import ${package}.${providerName}ApiMetadata;
import ${package}.${providerName}AsyncApi;
import ${package}.${providerName}Api;
import org.jclouds.rest.RestContext;

import com.google.common.reflect.TypeToken;


/**
 * @author ${author}
 */
public class Base${providerName}ApiLiveTest extends BaseContextLiveTest<RestContext<${providerName}Api, ${providerName}AsyncApi>> {

   public Base${providerName}ApiLiveTest() {
      provider = "${lcaseProviderName}";
   }
   
   @Override
   protected TypeToken<RestContext<${providerName}Api, ${providerName}AsyncApi>> contextType() {
      return ${providerName}ApiMetadata.CONTEXT_TOKEN;
   }

}

