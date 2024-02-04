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

package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstances;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.UpdateClusterConfigurationRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;


public class UpdateClusterConfigurationExecutor implements RequestExecutor {

    public static final Logger LOG = Logger.getLoggerFor(UpdateClusterConfigurationExecutor.class);

    private UpdateClusterConfigurationRequest request;
    private OpenStackInstances instances;

    public UpdateClusterConfigurationExecutor(UpdateClusterConfigurationRequest request, OpenStackInstances instances) {
        this.request = request;
        this.instances = instances;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        LOG.info("[Change Cluster Config] Request for Config Update Started...");

        final String status = request.getStatus();
        final ClusterProfileProperties oldClusterProfile = request.getOldClusterProfile();
        final ClusterProfileProperties newClusterProfile = request.getNewClusterProfile();

        if (status.equalsIgnoreCase("updated")) {
            if (!oldClusterProfile.equals(newClusterProfile)) {
                LOG.info("[Change Cluster Config] oldClusterProfile and newClusterProfile has to be equal. Skipping Config Update...");
                LOG.debug("[Change Cluster Config] request.toJSON()={}", request.toJSON());
                return new DefaultGoPluginApiResponse(200, request.toJSON());
            }
            if (!newClusterProfile.equals(instances.getPluginSettings())) {
                LOG.info("[Change Cluster Config] newClusterProfile and pluginSettings has to be equal. Skipping Config Update...");
                LOG.debug("[Change Cluster Config] request.toJSON()={}", request.toJSON());
                return new DefaultGoPluginApiResponse(200, request.toJSON());
            }
            instances.setPluginSettings(newClusterProfile);

        }
        LOG.info("[Change Cluster Config] Using newClusterProfile.");
        return new DefaultGoPluginApiResponse(200, request.toJSON());
    }
}
