/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.Agents;
import cd.go.contrib.elasticagents.openstack.PendingAgent;
import cd.go.contrib.elasticagents.openstack.PluginRequest;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;

import java.io.IOException;
import java.util.Map;

/**
 * Plugin implementors should implement these methods to interface to your cloud.
 * This interface is merely a suggestion for a very simple plugin. You may change it to your needs.
 */
public interface AgentInstances<T> {
    /**
     * This message is sent to request creation of an agent instance.
     * Implementations may, at their discretion choose to not spin up an agent instance.
     * <p>
     * So that instances created are auto-registered with the server, the agent instance should have an
     * <code>autoregister.properties</code>.
     *
     * @param request       the request object
     * @param transactionId
     */
    T create(CreateAgentRequest request, String transactionId) throws Exception;

    /**
     * This message is sent when the plugin needs to terminate the agent instance.
     *
     * @param agentId the elastic agent id, which is the same as OpenStack ID
     * @throws InstanceNotFoundException if the agent instance could not be found
     * @return
     */
    boolean terminate(String agentId) throws IOException;

    /**
     * Terminate instances that is not Pending nor Registered in GoCD server.
     *
     * @param agents the list of all the agents
     * @param pendingAgents
     */
    void terminateUnregisteredInstances(Agents agents, Map<String, PendingAgent> pendingAgents);

    boolean doesInstanceExist(String id) throws Exception;

    boolean matchInstance(String id, Map<String, String> properties, String environment,
                          String transactionId, boolean usePreviousImageId);

    /**
     * This message is sent from the {@link cd.go.contrib.elasticagents.openstack.executors.ServerPingRequestExecutor}
     * to filter out any expired agents. The TTL may be configurable and
     * set via the {@link PluginSettings} instance that is passed in through constructor.
     *
     * @param agents the list of all the agents
     * @return a list of agent instances which were created based on {@link PluginSettings#getAgentTTLMin()}
     * and {@link PluginSettings#getAgentTTLMax()}.
     */
    Agents fetchExpiredAgents(Agents agents);

    /**
     * This message is sent after plugin initialization time so that the plugin may connect to the cloud provider
     * and fetch a list of all instances that have been spun up by this plugin (before the server was shut down).
     * This call should ideally remember if the agent instances are refreshed from the cluster,
     * and do nothing if instances were previously refreshed.
     *
     * @param pluginRequest the plugin request object
     */
    void refreshAll(PluginRequest pluginRequest);

    /**
     * This
     * Returns an agent instance with the specified <code>id</code> or <code>null</code>, if the agent is not found.
     *
     * @param agentId the elastic agent id
     */
    T find(String agentId);

    boolean isInstanceInErrorState(String id) throws Exception;

    boolean hasPendingAgentTimedOut(String id);

    String getImageId(Map<String, String> properties, String transactionId) throws ImageNotFoundException;

    String getFlavorId(Map<String, String> properties, String transactionId);

    String getImageIdOrName(Map<String, String> properties);

    String getFlavorIdOrName(Map<String, String> properties);

    boolean hasInstance(String elasticAgentId);

    void performCleanup(PluginRequest pluginRequest);
}
