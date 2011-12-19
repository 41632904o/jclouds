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
package org.jclouds.openstack.swift.domain;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;


/**
 * 
 * @author Adrian Cole
 * 
 */
public class ContainerMetadata implements Comparable<ContainerMetadata> {
   private String name;
   private long count;
   private long bytes;
   @SerializedName("X-Container-Read")
   private String readACL;
   private Map<String, String> metadata = Maps.newLinkedHashMap();

   
   public ContainerMetadata() {
   }

   public ContainerMetadata(String name, long count, long bytes, String readACL, Map<String, String> metadata) {
      this.name = name;
      this.count = count;
      this.bytes = bytes;
      this.readACL = readACL;
      this.metadata = metadata;
   }

   public long getCount() {
      return count;
   }

   public void setCount(long count) {
	  this.count = count;
   }
   
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }
   
   public long getBytes() {
      return bytes;
   }
   
   public void setBytes(long bytes) {
      this.bytes = bytes;
   }
   
   public boolean isPublic() {
	   if (readACL == null)
		   return false;
	   return readACL.equals(".r:*,.rlistings");
   }
   
   public void setReadACL(String readACL) {
	   this.readACL = readACL;
	   
   }
   
   public Map<String, String> getMetadata() {
      return metadata;
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (bytes ^ (bytes >>> 32));
      result = prime * result + (int) (count ^ (count >>> 32));
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ContainerMetadata other = (ContainerMetadata) obj;
      if (bytes != other.bytes)
         return false;
      if (count != other.count)
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      return true;
   }

   public int compareTo(ContainerMetadata o) {
      if (getName() == null)
         return -1;
      return (this == o) ? 0 : getName().compareTo(o.getName());
   }

   @Override
   public String toString() {
		return "ContainerMetadata [name=" + name + ", count=" + count + ", bytes="
				+ bytes + ", isPublic=" + isPublic() + "]";
   }

}
