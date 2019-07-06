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

package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.*;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Collection;

public class ServerPingRequestExecutor implements RequestExecutor {

    private static final Logger LOG = Logger.getLoggerFor(ServerPingRequestExecutor.class);
    private final AgentInstances agentInstances;
    private final PluginRequest pluginRequest;

    public ServerPingRequestExecutor(AgentInstances agentInstances, PluginRequest pluginRequest) {
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        PluginSettings pluginSettings = pluginRequest.getPluginSettings();
        if (pluginSettings == null) {
            LOG.warn("Openstack elastic agents plugin settings are empty");
            return DefaultGoPluginApiResponse.success("");
        }

        Agents agents = pluginRequest.listAgents();
        Agents missingAgents = new Agents();

        for (Agent agent : agents.agents()) {
            if (agentInstances.find(agent.elasticAgentId()) == null) {
                missingAgents.add(agent);
            } else {
                if (agent.agentState() == Agent.AgentState.LostContact) {
                    if (!agentInstances.doesInstanceExist(pluginSettings, agent.elasticAgentId())) {
                        missingAgents.add(agent);
                    }
                }
            }
        }
        disableIdleAgents(missingAgents);

        Agents idleAgents = agentInstances.instancesCreatedAfterTTL(pluginSettings, agents);
        disableIdleAgents(idleAgents);
        terminateDisabledAgents(idleAgents, pluginSettings);

        idleAgents.addAll(missingAgents);
        deleteDisabledAgents(idleAgents);

        agents = pluginRequest.listAgents();
        agentInstances.terminateUnregisteredInstances(pluginSettings, agents);

        return DefaultGoPluginApiResponse.success("");
    }

    private void disableIdleAgents(Agents agents) throws ServerRequestFailedException {
        this.pluginRequest.disableAgents(agents.findInstancesToDisable());
    }

    private void deleteDisabledAgents(Agents agents) throws ServerRequestFailedException {
        this.pluginRequest.deleteAgents(agents.findInstancesToTerminate());
    }

    private void terminateDisabledAgents(Agents agents, PluginSettings pluginSettings) throws Exception {
        Collection<Agent> toBeDeleted = agents.findInstancesToTerminate();

        for (Agent agent : toBeDeleted) {
            agentInstances.terminate(agent.elasticAgentId(), pluginSettings);
        }

    }

}
