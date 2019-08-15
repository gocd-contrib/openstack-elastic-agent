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

import cd.go.contrib.elasticagents.openstack.Agent;
import cd.go.contrib.elasticagents.openstack.PluginRequest;
import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.client.AgentInstances;
import cd.go.contrib.elasticagents.openstack.requests.ServerPingRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerPingRequestExecutor implements RequestExecutor {

    private static final Logger LOG = Logger.getLoggerFor(ServerPingRequestExecutor.class);
    private final ServerPingRequest serverPingRequest;
    private final PluginRequest pluginRequest;
    private Map<String, AgentInstances> clusterSpecificAgentInstances;

    public ServerPingRequestExecutor(ServerPingRequest serverPingRequest, Map<String, AgentInstances> clusterSpecificAgentInstances, PluginRequest pluginRequest) {
        this.serverPingRequest = serverPingRequest;
        this.clusterSpecificAgentInstances = clusterSpecificAgentInstances;
        this.pluginRequest = pluginRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        LOG.debug("[execute] clusterSpecificAgentInstances.size()={}", clusterSpecificAgentInstances.size());

        for (AgentInstances agentInstances : clusterSpecificAgentInstances.values()) {
            agentInstances.performCleanup(pluginRequest);
        }

        // FIXME: 2019-08-18 Is it needed?
        // This seems to be performed in cd.go.contrib.elasticagents.openstack.PendingAgentsService
        checkForPossiblyMissingAgents();
        return DefaultGoPluginApiResponse.success("");
    }

    private void checkForPossiblyMissingAgents() throws Exception {
        Collection<Agent> allAgents = pluginRequest.listAgents().agents();
        LOG.debug("[checkForPossiblyMissingAgents] allAgents.size()={}", allAgents.size());

        List<Agent> missingAgents = new ArrayList<>();
        for (Agent agent : allAgents) {
            if (clusterSpecificAgentInstances.values().stream()
                    .noneMatch(instances -> instances.hasInstance(agent.elasticAgentId()))) {
                missingAgents.add(agent);
            }
        }
        LOG.debug("[checkForPossiblyMissingAgents] missingAgents.size()={}", missingAgents.size());

        if (!missingAgents.isEmpty()) {
            List<String> missingAgentIds = missingAgents.stream().map(Agent::elasticAgentId).collect(Collectors.toList());
            LOG.warn("[checkForPossiblyMissingAgents] Was expecting an instance with IDs " + missingAgentIds + ", but it was missing! Removing missing agents from config.");
            pluginRequest.disableAgents(missingAgents);
            pluginRequest.deleteAgents(missingAgents);
        }
    }

}
