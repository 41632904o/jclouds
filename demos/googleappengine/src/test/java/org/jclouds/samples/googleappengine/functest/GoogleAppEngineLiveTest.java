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
package org.jclouds.samples.googleappengine.functest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.compute.ComputeService;
import org.jclouds.util.Maps2;
import org.jclouds.util.Strings2;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

/**
 * Starts up the Google App Engine for Java Development environment and deploys an application which
 * tests {@link ComputeService} and {@link BlobStore}.
 * 
 * @author Adrian Cole
 */
@Test(groups = "live", singleThreaded = true)
public class GoogleAppEngineLiveTest {

   GoogleDevServer server;
   private URL url;

   @BeforeTest
   @Parameters( { "warfile", "devappserver.address", "devappserver.port" })
   public void startDevAppServer(final String warfile, final String address, final String port)
            throws Exception {
      url = new URL(String.format("http://%s:%s", address, port));
      
      Properties props = new Properties();
      props.putAll(stripTestPrefix(selectPropertiesForIdentityAndCredential()));
      server = new GoogleDevServer();
      server.writePropertiesAndStartServer(address, port, warfile, props);
   }

   Map<String, String> stripTestPrefix(Map<String, String> identityCrendential) {
      return Maps2.transformKeys(identityCrendential, new Function<String, String>() {

         @Override
         public String apply(String arg0) {
            return arg0.replace("test.", "");
         }

      });
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   Map<String, String> selectPropertiesForIdentityAndCredential() {
      return Maps.filterKeys((Map) System.getProperties(), new Predicate<String>() {

         @Override
         public boolean apply(String input) {
            // TODO Auto-generated method stub
            return input.matches("^test\\.[a-z0-9-]+\\.(identity|credential)$");
         }

      });
   }

   @Test
   public void shouldPass() throws InterruptedException, IOException {
      InputStream i = url.openStream();
      String string = Strings2.toStringAndClose(i);
      assert string.indexOf("Welcome") >= 0 : string;
   }

   @Test(invocationCount = 5, enabled = true)
   public void testGuiceJCloudsSerial() throws InterruptedException, IOException {
      URL gurl = new URL(url, "/guice/resources.check");
      InputStream i = gurl.openStream();
      String string = Strings2.toStringAndClose(i);
      assert string.indexOf("List") >= 0 : string;
   }

   @Test(invocationCount = 10, enabled = false, threadPoolSize = 3)
   public void testGuiceJCloudsParallel() throws InterruptedException, IOException {
      URL gurl = new URL(url, "/guice/resources.check");
      InputStream i = gurl.openStream();
      String string = Strings2.toStringAndClose(i);
      assert string.indexOf("List") >= 0 : string;
   }
}
