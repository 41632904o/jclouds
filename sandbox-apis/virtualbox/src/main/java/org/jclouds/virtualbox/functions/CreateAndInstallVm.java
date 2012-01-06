/*
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

package org.jclouds.virtualbox.functions;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.compute.options.RunScriptOptions.Builder.runAsRoot;
import static org.jclouds.compute.options.RunScriptOptions.Builder.wrapInInitScript;
import static org.jclouds.virtualbox.config.VirtualBoxConstants.VIRTUALBOX_INSTALLATION_KEY_SEQUENCE;
import static org.jclouds.virtualbox.util.MachineUtils.applyForMachine;
import static org.jclouds.virtualbox.util.MachineUtils.lockMachineAndApply;
import static org.jclouds.virtualbox.util.MachineUtils.lockSessionOnMachineAndApply;
import static org.virtualbox_4_1.LockType.Shared;
import static org.virtualbox_4_1.LockType.Write;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Named;

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.net.IPSocket;
import org.jclouds.ssh.SshException;
import org.jclouds.virtualbox.config.VirtualBoxConstants;
import org.jclouds.virtualbox.domain.DeviceDetails;
import org.jclouds.virtualbox.domain.ExecutionType;
import org.jclouds.virtualbox.domain.HardDisk;
import org.jclouds.virtualbox.domain.IsoImage;
import org.jclouds.virtualbox.domain.NatAdapter;
import org.jclouds.virtualbox.domain.StorageController;
import org.jclouds.virtualbox.domain.VmSpec;
import org.jclouds.virtualbox.settings.KeyboardScancodes;
import org.virtualbox_4_1.AccessMode;
import org.virtualbox_4_1.DeviceType;
import org.virtualbox_4_1.IMachine;
import org.virtualbox_4_1.IMedium;
import org.virtualbox_4_1.IProgress;
import org.virtualbox_4_1.ISession;
import org.virtualbox_4_1.VirtualBoxManager;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.inject.Inject;

public class CreateAndInstallVm implements Function<VmSpec, IMachine> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final VirtualBoxManager manager;
   private final CreateAndRegisterMachineFromIsoIfNotAlreadyExists createAndRegisterMachineFromIsoIfNotAlreadyExists;
   private String guestId;
   private final ComputeServiceContext context;
   private final String hostId;
   private final Predicate<IPSocket> socketTester;
   private final String webServerHost;
   private final int webServerPort;
   private final ExecutionType executionType;

   @Inject
   public CreateAndInstallVm(VirtualBoxManager manager,
            CreateAndRegisterMachineFromIsoIfNotAlreadyExists CreateAndRegisterMachineFromIsoIfNotAlreadyExists,
            @Named(VirtualBoxConstants.VIRTUALBOX_WORKINGDIR) String workingDir, String guestId,
            ComputeServiceContext context, String hostId, Predicate<IPSocket> socketTester, String webServerHost,
            int webServerPort, ExecutionType executionType) {
      this.manager = manager;
      this.createAndRegisterMachineFromIsoIfNotAlreadyExists = CreateAndRegisterMachineFromIsoIfNotAlreadyExists;
      this.guestId = guestId;
      this.context = context;
      this.hostId = hostId;
      this.socketTester = socketTester;
      this.webServerHost = webServerHost;
      this.webServerPort = webServerPort;
      this.executionType = executionType;
   }

   @Override
   public IMachine apply(VmSpec vmSpec) {

      ensureWebServerIsRunning();

      final IMachine vm =createAndRegisterMachineFromIsoIfNotAlreadyExists.apply(vmSpec);

      String vmName = vmSpec.getVmName();

      // Change RAM
      ensureMachineHasMemory(vmName, vmSpec.getMemory());

      Set<StorageController> controllers = vmSpec.getControllers();
      if (controllers.isEmpty()) {
         throw new IllegalStateException(missingIDEControllersMessage(vmSpec));
      }
      StorageController controller = controllers.iterator().next();
      ensureMachineHasIDEControllerNamed(vmName, controller);
      setupHardDisksForController(vmName, controller);
      setupDvdsForController(vmSpec, vmName, controller);

      // NAT
      Map<Long, NatAdapter> natNetworkAdapters = vmSpec.getNatNetworkAdapters();
      for (Map.Entry<Long, NatAdapter> natAdapterAndSlot : natNetworkAdapters.entrySet()) {
         long slotId = natAdapterAndSlot.getKey();
         NatAdapter natAdapter = natAdapterAndSlot.getValue();
         ensureNATNetworkingIsAppliedToMachine(vmName, slotId, natAdapter);
      }

      // Launch machine and wait for it to come online
      ensureMachineIsLaunched(vmName);

      final String installKeySequence = System.getProperty(VIRTUALBOX_INSTALLATION_KEY_SEQUENCE, defaultInstallSequence(vmName));
      sendKeyboardSequence(installKeySequence, vmName);

      boolean sshDeamonIsRunning = false;
      while (!sshDeamonIsRunning) {
         try {
            if (runScriptOnNode(guestId, "id", wrapInInitScript(false).runAsRoot(false)).getExitCode() == 0) {
               logger.debug("Got response from ssh daemon.");
               sshDeamonIsRunning = true;
            }
         } catch (SshException e) {
            logger.debug("No response from ssh daemon...");
         }
      }

      logger.debug("Installation of image complete. Powering down...");
      lockSessionOnMachineAndApply(manager, Shared, vmName, new Function<ISession, Void>() {

         @Override
         public Void apply(ISession session) {
            IProgress powerDownProgress = session.getConsole().powerDown();
            powerDownProgress.waitForCompletion(-1);
            return null;
         }

      });
      return vm;
   }

   private void setupDvdsForController(VmSpec vmSpecification, String vmName, StorageController controller) {
      Set<IsoImage> dvds = controller.getIsoImages();
      for (IsoImage dvd : dvds) {
         String dvdSource = dvd.getSourcePath();
         final IMedium dvdMedium = manager.getVBox().openMedium(dvdSource, DeviceType.DVD,
                 AccessMode.ReadOnly, vmSpecification.isForceOverwrite());
         ensureMachineDevicesAttached(vmName, dvdMedium, dvd.getDeviceDetails(), controller.getName());
      }
   }

   private void setupHardDisksForController(String vmName, StorageController controller) {
      Set<HardDisk> hardDisks = controller.getHardDisks();
      for (HardDisk hardDisk : hardDisks) {
         String sourcePath = hardDisk.getDiskPath();
         if (new File(sourcePath).exists()) {
            boolean deleted = new File(sourcePath).delete();
            if (!deleted) {
               logger.error(String.format("File %s could not be deleted.", sourcePath));
            }
         }
         IMedium medium = new CreateMediumIfNotAlreadyExists(manager, true).apply(hardDisk);
         ensureMachineDevicesAttached(vmName, medium, hardDisk.getDeviceDetails(), controller.getName());
      }
   }

   private String missingIDEControllersMessage(VmSpec vmSpecification) {
      return String.format("First controller is not an IDE controller. Please verify that the VM spec is a correct master node: %s", vmSpecification);
   }

   private void ensureWebServerIsRunning() {
      final IPSocket webServerSocket = new IPSocket(webServerHost, webServerPort);
      if (!socketTester.apply(webServerSocket)) {
         throw new IllegalStateException(String.format("Web server is not running on host %s:%s which is needed to serve preseed.cfg.", webServerHost, webServerPort));
      }
   }

   private void ensureMachineIsLaunched(String vmName) {
      applyForMachine(manager, vmName, new LaunchMachineIfNotAlreadyRunning(manager, executionType, ""));
   }

   private void ensureMachineDevicesAttached(String vmName, IMedium medium, DeviceDetails deviceDetails, String controllerName) {
      lockMachineAndApply(manager, Write, vmName, new AttachMediumToMachineIfNotAlreadyAttached(deviceDetails, medium, controllerName));
   }

   private void ensureMachineHasMemory(String vmName, final long memorySize) {
      lockMachineAndApply(manager, Write, vmName, new ApplyMemoryToMachine(memorySize));
   }

   private void ensureNATNetworkingIsAppliedToMachine(String vmName, long slotId, NatAdapter natAdapter) {
      lockMachineAndApply(manager, Write, vmName, new AttachNATAdapterToMachineIfNotAlreadyExists(slotId, natAdapter));
   }

   public void ensureMachineHasIDEControllerNamed(String vmName, StorageController storageController) {
      lockMachineAndApply(manager, Write, checkNotNull(vmName, "vmName"),
              new AddIDEControllerIfNotExists(checkNotNull(storageController, "storageController")));
   }

   private String defaultInstallSequence(String vmName) {
      return "<Esc><Esc><Enter> "
              + "/install/vmlinuz noapic preseed/url=http://10.0.2.2:" + webServerPort + "/src/test/resources/preseed.cfg "
              + "debian-installer=en_US auto locale=en_US kbd-chooser/method=us " + "hostname=" + vmName + " "
              + "fb=false debconf/frontend=noninteractive "
              + "keyboard-configuration/layout=USA keyboard-configuration/variant=USA console-setup/ask_detect=false "
              + "initrd=/install/initrd.gz -- <Enter>";
   }

   private void sendKeyboardSequence(String keyboardSequence, String vmName) {
      String[] splitSequence = keyboardSequence.split(" ");
      StringBuilder sb = new StringBuilder();
      for (String line : splitSequence) {
         String converted = stringToKeycode(line);
         for (String word : converted.split("  ")) {
            sb.append("vboxmanage controlvm ").append(vmName).append(" keyboardputscancode ").append(word).append("; ");
            runScriptIfWordEndsWith(sb, word, "<Enter>");
            runScriptIfWordEndsWith(sb, word, "<Return>");
         }
      }
   }

   private void runScriptIfWordEndsWith(StringBuilder sb, String word, String key) {
      if (word.endsWith(KeyboardScancodes.SPECIAL_KEYBOARD_BUTTON_MAP.get(key))) {
         runScriptOnNode(hostId, sb.toString(), runAsRoot(false).wrapInInitScript(false));
         sb.delete(0, sb.length() - 1);
      }
   }

   private String stringToKeycode(String s) {
      StringBuilder keycodes = new StringBuilder();
      if (s.startsWith("<")) {
         String[] specials = s.split("<");
         for (int i = 1; i < specials.length; i++) {
            keycodes.append(KeyboardScancodes.SPECIAL_KEYBOARD_BUTTON_MAP.get("<" + specials[i])).append("  ");
         }
         return keycodes.toString();
      }

      int i = 0;
      while (i < s.length()) {
         String digit = s.substring(i, i + 1);
         String hex = KeyboardScancodes.NORMAL_KEYBOARD_BUTTON_MAP.get(digit);
         keycodes.append(hex).append(" ");
         if (i != 0 && i % 14 == 0)
            keycodes.append(" ");
         i++;
      }
      keycodes.append(KeyboardScancodes.SPECIAL_KEYBOARD_BUTTON_MAP.get("<Spacebar>")).append(" ");

      return keycodes.toString();
   }

   protected ExecResponse runScriptOnNode(String nodeId, String command, RunScriptOptions options) {
      return context.getComputeService().runScriptOnNode(nodeId, command, options);
   }

}
