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
package org.jclouds.trmk.vcloud_0_8.compute.suppliers;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.jclouds.compute.BaseVersionedServiceLiveTest;
import org.jclouds.compute.domain.Image;
import org.jclouds.lifecycle.Closer;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.rest.RestContextFactory;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.jclouds.trmk.vcloud_0_8.TerremarkVCloudClient;
import org.jclouds.trmk.vcloud_0_8.domain.CatalogItem;
import org.jclouds.trmk.vcloud_0_8.functions.AllCatalogItemsInOrg;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Tests behavior of {@code VAppTemplatesInOrgs}
 * 
 * @author Adrian Cole
 */
@Test(groups = "live", singleThreaded = true, testName = "VAppTemplatesInOrgsLiveTest")
public class VAppTemplatesInOrgsLiveTest extends BaseVersionedServiceLiveTest {
   public VAppTemplatesInOrgsLiveTest() {
      provider = "trmk-vcloudexpress";
   }

   private TerremarkVCloudClient tmClient;
   private VAppTemplatesInOrgs parser;
   private Closer closer;
   private AllCatalogItemsInOrg allCatalogItemsInOrg;


   @BeforeGroups(groups = { "live" })
   public void setupClient() {
      setupCredentials();
      Properties overrides = setupProperties();

      Injector injector = new RestContextFactory().createContextBuilder(provider,
               ImmutableSet.<Module> of(new Log4JLoggingModule(), new SshjSshClientModule()),overrides).buildInjector();

      tmClient = injector.getInstance(TerremarkVCloudClient.class);
      allCatalogItemsInOrg = injector.getInstance(AllCatalogItemsInOrg.class);
      parser = injector.getInstance(VAppTemplatesInOrgs.class);
      closer = injector.getInstance(Closer.class);
   }

   @Test
   public void testParseAllImages() {

      Set<? extends Image> images = parser.get();

      Iterable<? extends CatalogItem> templates = allCatalogItemsInOrg.apply(tmClient
               .findOrgNamed(null));

      assertEquals(images.size(), Iterables.size(templates));
      assert images.size() > 0;
   }

   @AfterGroups(groups = { "live" })
   public void close() throws IOException {
      closer.close();
   }
}
