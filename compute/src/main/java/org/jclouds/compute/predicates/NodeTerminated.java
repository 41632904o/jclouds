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
package org.jclouds.compute.predicates;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Resource;
import javax.inject.Singleton;

import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.compute.strategy.GetNodeMetadataStrategy;
import org.jclouds.logging.Logger;

import com.google.common.base.Predicate;
import com.google.inject.Inject;

/**
 * 
 * Tests to see if a node is deleted
 * 
 * @author Adrian Cole
 */
@Singleton
public class NodeTerminated implements Predicate<NodeMetadata> {

   private final GetNodeMetadataStrategy client;

   @Resource
   protected Logger logger = Logger.NULL;

   @Inject
   public NodeTerminated(GetNodeMetadataStrategy client) {
      this.client = client;
   }

   public boolean apply(NodeMetadata node) {
      logger.trace("looking for state on node %s", checkNotNull(node, "node"));
      node = refresh(node);
      if (node == null)
         return true;
      logger.trace("%s: looking for node state %s: currently: %s",
            node.getId(), NodeState.TERMINATED, node.getState());
      return node.getState() == NodeState.TERMINATED;
   }

   private NodeMetadata refresh(NodeMetadata node) {
      return client.getNode(node.getId());
   }
}
