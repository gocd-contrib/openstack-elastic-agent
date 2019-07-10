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
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.ServerPingRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServerPingRequestExecutor implements RequestExecutor {

    private static final Logger LOG = Logger.getLoggerFor(ServerPingRequestExecutor.class);
    private final ServerPingRequest serverPingRequest;
    private Map<String, OpenStackInstances> clusterSpecificAgentInstances;
    private final PluginRequest pluginRequest;

    public ServerPingRequestExecutor(ServerPingRequest serverPingRequest, Map<String, OpenStackInstances> clusterSpecificAgentInstances, PluginRequest pluginRequest) {
        this.serverPingRequest = serverPingRequest;
        this.clusterSpecificAgentInstances = clusterSpecificAgentInstances;
        this.pluginRequest = pluginRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        List<ClusterProfileProperties> allClusterProfileProperties = serverPingRequest.allClusterProfileProperties();
        LOG.debug("[Server Ping] execute() serverPingRequest.allClusterProfileProperties.size()={}", serverPingRequest.allClusterProfileProperties().size());

        for (ClusterProfileProperties clusterProfileProperties : allClusterProfileProperties) {
            performCleanupForACluster(clusterProfileProperties, clusterSpecificAgentInstances.get(clusterProfileProperties.uuid()));
        }

        checkForPossiblyMissingAgents();
        return DefaultGoPluginApiResponse.success("");
    }

    private void performCleanupForACluster(ClusterProfileProperties clusterProfileProperties, OpenStackInstances instances) throws Exception {
        LOG.debug("[Server Ping] performCleanupForACluster clusterProfileProperties={}", clusterProfileProperties);
        Agents allAgents = pluginRequest.listAgents();
        Agents agentsToDisable = instances.instancesCreatedAfterTTL(clusterProfileProperties, allAgents);
        disableIdleAgents(agentsToDisable);

        allAgents = pluginRequest.listAgents();
        terminateDisabledAgents(allAgents, clusterProfileProperties, instances);

        instances.terminateUnregisteredInstances(clusterProfileProperties, allAgents);
    }

    private void checkForPossiblyMissingAgents() throws Exception {
        Collection<Agent> allAgents = pluginRequest.listAgents().agents();
        LOG.debug("[Server Ping] checkForPossiblyMissingAgents allAgents.size()={}", allAgents.size());

        List<Agent> missingAgents = allAgents.stream().filter(agent -> clusterSpecificAgentInstances.values().stream()
                .noneMatch(instances -> instances.hasInstance(agent.elasticAgentId()))).collect(Collectors.toList());
        LOG.debug("[Server Ping] checkForPossiblyMissingAgents missingAgents.size()={}", missingAgents.size());

        if (!missingAgents.isEmpty()) {
            List<String> missingAgentIds = missingAgents.stream().map(Agent::elasticAgentId).collect(Collectors.toList());
            LOG.warn("[Server Ping] Was expecting an instance with IDs " + missingAgentIds + ", but it was missing! Removing missing agents from config.");
            pluginRequest.disableAgents(missingAgents);
            pluginRequest.deleteAgents(missingAgents);
        }
    }

    private void disableIdleAgents(Agents agents) throws ServerRequestFailedException {
        Collection<Agent> instancesToDisable = agents.findInstancesToDisable();
        if (!instancesToDisable.isEmpty()) {
            pluginRequest.disableAgents(instancesToDisable);
        }
    }

    private void terminateDisabledAgents(Agents agents, ClusterProfileProperties clusterProfileProperties, OpenStackInstances instances) throws Exception {
        Collection<Agent> toBeDeleted = agents.findInstancesToTerminate();

        for (Agent agent : toBeDeleted) {
            instances.terminate(agent.elasticAgentId(), clusterProfileProperties);
        }

        pluginRequest.deleteAgents(toBeDeleted);
    }

}
