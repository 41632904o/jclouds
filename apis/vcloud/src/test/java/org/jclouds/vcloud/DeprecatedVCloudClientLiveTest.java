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
package org.jclouds.vcloud;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.jclouds.Constants;
import org.jclouds.compute.ComputeServiceContextFactory;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.rest.AuthorizationException;
import org.jclouds.rest.RestContext;
import org.jclouds.rest.RestContextFactory;
import org.jclouds.vcloud.domain.Catalog;
import org.jclouds.vcloud.domain.CatalogItem;
import org.jclouds.vcloud.domain.Org;
import org.jclouds.vcloud.domain.ReferenceType;
import org.jclouds.vcloud.domain.Task;
import org.jclouds.vcloud.domain.VApp;
import org.jclouds.vcloud.domain.VAppTemplate;
import org.jclouds.vcloud.domain.VDC;
import org.jclouds.vcloud.domain.Vm;
import org.jclouds.vcloud.domain.network.OrgNetwork;
import org.jclouds.vcloud.reference.VCloudConstants;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Module;

/**
 * Tests behavior of deprecated {@code VCloudClient} features
 * 
 * @author Adrian Cole
 */
@Deprecated
@Test(groups = "live", singleThreaded = true)
public class DeprecatedVCloudClientLiveTest  {

   protected VCloudClient connection;
   protected RestContext<VCloudClient,VCloudAsyncClient> context;

   @Test
   public void testOrg() throws Exception {
      for (Org org : orgs) {
         assertNotNull(org);
         assertNotNull(org.getName());
         assert org.getCatalogs().size() >= 1;
         assert org.getTasksList() != null;
         assert org.getVDCs().size() >= 1;
         assertEquals(connection.findOrgNamed(org.getName()), org);
      }
   }

   @Test
   public void testPropertiesCanOverrideDefaultOrg() throws Exception {
      for (Org org : orgs) {
         RestContext<VCloudClient, VCloudAsyncClient> newContext = null;
         try {
            newContext = createContextWithProperties(overrideDefaults(ImmutableMap.of(
                  VCloudConstants.PROPERTY_VCLOUD_DEFAULT_ORG, Pattern.quote(org.getName()))));
            assertEquals(newContext.getApi().findOrgNamed(null), org);
         } finally {
            newContext.close();
         }
      }
   }

   public Properties overrideDefaults(Map<String, String> overrides) {
      Properties properties = setupProperties();
      properties.putAll(overrides);
      return properties;
   }

