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
package org.apache.seata.server.cluster.raft.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.context.ContextCore;
import org.apache.seata.core.context.ContextCoreLoader;

import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 */
public class SeataClusterContext {

    //从系统或jvm的配置中得到raft的组名
    private static final String GROUP = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SERVER_RAFT_GROUP, DEFAULT_SEATA_GROUP);

    private SeataClusterContext() {
    }

    public static final String KEY_GROUP = "TX_GROUP";

    private static ContextCore CONTEXT_HOLDER = ContextCoreLoader.load();

    /**
     * 绑定组名，把KEY_GROUP绑定到传入的组名上
     *
     * @param group the group
     */
    public static void bindGroup(@Nonnull String group) {
        CONTEXT_HOLDER.put(KEY_GROUP, group);
    }

    /**
     * 绑定组名，把KEY_GROUP绑定到从系统配置获取的默认的组名上
     *
     */
    public static String bindGroup() {
        CONTEXT_HOLDER.put(KEY_GROUP, GROUP);
        return GROUP;
    }

    /**
     * 取消绑定
     */
    public static void unbindGroup() {
        CONTEXT_HOLDER.remove(KEY_GROUP);
    }

    @Nullable
    public static String getGroup() {
        return (String) CONTEXT_HOLDER.get(KEY_GROUP);
    }
}
