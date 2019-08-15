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

import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.client.AgentInstances;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.ShouldAssignWorkRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.UUID;

import static java.text.MessageFormat.format;

public class ShouldAssignWorkRequestExecutor implements RequestExecutor {
    private static final Logger LOG = Logger.getLoggerFor(ShouldAssignWorkRequestExecutor.class);
    private final AgentInstances agentInstances;
    private final ClusterProfileProperties clusterProfileProperties;
    private final ShouldAssignWorkRequest request;

    public ShouldAssignWorkRequestExecutor(ShouldAssignWorkRequest request, AgentInstances agentInstances, ClusterProfileProperties clusterProfileProperties) {
        this.request = request;
        this.agentInstances = agentInstances;
        this.clusterProfileProperties = clusterProfileProperties;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        String transactionId = UUID.randomUUID().toString();
        LOG.info(format("[{0}] [should-assign-work] {1}", transactionId, request));

        OpenStackInstance instance = (OpenStackInstance) agentInstances.find(request.agent().elasticAgentId());
        if (instance == null) {
            LOG.info(format("[{0}] [should-assign-work] Work can NOT be assigned to missing Agent {1}", transactionId, request.agent().elasticAgentId()));
            return DefaultGoPluginApiResponse.success("false");
        }

        LOG.debug(format("[{0}] [should-assign-work] {1} {2}", transactionId, request.elasticAgentProfileProperties(), clusterProfileProperties));

        if ((agentInstances.matchInstance(request.agent().elasticAgentId(), request.elasticAgentProfileProperties(), request.environment(),
                transactionId, clusterProfileProperties.getUsePreviousOpenstackImage()))) {
            LOG.info(format("[{0}] [should-assign-work] Work can be assigned to Agent {1}", transactionId, request.agent().elasticAgentId()));
            return DefaultGoPluginApiResponse.success("true");
        } else {
            LOG.info(format("[{0}] [should-assign-work] Work can NOT be assigned to Agent {1}", transactionId, request.agent().elasticAgentId()));
            return DefaultGoPluginApiResponse.success("false");
        }
    }
}