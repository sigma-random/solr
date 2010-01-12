package org.apache.solr.cloud;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * Information about a Shard.
 * 
 */
public final class ShardInfo {

  private final String url;
  //nocommit do role based on existing ReplicationHandler role detection?
  private final Role role;

  public ShardInfo(String url) {
    this.url = url;
    role = Role.SLAVE;
  }

  public ShardInfo(String url, Role role) {
    this.url = url;
    this.role = role;
  }

  public Role getRole() {
    return role;
  }

  public String getUrl() {
    return url;
  }
  

  enum Role {
    MASTER, SLAVE
  }
}