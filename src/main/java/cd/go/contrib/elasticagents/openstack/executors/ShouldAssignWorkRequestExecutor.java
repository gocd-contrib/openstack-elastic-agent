/*
 * Copyright 2016 Thoughtworks, Inc.
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
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstances;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.ShouldAssignWorkRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.UUID;

public class ShouldAssignWorkRequestExecutor implements RequestExecutor {
    private static final Logger LOG = Logger.getLoggerFor(ShouldAssignWorkRequestExecutor.class);
    private final OpenStackInstances openStackInstances;
    private final ClusterProfileProperties clusterProfileProperties;
    private final ShouldAssignWorkRequest request;

    public ShouldAssignWorkRequestExecutor(ShouldAssignWorkRequest request, OpenStackInstances openStackInstances, ClusterProfileProperties clusterProfileProperties) {
        this.request = request;
        this.openStackInstances = openStackInstances;
        this.clusterProfileProperties = clusterProfileProperties;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        final long startTimeMillis = System.currentTimeMillis();
        String transactionId = UUID.randomUUID().toString();
        LOG.info("[{}] [should-assign-work] startTimeMillis=[{}] {}", transactionId, startTimeMillis, request);

        OpenStackInstance instance = openStackInstances.find(request.agent().elasticAgentId());
        if (instance == null) {
            LOG.info("[{}] [should-assign-work] Work can NOT be assigned to missing Agent {}", transactionId, request.agent().elasticAgentId());
            return DefaultGoPluginApiResponse.success("false");
        }

        LOG.debug("[{}] [should-assign-work] {} {}", transactionId, request.elasticAgentProfileProperties(), clusterProfileProperties);

        if ((openStackInstances.matchInstance(request.agent().elasticAgentId(), request.elasticAgentProfileProperties(), request.environment(),
                transactionId, clusterProfileProperties.getUsePreviousOpenstackImage()))) {
            LOG.info("[{}] [should-assign-work] Work can be assigned to Agent {} in {} millis", transactionId, request.agent().elasticAgentId(), (System.currentTimeMillis() - startTimeMillis));
            return DefaultGoPluginApiResponse.success("true");
        } else {
            LOG.info("[{}] [should-assign-work] Work can NOT be assigned to Agent {} in {} millis", transactionId, request.agent().elasticAgentId(), (System.currentTimeMillis() - startTimeMillis));
            return DefaultGoPluginApiResponse.success("false");
        }
    }
}