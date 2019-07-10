/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.executors.*;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.*;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cd.go.contrib.elasticagents.openstack.Constants.PLUGIN_IDENTIFIER;

@Extension
public class OpenStackPlugin implements GoPlugin {

    public static final Logger LOG = Logger.getLoggerFor(OpenStackPlugin.class);

    private Map<String, OpenStackInstances> clusterSpecificAgentInstances;
    private PluginRequest pluginRequest;
    private Map<String, PendingAgentsService> clusterSpecificPendingAgents;


    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor accessor) {
        pluginRequest = new PluginRequest(accessor);
        clusterSpecificAgentInstances = new HashMap<>();
        clusterSpecificPendingAgents = new HashMap<>();
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) {
        ClusterProfileProperties clusterProfileProperties;
        try {
            switch (Request.fromString(request.requestName())) {
                case REQUEST_CAPABILITIES:
                    return new GetCapabilitiesExecutor().execute();
                case REQUEST_SHOULD_ASSIGN_WORK:
                    LOG.debug("REQUEST_SHOULD_ASSIGN_WORK: request.requestBody()={}", request.requestBody());
                    ShouldAssignWorkRequest shouldAssignWorkRequest = ShouldAssignWorkRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = shouldAssignWorkRequest.clusterProfileProperties();
                    clusterProfileProperties.validate(shouldAssignWorkRequest.jobIdentifier().represent());
                    LOG.debug("REQUEST_SHOULD_ASSIGN_WORK: clusterProfileProperties={}", clusterProfileProperties);
                    return shouldAssignWorkRequest.executor(getAgentInstancesFor(clusterProfileProperties)).execute();
                case REQUEST_CREATE_AGENT:
                    LOG.debug("REQUEST_CREATE_AGENT: request.requestBody()={}", request.requestBody());
                    CreateAgentRequest createAgentRequest = CreateAgentRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = createAgentRequest.clusterProfileProperties();
                    clusterProfileProperties.validate(createAgentRequest.job().represent());
                    LOG.debug("REQUEST_CREATE_AGENT: clusterProfileProperties={}", clusterProfileProperties);
                    refreshInstancesForCluster(clusterProfileProperties);
                    LOG.debug("REQUEST_CREATE_AGENT: before refreshPendingInstancesForCluster()");
                    refreshPendingInstancesForCluster(clusterProfileProperties);
                    LOG.debug("REQUEST_CREATE_AGENT: before execute()");
                    final PendingAgentsService pendingAgentsService = getPendingInstancesFor(clusterProfileProperties);
                    final AgentInstances<OpenStackInstance> instances = getAgentInstancesFor(clusterProfileProperties);
                    return createAgentRequest.executor(pendingAgentsService, instances, pluginRequest).execute();
                case REQUEST_SERVER_PING:
                    LOG.debug("REQUEST_SERVER_PING: request.requestBody()={}", request.requestBody());
                    ServerPingRequest serverPingRequest = ServerPingRequest.fromJSON(request.requestBody());
                    List<ClusterProfileProperties> listOfClusterProfileProperties = serverPingRequest.allClusterProfileProperties();
                    for (ClusterProfileProperties prop : listOfClusterProfileProperties) {
                        prop.validate(serverPingRequest.toString());
                    }
                    refreshInstancesForAllClusters(listOfClusterProfileProperties);
                    pluginRequest.removeServerHealthMessage();
                    return serverPingRequest.executor(clusterSpecificAgentInstances, pluginRequest).execute();
                case REQUEST_GET_CLUSTER_PROFILE_METADATA:
                    return new GetClusterProfileMetadataExecutor().execute();
                case REQUEST_GET_CLUSTER_PROFILE_VIEW:
                    return new GetClusterProfileViewRequestExecutor().execute();
                case REQUEST_GET_ELASTIC_AGENT_PROFILE_METADATA:
                    return new GetProfileMetadataExecutor().execute();
                case REQUEST_GET_ELASTIC_AGENT_PROFILE_VIEW:
                    return new GetProfileViewExecutor().execute();
                case REQUEST_GET_ICON:
                    return new GetPluginSettingsIconExecutor().execute();
                case REQUEST_VALIDATE_CLUSTER_PROFILE_CONFIGURATION:
                    LOG.debug("REQUEST_VALIDATE_CLUSTER_PROFILE_CONFIGURATION: request.requestBody()={}", request.requestBody());
                    return ClusterProfileValidateRequest.fromJSON(request.requestBody()).executor().execute();
                case REQUEST_VALIDATE_ELASTIC_AGENT_PROFILE:
                    LOG.debug("REQUEST_VALIDATE_ELASTIC_AGENT_PROFILE: request.requestBody()={}", request.requestBody());
                    return ProfileValidateRequest.fromJSON(request.requestBody()).executor().execute();
                case REQUEST_MIGRATE_CONFIGURATION:
                    LOG.debug("REQUEST_MIGRATE_CONFIGURATION: request.requestBody()={}", request.requestBody());
                    final GoPluginApiResponse response = MigrateConfigurationRequest.fromJSON(request.requestBody()).executor().execute();
                    LOG.debug("REQUEST_MIGRATE_CONFIGURATION: response.responseBody={}", response.responseBody());
                    return response;
                case REQUEST_JOB_COMPLETION:
                    LOG.debug("REQUEST_JOB_COMPLETION: request.requestBody()={}", request.requestBody());
                    JobCompletionRequest jobCompletionRequest = JobCompletionRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = jobCompletionRequest.getClusterProfileProperties();
                    clusterProfileProperties.validate(jobCompletionRequest.jobIdentifier().represent());
                    refreshInstancesForCluster(clusterProfileProperties);
                    return jobCompletionRequest.executor(getAgentInstancesFor(clusterProfileProperties), pluginRequest).execute();
                case REQUEST_CLUSTER_PROFILE_CHANGED:
                    LOG.debug("REQUEST_CLUSTER_PROFILE_CHANGED: request.requestBody()={}", request.requestBody());
                default:
                    throw new UnhandledRequestTypeException(request.requestName());
            }
        } catch (Exception e) {
            List<Map<String, String>> messages = new ArrayList<>();
            String message = String.format("Exception in Request %s with message: %s", request.requestName(), e.getLocalizedMessage());
            Map<String, String> messageToBeAdded = new HashMap<>();
            messageToBeAdded.put("type", "error");
            messageToBeAdded.put("message", message);
            messages.add(messageToBeAdded);
            pluginRequest.addServerHealthMessage(messages);
            LOG.warn(message);
            throw new RuntimeException(e);
        }
    }

    private void refreshInstancesForAllClusters(List<ClusterProfileProperties> listOfClusterProfileProperties) throws Exception {
        for (ClusterProfileProperties clusterProfileProperties : listOfClusterProfileProperties) {
            refreshInstancesForCluster(clusterProfileProperties);
        }
    }

    private AgentInstances<OpenStackInstance> getAgentInstancesFor(ClusterProfileProperties clusterProfileProperties) {
        return clusterSpecificAgentInstances.get(clusterProfileProperties.uuid());
    }

    private void refreshInstancesForCluster(ClusterProfileProperties clusterProfileProperties) throws Exception {
        OpenStackInstances openStackInstances = clusterSpecificAgentInstances.getOrDefault(clusterProfileProperties.uuid(), new OpenStackInstances());
        openStackInstances.refreshAll(pluginRequest, clusterProfileProperties);
        clusterSpecificAgentInstances.put(clusterProfileProperties.uuid(), openStackInstances);
    }

    private PendingAgentsService getPendingInstancesFor(ClusterProfileProperties clusterProfileProperties) {
        return clusterSpecificPendingAgents.get(clusterProfileProperties.uuid());
    }

    private void refreshPendingInstancesForCluster(ClusterProfileProperties clusterProfileProperties) throws Exception {
        PendingAgentsService pendingAgents = clusterSpecificPendingAgents.getOrDefault(clusterProfileProperties.uuid(),
                new PendingAgentsService(getAgentInstancesFor(clusterProfileProperties)));
        pendingAgents.refreshAll(pluginRequest, clusterProfileProperties);
        clusterSpecificPendingAgents.put(clusterProfileProperties.uuid(), pendingAgents);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return PLUGIN_IDENTIFIER;
    }

}
