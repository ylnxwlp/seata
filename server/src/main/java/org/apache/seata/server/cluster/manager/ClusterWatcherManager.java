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
package org.apache.seata.server.cluster.manager;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.listener.ClusterChangeListener;
import org.apache.seata.server.cluster.watch.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 集群的监听者管理
 */
@Component
public class ClusterWatcherManager implements ClusterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    //把监听者放入一个队列中
    private static final Map<String, Queue<Watcher<?>>> WATCHERS = new ConcurrentHashMap<>();

    private static final Map<String, Long> GROUP_UPDATE_TIME = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // 响应超时的监视器，以一个固定的速率执行
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            for (String group : WATCHERS.keySet()) {
                //获取并移除当前组的所有观察者（如果有的话）
                Optional.ofNullable(WATCHERS.remove(group))
                    .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                        //对于每个观察者，检查其是否已超时
                        if (System.currentTimeMillis() >= watcher.getTimeout()) {
                            //如果超时，则设置状态，并通过异步上下文（AsyncContext）发送HTTP响应码SC_NOT_MODIFIED（304），表示资源未被修改，然后结束异步处理
                            HttpServletResponse httpServletResponse =
                                (HttpServletResponse)((AsyncContext)watcher.getAsyncContext()).getResponse();
                            watcher.setDone(true);
                            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                            ((AsyncContext)watcher.getAsyncContext()).complete();
                        }
                        if (!watcher.isDone()) {
                            // 如果观察者尚未完成（未超时），则重新注册该观察者以便后续继续监控
                            registryWatcher(watcher);
                        }
                    }));
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    //标记此方法为事件监听器
    @EventListener
    @Async
    public void onChangeEvent(ClusterChangeEvent event) {
        // 如果事件的任期号大于0，则认为这是一个有效的变更事件
        if (event.getTerm() > 0) {
            // 更新组的最后更新时间
            GROUP_UPDATE_TIME.put(event.getGroup(), event.getTerm());
            // 获取并移除与该组相关的所有观察者
            Optional.ofNullable(WATCHERS.remove(event.getGroup()))
                    .ifPresent(watchers -> watchers.parallelStream().forEach(this::notify));
        }
    }

    private void notify(Watcher<?> watcher) {
        //获取异步上下文
        AsyncContext asyncContext = (AsyncContext)watcher.getAsyncContext();
        //获取HTTP响应对象
        HttpServletResponse httpServletResponse = (HttpServletResponse)asyncContext.getResponse();
        //标记观察者为已完成
        watcher.setDone(true);
        if (logger.isDebugEnabled()) {
            logger.debug("notify cluster change event to: {}", asyncContext.getRequest().getRemoteAddr());
        }
        //设置HTTP响应状态
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        //完成异步请求
        asyncContext.complete();
    }

    public void registryWatcher(Watcher<?> watcher) {
        String group = watcher.getGroup();
        Long term = GROUP_UPDATE_TIME.get(group);
        //如果集群变化时间为空，亦或者是观察者认定的集群变化时间小于集群变化的时间，就把观察值放回原先队列中
        if (term == null || watcher.getTerm() >= term) {
            WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>()).add(watcher);
        } else {
            //说明集群变化，开始准备进行通知操作
            notify(watcher);
        }
    }

}
