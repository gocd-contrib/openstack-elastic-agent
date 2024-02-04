/*
 * Copyright 2019 Thoughtworks, Inc.
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

import cd.go.contrib.elasticagents.openstack.client.OpenStackInstances;
import cd.go.contrib.elasticagents.openstack.executors.*;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.*;
import cd.go.contrib.elasticagents.openstack.utils.ServerHealthMessages;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cd.go.contrib.elasticagents.openstack.Constants.PLUGIN_IDENTIFIER;

@Extension
public class OpenStackPlugin implements GoPlugin {

    public static final Logger LOG = Logger.getLoggerFor(OpenStackPlugin.class);
    private static Map<String, OpenStackInstances> clusterSpecificInstances;

    static {
        clusterSpecificInstances = new ConcurrentHashMap<>();
    }

    private PluginRequest pluginRequest;

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor accessor) {
        pluginRequest = new PluginRequest(accessor, new ServerHealthMessages());
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) {
        ClusterProfileProperties clusterProfileProperties;
        try {
            switch (Request.fromString(request.requestName())) {
                case REQUEST_CAPABILITIES:
                    return new GetCapabilitiesExecutor().execute();
                case REQUEST_SHOULD_ASSIGN_WORK:
                    final long startTimeMillis = System.currentTimeMillis();
                    LOG.debug("[REQUEST_SHOULD_ASSIGN_WORK]: startTimeMillis=[{}] request.requestBody()={}", startTimeMillis, request.requestBody());
                    ShouldAssignWorkRequest shouldAssignWorkRequest = ShouldAssignWorkRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = shouldAssignWorkRequest.clusterProfileProperties();
                    clusterProfileProperties.validate(shouldAssignWorkRequest.jobIdentifier().represent());
                    LOG.debug("REQUEST_SHOULD_ASSIGN_WORK: clusterProfileProperties={}", clusterProfileProperties);
                    final GoPluginApiResponse apiResponse = shouldAssignWorkRequest.executor(getAgentInstancesFor(clusterProfileProperties)).execute();
                    final long durationInMillis = System.currentTimeMillis() - startTimeMillis;
                    LOG.debug("[REQUEST_SHOULD_ASSIGN_WORK] assigning instance decision took {} millis", durationInMillis);
                    return apiResponse;
                case REQUEST_CREATE_AGENT:
                    LOG.debug("REQUEST_CREATE_AGENT: request.requestBody()={}", request.requestBody());
                    CreateAgentRequest createAgentRequest = CreateAgentRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = createAgentRequest.clusterProfileProperties();
                    clusterProfileProperties.validate(createAgentRequest.job().represent());
                    final OpenStackInstances instances = getAgentInstancesFor(clusterProfileProperties);
                    return createAgentRequest.executor(instances, pluginRequest).execute();
                case REQUEST_SERVER_PING:
                    LOG.debug("REQUEST_SERVER_PING: request.requestBody()={}", request.requestBody());
                    ServerPingRequest serverPingRequest = ServerPingRequest.fromJSON(request.requestBody());
                    List<ClusterProfileProperties> listOfClusterProfileProperties = serverPingRequest.allClusterProfileProperties();
                    for (ClusterProfileProperties prop : listOfClusterProfileProperties) {
                        prop.validate(serverPingRequest.toString());
                        getAgentInstancesFor(prop);
                    }
                    return serverPingRequest.executor(clusterSpecificInstances, pluginRequest).execute();
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
                    return jobCompletionRequest.executor(getAgentInstancesFor(clusterProfileProperties), pluginRequest).execute();
                case REQUEST_CLUSTER_PROFILE_CHANGED:
                    LOG.debug("NOOP REQUEST_CLUSTER_PROFILE_CHANGED: request.requestBody()={}", request.requestBody());
                    final UpdateClusterConfigurationRequest updateClusterConfigurationRequest = UpdateClusterConfigurationRequest.fromJSON(request.requestBody());
                    final OpenStackInstances agentInstances = getAgentInstancesFor(updateClusterConfigurationRequest.getOldClusterProfile());
                    updateClusterConfigurationRequest.executor(agentInstances).execute();
                case REQUEST_GET_CONFIG:
                    LOG.debug("NOOP REQUEST_GET_CONFIG: request.requestBody()={}", request.requestBody());
                default:
                    throw new UnhandledRequestTypeException(request.requestName());
            }
        } catch (Exception e) {
            String message = String.format("Exception in Request %s with message: %s", request.requestName(), e.getLocalizedMessage());
            pluginRequest.addServerHealthMessage("Exception", ServerHealthMessages.Type.WARNING, message);
            LOG.warn(message);
            throw new RuntimeException(message, e);
        }
    }

    private synchronized OpenStackInstances getAgentInstancesFor(ClusterProfileProperties clusterProfileProperties) {
        OpenStackInstances openStackInstances;
        final String uuid = clusterProfileProperties.uuid();
        LOG.debug("getAgentInstancesFor [{}]: uuid()={} clusterSpecificAgentInstances.size()={} ",
                this, uuid, clusterSpecificInstances.size());
        if (clusterSpecificInstances.containsKey(uuid)) {
            LOG.debug("getAgentInstancesFor [{}]: clusterProfileProperties={} does exist", this, uuid);
            openStackInstances = clusterSpecificInstances.get(uuid);
        } else {
            LOG.debug("getAgentInstancesFor [{}]: uuid={}, cluster={} does NOT exist, " +
                            "creating new OpenStackInstances",
                    this, uuid, clusterProfileProperties.getOpenstackEndpoint());
            openStackInstances = new OpenStackInstances(clusterProfileProperties);
            clusterSpecificInstances.put(uuid, openStackInstances);
            openStackInstances.refreshAll(pluginRequest);
        }
        return openStackInstances;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return PLUGIN_IDENTIFIER;
    }

}
