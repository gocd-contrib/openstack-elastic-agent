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
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.utils.ImageNotFoundException;
import cd.go.contrib.elasticagents.openstack.utils.OpenstackClientWrapper;
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
    private final OpenstackClientWrapper clientWrapper;
    private PendingAgentsService pendingAgentsService;

    public CreateAgentRequestExecutor(CreateAgentRequest request, AgentInstances<OpenStackInstance> agentInstances, PluginRequest pluginRequest,
                                      OpenstackClientWrapper clientWrapper, PendingAgentsService pendingAgentsService) throws Exception {
        this.request = request;
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
        this.clientWrapper = clientWrapper;
        this.pendingAgentsService = pendingAgentsService;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        Agents agents = pluginRequest.listAgents();
        LOG.debug(format("[{0}] [create-agent] {1}", transactionId, request));

        int matchingAgentCount = 0;
        List<String> idleAgentsFound = new ArrayList<>();

        final String profileMaxLimitStr = request.properties().get(Constants.OPENSTACK_MAX_INSTANCE_LIMIT);
        int maxInstanceLimit;
        if (StringUtils.isNotBlank(profileMaxLimitStr) && StringUtils.isNumeric(profileMaxLimitStr)) {
            maxInstanceLimit = Integer.parseInt(profileMaxLimitStr);
            LOG.debug(format("[{0}] [create-agent] Using maxInstanceLimit from profile value: {1}", transactionId, request));
        } else {
            maxInstanceLimit = Integer.parseInt(pluginRequest.getPluginSettings().getDefaultMaxInstanceLimit());
            LOG.debug(format("[{0}] [create-agent] Using maxInstanceLimit from default plugin value: {1}", transactionId, request));
        }

        PluginSettings pluginSettings = pluginRequest.getPluginSettings();
        String requestImageId = OpenStackInstance.getImageIdOrName(request.properties(), pluginSettings);
        requestImageId = clientWrapper.getImageId(requestImageId, transactionId);
        String flavorId = OpenStackInstance.getFlavorIdOrName(request.properties(), pluginSettings);
        flavorId = clientWrapper.getFlavorId(flavorId, transactionId);

        for (PendingAgent agent : pendingAgentsService.getAgents()) {
            LOG.debug(format("[{0}] [create-agent] Check if pending agent {1} match job profile", transactionId, agent));
            AgentMatchResult matchResult = agent.match(transactionId, requestImageId, flavorId, request.environment(), request.job());
            if (matchResult.isJobMatch()) {
                LOG.info(format("[{0}] [create-agent] Will NOT create new instance, agent for job {1} is still being created {2} ", transactionId, request.job().getRepresentation(), agent.elasticAgentId()));
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
            LOG.debug(format("[{0}] [create-agent] Using minInstanceLimit from profile value: {1}", transactionId, request));
        } else {
            minInstanceLimit = Integer.parseInt(pluginRequest.getPluginSettings().getDefaultMinInstanceLimit());
            LOG.debug(format("[{0}] [create-agent] Using minInstanceLimit from default plugin value: {1}", transactionId, request));
        }

        for (Agent agent : agents.agents()) {
            LOG.debug(format("[{0}] [create-agent] Check if agent {1} match job profile", transactionId, agent));
            if (agentInstances.matchInstance(agent.elasticAgentId(), request.properties(), request.environment(), pluginRequest.getPluginSettings(),
                    clientWrapper, transactionId, false)) {
                matchingAgentCount++;
                LOG.debug(format("[{0}] [create-agent] found matching agent {1} ", transactionId, agent.elasticAgentId()));
                if ((agent.agentState() == Agent.AgentState.Idle)) {
                    idleAgentsFound.add(agent.elasticAgentId());
                    LOG.info(format("[{0}] [create-agent] found {1} matching idle agent {2} ", transactionId, idleAgentsFound, agent.elasticAgentId()));
                    if (idleAgentsFound.size() >= minInstanceLimit) {
                        LOG.info(format("[{0}] [create-agent] Will NOT create new instance, found {1} matching idle agent {2} ", transactionId, minInstanceLimit, idleAgentsFound));
                        return new DefaultGoPluginApiResponse(200);
                    }
                }
            }
        }

        if (matchingAgentCount >= maxInstanceLimit) {
            LOG.info(format("[{0}] [create-agent] Will NOT create new instance, has reached max instance limit of {1} ", transactionId, maxInstanceLimit));
            return new DefaultGoPluginApiResponse(200);
        }

        try {
            OpenStackInstance pendingInstance = agentInstances.create(request, pluginRequest.getPluginSettings(), transactionId);
            LOG.info(format("[{0}] [create-agent] Will create new agent since no matching agents found", transactionId));
            this.pendingAgentsService.addPending(pendingInstance, request);
        } catch (ImageNotFoundException ex) {
            LOG.error(format("[{0}] [create-agent] Cannot create new agent since no image found", transactionId));
        }
        return new DefaultGoPluginApiResponse(200);
    }
}
