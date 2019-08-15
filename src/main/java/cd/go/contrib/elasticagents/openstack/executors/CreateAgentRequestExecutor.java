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
import cd.go.contrib.elasticagents.openstack.client.AgentInstances;
import cd.go.contrib.elasticagents.openstack.client.ImageNotFoundException;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstance;
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

import static java.text.MessageFormat.format;

public class CreateAgentRequestExecutor implements RequestExecutor {

    private static final Logger LOG = Logger.getLoggerFor(CreateAgentRequestExecutor.class);
    private final CreateAgentRequest request;
    private final AgentInstances<OpenStackInstance> agentInstances;
    private final PluginRequest pluginRequest;
    private PendingAgentsService pendingAgentsService;

    public CreateAgentRequestExecutor(CreateAgentRequest request, AgentInstances<OpenStackInstance> agentInstances, PluginRequest pluginRequest,
                                      PendingAgentsService pendingAgentsService) {
        this.request = request;
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
        this.pendingAgentsService = pendingAgentsService;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        Agents agents = pluginRequest.listAgents();
        LOG.debug(format("[{0}] [create-agent] {1}", transactionId, request));
        ClusterProfileProperties settings = request.clusterProfileProperties();

        int matchingAgentCount = 0;
        List<String> idleAgentsFound = new ArrayList<>();

        final String profileMaxLimitStr = request.properties().get(Constants.OPENSTACK_MAX_INSTANCE_LIMIT);
        int maxInstanceLimit;
        if (StringUtils.isNotBlank(profileMaxLimitStr) && StringUtils.isNumeric(profileMaxLimitStr)) {
            maxInstanceLimit = Integer.parseInt(profileMaxLimitStr);
            LOG.debug(format("[{0}] [create-agent] Using maxInstanceLimit from agent profile value: {1}", transactionId, request));
        } else {
            maxInstanceLimit = Integer.parseInt(settings.getDefaultMaxInstanceLimit());
            LOG.debug(format("[{0}] [create-agent] Using maxInstanceLimit from cluster profile value: {1}", transactionId, request));
        }

        String requestImageId = agentInstances.getImageId(request.properties(), transactionId);
        String flavorId = agentInstances.getFlavorId(request.properties(), transactionId);

        for (PendingAgent agent : pendingAgentsService.getAgents()) {
            LOG.debug(format("[{0}] [create-agent] Check if pending agent {1} match job profile", transactionId, agent));
            AgentMatchResult matchResult = agent.match(transactionId, requestImageId, flavorId, request.environment(), request.job());
            if (matchResult.isJobMatch()) {
                LOG.info(format("[{0}] [create-agent] Will NOT create new instance for job {1}, agent is still being created {2} ",
                        transactionId, request.job().represent(), agent.elasticAgentId()));
                return new DefaultGoPluginApiResponse(200);
            }
            if (matchResult.isProfileMatch()) {
                LOG.debug(format("[{0}] [create-agent] found matching pending agent {1} ", transactionId, agent.elasticAgentId()));
                matchingAgentCount++;
            }
        }

        final String profileMinLimitStr = request.properties().get(Constants.OPENSTACK_MIN_INSTANCE_LIMIT);
        int minInstanceLimit;
        if (StringUtils.isNotBlank(profileMinLimitStr) && StringUtils.isNumeric(profileMinLimitStr)) {
            minInstanceLimit = Integer.parseInt(profileMinLimitStr);
            LOG.debug(format("[{0}] [create-agent] Using minInstanceLimit from agent profile value: {1}", transactionId, request));
        } else {
            minInstanceLimit = Integer.parseInt(settings.getDefaultMinInstanceLimit());
            LOG.debug(format("[{0}] [create-agent] Using minInstanceLimit from cluster profile value: {1}", transactionId, request));
        }

        for (Agent agent : agents.agents()) {
            LOG.debug(format("[{0}] [create-agent] Check if agent {1} match job {2}", transactionId, agent, request.job().represent()));
            if (agentInstances.matchInstance(agent.elasticAgentId(), request.properties(), request.environment(),
                    transactionId, false)) {
                matchingAgentCount++;
                LOG.debug(format("[{0}] [create-agent] found matching agent {1} for job {2}",
                        transactionId, agent.elasticAgentId(), request.job().represent()));
                if ((agent.agentState() == Agent.AgentState.Idle)) {
                    idleAgentsFound.add(agent.elasticAgentId());
                    LOG.info(format("[{0}] [create-agent] found {1} matching idle agent {2} for job {3}",
                            transactionId, idleAgentsFound, agent.elasticAgentId(), request.job().represent()));
                    if (idleAgentsFound.size() >= minInstanceLimit) {
                        LOG.info(format("[{0}] [create-agent] Will NOT create new instance, found {1} matching idle agent {2} for job {3}",
                                transactionId, minInstanceLimit, idleAgentsFound), request.job().represent());
                        return new DefaultGoPluginApiResponse(200);
                    }
                }
            }
        }

        if (matchingAgentCount >= maxInstanceLimit) {
            String maxLimitExceededMessage = String.format("Will NOT create new instance for job %s, has reached max instance limit of %s",
                    request.job().represent(), maxInstanceLimit);
            pluginRequest.addServerHealthMessage("maxLimitExceededMessage", ServerHealthMessages.Type.WARNING, maxLimitExceededMessage);
            LOG.warn(format("[{0}] [create-agent] {1}", transactionId, maxLimitExceededMessage));
            return new DefaultGoPluginApiResponse(200);
        }

        try {
            OpenStackInstance pendingInstance = agentInstances.create(request, transactionId);
            LOG.info(format("[{0}] [create-agent] Will create new agent since no matching agents found", transactionId));
            this.pendingAgentsService.addPending(pendingInstance, request);
        } catch (ImageNotFoundException ex) {
            final String errorMsg = "Cannot create new agent since no image found";
            LOG.error(format("[{0}] [create-agent] " + errorMsg, transactionId));
            pluginRequest.addServerHealthMessage("ImageNotFoundException", ServerHealthMessages.Type.ERROR, errorMsg);
        }
        return new DefaultGoPluginApiResponse(200);
    }
}
