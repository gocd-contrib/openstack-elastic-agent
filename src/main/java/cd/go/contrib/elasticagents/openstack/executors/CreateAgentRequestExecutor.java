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
import cd.go.contrib.elasticagents.openstack.utils.OpenstackClientWrapper;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.lang.StringUtils;

import java.util.UUID;

import static java.text.MessageFormat.format;

public class CreateAgentRequestExecutor implements RequestExecutor {

    private static final Logger LOG = Logger.getLoggerFor(CreateAgentRequestExecutor.class);
    private final CreateAgentRequest request;
    private final AgentInstances agentInstances;
    private final PluginRequest pluginRequest;
    private final OpenstackClientWrapper clientWrapper;

    public CreateAgentRequestExecutor(CreateAgentRequest request, AgentInstances agentInstances, PluginRequest pluginRequest,
                                      OpenstackClientWrapper clientWrapper) throws Exception {
        this.request = request;
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
        this.clientWrapper = clientWrapper;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        Agents agents = pluginRequest.listAgents();
        LOG.debug(format("[{0}] [create-agent] {1}", transactionId, request));

        int matchingAgentCount = 0;
        int maxInstanceLimit;

        final String profileMaxLimitStr = request.properties().get(Constants.OPENSTACK_MAX_INSTANCE_LIMIT);
        if (StringUtils.isNotBlank(profileMaxLimitStr) && StringUtils.isNumeric(profileMaxLimitStr)) {
            maxInstanceLimit = Integer.parseInt(profileMaxLimitStr);
            LOG.debug(format("[{0}] [create-agent] Using maxInstanceLimit from profile value: {1}", transactionId, request));
        } else {
            maxInstanceLimit = Integer.parseInt(pluginRequest.getPluginSettings().getDefaultMaxInstanceLimit());
            LOG.debug(format("[{0}] [create-agent] Using maxInstanceLimit from default plugin value: {1}", transactionId, request));
        }

        for (Agent agent : agents.agents()) {
            LOG.debug(format("[{0}] [create-agent] Check if agent {1} match job profile", transactionId, agent));
            if (agentInstances.matchInstance(agent.elasticAgentId(), request.properties(), request.environment(), pluginRequest.getPluginSettings(),
                    clientWrapper, transactionId, false)) {
                matchingAgentCount++;
                LOG.debug(format("[{0}] [create-agent] found matching agent {1} ", transactionId, agent.elasticAgentId()));
                if (matchingAgentCount >= maxInstanceLimit) {
                    LOG.info(format("[{0}] [create-agent] Will NOT create new instance, has reached max instance limit of {1} ", transactionId, maxInstanceLimit));
                    return new DefaultGoPluginApiResponse(200);
                } else if ((agent.agentState() == Agent.AgentState.Idle)) {
                    LOG.info(format("[{0}] [create-agent] Will NOT create new instance, found matching idle agent {1} ", transactionId, agent.elasticAgentId()));
                    return new DefaultGoPluginApiResponse(200);
                }
            }
        }
        LOG.info(format("[{0}] [create-agent] Will create new agent since no matching agents found", transactionId));
        agentInstances.create(request, pluginRequest.getPluginSettings(), transactionId);
        return new DefaultGoPluginApiResponse(200);
    }
}
