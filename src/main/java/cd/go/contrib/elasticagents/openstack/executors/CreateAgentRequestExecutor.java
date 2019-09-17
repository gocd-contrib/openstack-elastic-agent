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

import cd.go.contrib.elasticagents.openstack.AgentMatchResult;
import cd.go.contrib.elasticagents.openstack.Constants;
import cd.go.contrib.elasticagents.openstack.PluginRequest;
import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.client.ImageNotFoundException;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstances;
import cd.go.contrib.elasticagents.openstack.client.PendingAgent;
import cd.go.contrib.elasticagents.openstack.model.Agent;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.utils.ServerHealthMessages;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateAgentRequestExecutor implements RequestExecutor {

    private static final Logger LOG = Logger.getLoggerFor(CreateAgentRequestExecutor.class);
    private final CreateAgentRequest request;
    private final OpenStackInstances agentInstances;
    private final PluginRequest pluginRequest;

    public CreateAgentRequestExecutor(CreateAgentRequest request, OpenStackInstances agentInstances, PluginRequest pluginRequest) {
        this.request = request;
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        LOG.debug("[{}] [create-agent] {}", transactionId, request);
        ClusterProfileProperties settings = request.clusterProfileProperties();

        int matchingAgentCount = 0;
        List<String> idleAgentsFound = new ArrayList<>();

        final String profileMaxLimitStr = request.properties().get(Constants.OPENSTACK_MAX_INSTANCE_LIMIT);
        int maxInstanceLimit;
        if (StringUtils.isNotBlank(profileMaxLimitStr) && StringUtils.isNumeric(profileMaxLimitStr)) {
            maxInstanceLimit = Integer.parseInt(profileMaxLimitStr);
            LOG.debug("[{}] [create-agent] Using maxInstanceLimit from agent profile value: {}", transactionId, request);
        } else {
            maxInstanceLimit = Integer.parseInt(settings.getDefaultMaxInstanceLimit());
            LOG.debug("[{}] [create-agent] Using maxInstanceLimit from cluster profile value: {}", transactionId, request);
        }

        String requestImageId = agentInstances.getImageId(request.properties(), transactionId);
        String flavorId = agentInstances.getFlavorId(request.properties(), transactionId);

        for (PendingAgent agent : agentInstances.getPendingAgents()) {
            LOG.debug("[{}] [create-agent] Check if pending agent {} match job profile", transactionId, agent);
            AgentMatchResult matchResult = agent.match(transactionId, requestImageId, flavorId, request.environment(), request.job());
            if (matchResult.isJobMatch()) {
                LOG.info("[{}] [create-agent] Will NOT create new instance for job {}, agent is still being created {} ",
                        transactionId, request.job().represent(), agent.elasticAgentId());
                return new DefaultGoPluginApiResponse(200);
            }
            if (matchResult.isProfileMatch()) {
                LOG.debug("[{}] [create-agent] found matching pending agent {} ", transactionId, agent.elasticAgentId());
                matchingAgentCount++;
            }
        }

        final String profileMinLimitStr = request.properties().get(Constants.OPENSTACK_MIN_INSTANCE_LIMIT);
        int minInstanceLimit;
        if (StringUtils.isNotBlank(profileMinLimitStr) && StringUtils.isNumeric(profileMinLimitStr)) {
            minInstanceLimit = Integer.parseInt(profileMinLimitStr);
            LOG.debug("[{}] [create-agent] Using minInstanceLimit from agent profile value: {}", transactionId, request);
        } else {
            minInstanceLimit = Integer.parseInt(settings.getDefaultMinInstanceLimit());
            LOG.debug("[{}] [create-agent] Using minInstanceLimit from cluster profile value: {}", transactionId, request);
        }

        for (Agent agent : pluginRequest.listAgents().agents()) {
            LOG.debug("[{}] [create-agent] Check if agent {} match job {}", transactionId, agent, request.job().represent());
            if (agentInstances.matchInstance(agent.elasticAgentId(), request.properties(), request.environment(),
                    transactionId, false)) {
                matchingAgentCount++;
                LOG.debug("[{}] [create-agent] found matching agent {} for job {}", transactionId, agent.elasticAgentId(), request.job().represent());
                if ((agent.agentState() == Agent.AgentState.Idle)) {
                    idleAgentsFound.add(agent.elasticAgentId());
                    LOG.info("[{}] [create-agent] found {} matching idle agent {} for job {}",
                            transactionId, idleAgentsFound, agent.elasticAgentId(), request.job().represent());
                    if (idleAgentsFound.size() >= minInstanceLimit) {
                        LOG.info("[{}] [create-agent] Will NOT create new instance, found {} matching idle agent {} for job {}",
                                transactionId, idleAgentsFound.size(), idleAgentsFound, request.job().represent());
                        return new DefaultGoPluginApiResponse(200);
                    }
                }
            }
        }

        if (matchingAgentCount >= maxInstanceLimit) {
            String maxLimitExceededMessage = String.format("Will NOT create new instance for job %s, has reached max instance limit of %s",
                    request.job().represent(), maxInstanceLimit);
            pluginRequest.addServerHealthMessage("maxLimitExceededMessage", ServerHealthMessages.Type.WARNING, maxLimitExceededMessage);
            LOG.warn("[{}] [create-agent] {}", transactionId, maxLimitExceededMessage);
            return new DefaultGoPluginApiResponse(200);
        }

        try {
            agentInstances.create(request, transactionId);
            LOG.info("[{}] [create-agent] Will create new agent since no matching agents found", transactionId);
        } catch (ImageNotFoundException ex) {
            final String errorMsg = "Cannot create new agent since no image found";
            LOG.error("[{}] [create-agent] " + errorMsg, transactionId);
            pluginRequest.addServerHealthMessage("ImageNotFoundException", ServerHealthMessages.Type.ERROR, errorMsg);
        }
        return new DefaultGoPluginApiResponse(200);
    }
}