   @Test
   public void testCatalog() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType cat : org.getCatalogs().values()) {
            Catalog response = connection.getCatalog(cat.getHref());
            assertNotNull(response);
            assertNotNull(response.getName());
            assertNotNull(response.getHref());
            assertEquals(connection.findCatalogInOrgNamed(org.getName(), response.getName()), response);
         }
      }
   }

   @Test
   public void testPropertiesCanOverrideDefaultCatalog() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType cat : org.getCatalogs().values()) {
            RestContext<VCloudClient, VCloudAsyncClient> newContext = null;
            try {
               newContext = createContextWithProperties(overrideDefaults(ImmutableMap.of(
                     VCloudConstants.PROPERTY_VCLOUD_DEFAULT_ORG, Pattern.quote(org.getName()),
                     VCloudConstants.PROPERTY_VCLOUD_DEFAULT_CATALOG, Pattern.quote(cat.getName()))));
               assertEquals(newContext.getApi().findCatalogInOrgNamed(null, null), connection.getCatalog(cat.getHref()));
            } finally {
               newContext.close();
            }
         }
      }
   }

   @Test
   public void testGetOrgNetwork() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType resource : org.getNetworks().values()) {
            if (resource.getType().equals(VCloudMediaType.NETWORK_XML)) {
               OrgNetwork item = connection.getNetwork(resource.getHref());
               assertNotNull(item);
            }
         }
      }
   }

   @Test
   public void testGetVDCNetwork() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType vdc : org.getVDCs().values()) {
            VDC response = connection.getVDC(vdc.getHref());
            for (ReferenceType resource : response.getAvailableNetworks().values()) {
               if (resource.getType().equals(VCloudMediaType.NETWORK_XML)) {
                  try {
                     OrgNetwork net = connection.getNetwork(resource.getHref());
                     assertNotNull(net);
                     assertNotNull(net.getName());
                     assertNotNull(net.getHref());
                     assertEquals(
                           connection.findNetworkInOrgVDCNamed(org.getName(), response.getName(), net.getName()), net);
                  } catch (AuthorizationException e) {

                  }
               }
            }
         }
      }
   }

   @Test
   public void testPropertiesCanOverrideDefaultNetwork() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType vdc : org.getVDCs().values()) {
            VDC response = connection.getVDC(vdc.getHref());
            for (ReferenceType net : response.getAvailableNetworks().values()) {
               RestContext<VCloudClient, VCloudAsyncClient> newContext = null;
               try {
                  newContext = createContextWithProperties(overrideDefaults(ImmutableMap.of(
                        VCloudConstants.PROPERTY_VCLOUD_DEFAULT_ORG, Pattern.quote(org.getName()),
                        VCloudConstants.PROPERTY_VCLOUD_DEFAULT_VDC, Pattern.quote(vdc.getName()),
                        VCloudConstants.PROPERTY_VCLOUD_DEFAULT_NETWORK, Pattern.quote(net.getName()))));
                  assertEquals(newContext.getApi().findNetworkInOrgVDCNamed(null, null, net.getName()),
                        connection.getNetwork(net.getHref()));
               } finally {
                  newContext.close();
               }
            }
         }
      }
   }

   @Test
   public void testGetCatalogItem() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType cat : org.getCatalogs().values()) {
            Catalog response = connection.getCatalog(cat.getHref());
            for (ReferenceType resource : response.values()) {
               if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
                  CatalogItem item = connection.getCatalogItem(resource.getHref());
                  verifyCatalogItem(item);
               }
            }
         }
      }
   }

   protected void verifyCatalogItem(CatalogItem item) {
      assertNotNull(item);
      assertNotNull(item);
      assertNotNull(item.getEntity());
      assertNotNull(item.getHref());
      assertNotNull(item.getProperties());
      assertNotNull(item.getType());
   }

   @Test
   public void testFindCatalogItem() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType cat : org.getCatalogs().values()) {
            Catalog response = connection.getCatalog(cat.getHref());
            for (ReferenceType resource : response.values()) {
               if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
                  CatalogItem item = connection.findCatalogItemInOrgCatalogNamed(org.getName(), response.getName(),
                        resource.getName());
                  verifyCatalogItem(item);
               }
            }
         }
      }
   }

   @Test
   public void testDefaultVDC() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType vdc : org.getVDCs().values()) {
            VDC response = connection.getVDC(vdc.getHref());
            assertNotNull(response);
            assertNotNull(response.getName());
            assertNotNull(response.getHref());
            assertNotNull(response.getResourceEntities());
            assertNotNull(response.getAvailableNetworks());
            assertEquals(connection.getVDC(response.getHref()), response);
         }
      }
   }

   @Test
   public void testPropertiesCanOverrideDefaultVDC() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType vdc : org.getVDCs().values()) {
            RestContext<VCloudClient, VCloudAsyncClient> newContext = null;
            try {
               newContext = createContextWithProperties(overrideDefaults(ImmutableMap.of(
                     VCloudConstants.PROPERTY_VCLOUD_DEFAULT_ORG, org.getName(),
                     VCloudConstants.PROPERTY_VCLOUD_DEFAULT_VDC, vdc.getName())));
               assertEquals(newContext.getApi().findVDCInOrgNamed(null, null), connection.getVDC(vdc.getHref()));
            } finally {
               newContext.close();
            }
         }
      }
   }

   @Test
   public void testDefaultTasksList() throws Exception {
      for (Org org : orgs) {
         org.jclouds.vcloud.domain.TasksList response = connection.findTasksListInOrgNamed(org.getName());
         assertNotNull(response);
         assertNotNull(response.getLocation());
         assertNotNull(response.getTasks());
         assertEquals(connection.getTasksList(response.getLocation()).getLocation(), response.getLocation());
      }
   }

   @Test
   public void testGetTask() throws Exception {
      for (Org org : orgs) {
         org.jclouds.vcloud.domain.TasksList response = connection.findTasksListInOrgNamed(org.getName());
         assertNotNull(response);
         assertNotNull(response.getLocation());
         assertNotNull(response.getTasks());
         if (response.getTasks().size() > 0) {
            Task task = response.getTasks().last();
            assertEquals(connection.getTask(task.getHref()).getHref(), task.getHref());
         }
      }
   }

   protected String provider = "vcloud";
   protected String identity;
   protected String credential;
   protected String endpoint;
   protected String apiVersion;
   protected Iterable<Org> orgs;

   protected void setupCredentials() {
      identity = checkNotNull(System.getProperty("test." + provider + ".identity"), "test." + provider + ".identity");
      credential = checkNotNull(System.getProperty("test." + provider + ".credential"), "test." + provider
            + ".identity");
      endpoint = System.getProperty("test." + provider + ".endpoint");
     apiVersion = System.getProperty("test." + provider + ".api-version");
   }

   protected Properties setupProperties() {
      Properties overrides = new Properties();
      overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
      overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
      overrides.setProperty(provider + ".identity", identity);
      overrides.setProperty(provider + ".credential", credential);
      if (endpoint != null)
         overrides.setProperty(provider + ".endpoint", endpoint);
      if (apiVersion != null)
         overrides.setProperty(provider + ".api-version", apiVersion);
      return overrides;
   }

   protected Properties setupRestProperties() {
      return RestContextFactory.getPropertiesFromResource("/rest.properties");
   }

   @BeforeGroups(groups = { "live" })
   public void setupClient() {
      setupCredentials();
      context = createContextWithProperties(setupProperties());
      connection = context.getApi();
      orgs = listOrgs();
   }

   public RestContext<VCloudClient, VCloudAsyncClient> createContextWithProperties(Properties overrides) {
      return new ComputeServiceContextFactory(setupRestProperties()).createContext(provider,
            ImmutableSet.<Module> of(new Log4JLoggingModule()), overrides).getProviderSpecificContext();
   }

   @AfterGroups(groups = { "live" })
   public void teardownClient() {
      context.close();
   }

   protected Iterable<Org> listOrgs() {
      return Iterables.transform(connection.listOrgs().values(), new Function<ReferenceType, Org>(){

         @Override
         public Org apply(ReferenceType arg0) {
            return connection.getOrg(arg0.getHref());
         }
         
      });
   }

   @Test
   public void testListOrgs() throws Exception {
      for (Org org : orgs) {
         assertNotNull(org);
         assertNotNull(org.getName());
         assertNotNull(org.getHref());
         assertEquals(connection.getOrg(org.getHref()).getName(), org.getName());
         assertEquals(connection.findOrgNamed(org.getName()).getName(), org.getName());
      }
   }

   @Test
   public void testGetVAppTemplate() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType cat : org.getCatalogs().values()) {
            Catalog response = connection.getCatalog(cat.getHref());
            for (ReferenceType resource : response.values()) {
               if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
                  CatalogItem item = connection.getCatalogItem(resource.getHref());
                  if (item.getEntity().getType().equals(VCloudMediaType.VAPPTEMPLATE_XML)) {
                     VAppTemplate template = connection.getVAppTemplate(item.getEntity().getHref());
                     if (template != null){
                        assertEquals(template.getName(),item.getEntity().getName());
                     } else {
                        // null can be no longer available or auth exception
                     }
                  }
               }
            }
         }
      }
   }

   @Test
   public void testGetOvfEnvelopeForVAppTemplate() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType cat : org.getCatalogs().values()) {
            Catalog response = connection.getCatalog(cat.getHref());
            for (ReferenceType resource : response.values()) {
               if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
                  try {
                     CatalogItem item = connection.getCatalogItem(resource.getHref());
                     if (item.getEntity().getType().equals(VCloudMediaType.VAPPTEMPLATE_XML)) {
                        assertNotNull(connection.getOvfEnvelopeForVAppTemplate(item.getEntity().getHref()));
                     }
                  } catch (AuthorizationException e) {

                  }
               }
            }
         }
      }
   }

   @Test
   public void testGetVApp() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType vdc : org.getVDCs().values()) {
            VDC response = connection.getVDC(vdc.getHref());
            for (ReferenceType item : response.getResourceEntities().values()) {
               if (item.getType().equals(VCloudMediaType.VAPP_XML)) {
                  connection.getVApp(item.getHref());
                  // null can be no longer available or auth exception
               }
            }
         }
      }
   }

   @Test
   public void testGetThumbnailOfVm() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType vdc : org.getVDCs().values()) {
            VDC response = connection.getVDC(vdc.getHref());
            for (ReferenceType item : response.getResourceEntities().values()) {
               if (item.getType().equals(VCloudMediaType.VAPP_XML)) {
                  VApp app = connection.getVApp(item.getHref());
                  if (app != null) {
                     for (Vm vm : app.getChildren()) {
                        assert connection.getThumbnailOfVm(vm.getHref()) != null;
                     }
                  } else {
                     // null can be no longer available or auth exception
                  }
               }
            }
         }
      }
   }

   @Test
   public void testGetVm() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType vdc : org.getVDCs().values()) {
            VDC response = connection.getVDC(vdc.getHref());
            for (ReferenceType item : response.getResourceEntities().values()) {
               if (item.getType().equals(VCloudMediaType.VAPP_XML)) {
                  VApp app = connection.getVApp(item.getHref());
                  if (app != null) {
                     for (Vm vmRef : app.getChildren()) {
                        Vm vm = connection.getVm(vmRef.getHref());
                        assertEquals(vm.getName(), vmRef.getName());
                     }
                  } else {
                     // null can be no longer available or auth exception
                  }
               }
            }
         }
      }
   }

   @Test
   public void testFindVAppTemplate() throws Exception {
      for (Org org : orgs) {
         for (ReferenceType cat : org.getCatalogs().values()) {
            Catalog response = connection.getCatalog(cat.getHref());
            for (ReferenceType resource : response.values()) {
               if (resource.getType().equals(VCloudMediaType.CATALOGITEM_XML)) {
                  CatalogItem item = connection.getCatalogItem(resource.getHref());
                  if (item.getEntity().getType().equals(VCloudMediaType.VAPPTEMPLATE_XML)) {
                     VAppTemplate template = connection.findVAppTemplateInOrgCatalogNamed(org.getName(), response.getName(),
                              item.getEntity().getName());
                     if (template != null){
                        assertEquals(template.getName(),item.getEntity().getName());
                     } else {
                        // null can be no longer available or auth exception
                     }
                  }
               }
            }
         }
      }
   }

}
