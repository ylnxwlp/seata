/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.cluster.raft.sync.msg;

/**
 * 集群同步的消息类型
 */
public enum RaftSyncMsgType {

    /**
     * 添加一个全局会话
     */
    ADD_GLOBAL_SESSION,
    /**
     * 移除一个全局会话
     */
    REMOVE_GLOBAL_SESSION,
    /**
     * 添加分支会话
     */
    ADD_BRANCH_SESSION,
    /**
     * 移除分支会话
     */
    REMOVE_BRANCH_SESSION,
    /**
     * 更新全局会话状态
     */
    UPDATE_GLOBAL_SESSION_STATUS,
    /**
     * 更新分支会话状态
     */
    UPDATE_BRANCH_SESSION_STATUS,
    /**
     * 释放全局会话锁
     */
    RELEASE_GLOBAL_SESSION_LOCK,
    /**
     * 释放分支会话锁
     */
    RELEASE_BRANCH_SESSION_LOCK,
    /**
     * 刷新集群元数据
     */
    REFRESH_CLUSTER_METADATA;
}
