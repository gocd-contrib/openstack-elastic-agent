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

package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.Constants;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.Builders;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;

import java.time.Instant;
import java.util.*;

import static cd.go.contrib.elasticagents.openstack.Constants.OPENSTACK_USERDATA_ARGS;
import static cd.go.contrib.elasticagents.openstack.utils.Util.integerFromString;
import static java.text.MessageFormat.format;

public class OpenStackInstance {

    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstance.class);
    private final PluginSettings pluginSettings;
    private final Instant createdAt;
    private final String environment;
    private final String imageId;
    private final String flavorId;
    private String id;
    private Integer jobsCompleted = 0;
    private Integer maxCompletedJobs = 0;

    public OpenStackInstance(String id, Date createdAt, String environment, String imageId, String flavorId, PluginSettings pluginSettings) {
        this.id = id;
        this.createdAt = Instant.ofEpochMilli(createdAt.getTime());
        this.environment = environment;
        this.imageId = imageId;
        this.flavorId = flavorId;
        this.pluginSettings = pluginSettings;
    }

    static OpenStackInstance create(String instanceName, String imageNameOrId, String flavorNameOrId, String encodedUserData,
                                    String transactionId, PluginSettings pluginSettings, CreateAgentRequest request,
                                    OpenstackClientWrapper client) throws InstanceNotFoundException {
        LOG.debug("[{}] [create] job {} cluster {}", transactionId, request.job().represent(), pluginSettings.getOpenstackEndpoint());

        HashMap<String, String> mdata = new HashMap<>();

        Iterator entries = request.properties().entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            if (!entry.getKey().equals(OPENSTACK_USERDATA_ARGS)) {
                mdata.put((String) entry.getKey(), (String) entry.getValue());
            }
        }

        if (StringUtils.isNotBlank(request.autoRegisterKey())) {
            mdata.put(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.REGISTER_KEY, request.autoRegisterKey());
        }
        if (StringUtils.isNotBlank(request.environment())) {
            mdata.put(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY, request.environment());
        }
        mdata.put(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.PLUGIN_ID_KEY, Constants.PLUGIN_ID);

        // cc_openstack_gocd.py will take care of this
        // /mdata.put("goserver_agent.auto.register.elasticAgent.agentId", opsInstanceName);

        if (request.properties().containsKey(Constants.BOOTSTRAPPER_ARGS)) {
            mdata.put(Constants.GOAGENT_PROPERTIES_PREFIX + Constants.BOOTSTRAPPER_KEY, request.properties().get(Constants.BOOTSTRAPPER_ARGS));
        }
        mdata.put(Constants.GOAGENT_PROPERTIES_PREFIX + Constants.GO_SERVER_URL_KEY, request.properties().containsValue(Constants.GO_SERVER_URL_ARGS) ? request.properties().get(Constants.GO_SERVER_URL_ARGS) : pluginSettings.getGoServerUrl());

        LOG.debug("mdata.toString()={}", mdata.toString());
        LOG.debug(request.properties().toString());

        String networkId = StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_NETWORK_ID_ARGS)) ? request.properties().get(Constants.OPENSTACK_NETWORK_ID_ARGS) : pluginSettings.getOpenstackNetwork();
        LOG.debug("create before ServerCreateBuilder : PluginSettings={}", pluginSettings);
        ServerCreateBuilder scb = Builders.server()
                .image(imageNameOrId)
                .name(instanceName)
                .flavor(flavorNameOrId)
                .networks(Arrays.asList(networkId))
                .addMetadata(mdata);
        if (encodedUserData != null)
            scb = scb.userData(encodedUserData);
        LOG.debug("create after ServerCreateBuilder : PluginSettings={}", pluginSettings);

        if (StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_SECURITY_GROUP))) {
            scb.addSecurityGroup(request.properties().get(Constants.OPENSTACK_SECURITY_GROUP));
        }

        if (StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_KEYPAIR))) {
            scb.keypairName(request.properties().get(Constants.OPENSTACK_KEYPAIR));
        }

        LOG.debug("create before osclient.compute().servers().boot(scb.build()) : scb.build()={}", scb.build());
        Server server = client.bootServer(scb.build());
        LOG.debug("create after osclient.compute().servers().boot(scb.build()) : server.getPowerState()={}", server.getPowerState());

        // create instance properties ( image id, network id, etc... ) and pass to OpenstackInstance()
        server = client.getServer(server.getId());
        LOG.info(format("after boot : [{0}] {1} with status: {2}", server.getId(), server.getFlavorId(), server.getStatus()));
        return new OpenStackInstance(server.getId(), server.getCreated(), request.environment(), server.getImageId(), server.getFlavorId(), pluginSettings);
    }

    String environment() {
        return environment;
    }

    public String id() {
        return id;
    }

    public Integer getJobsCompleted() {
        return jobsCompleted;
    }

    public Integer getMaxCompletedJobs() {
        return maxCompletedJobs;
    }

    public void setMaxCompletedJobs(String maxCompletedJobs) {
        this.maxCompletedJobs = integerFromString(maxCompletedJobs);
        LOG.info(format("instance {0} set maxCompletedJobs={1}", id, maxCompletedJobs));
    }

    /**
     * Increment the job counter and return a boolean
     * indicating if the instance should be terminated.
     *
     * @return Boolean indicating if the instance has executed
     * more jobs than its configured maximum
     */
    public boolean incrementJobsCompleted() {
        jobsCompleted++;
        LOG.info(format("instance {0} has completed {1} jobs", id, jobsCompleted));
        return maxCompletedJobs != 0 && jobsCompleted >= maxCompletedJobs;
    }

    Instant createAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpenStackInstance that = (OpenStackInstance) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public String getImageIdOrName() {
        return imageId;
    }

    public String getFlavorIdOrName() {
        return flavorId;
    }

    @Override
    public String toString() {
        return "OpenStackInstance{" +
                "pluginSettings=" + pluginSettings +
                ", createdAt=" + createdAt +
                ", environment='" + environment + '\'' +
                ", imageId='" + imageId + '\'' +
                ", flavorId='" + flavorId + '\'' +
                ", id='" + id + '\'' +
                ", jobsCompleted=" + jobsCompleted +
                ", maxCompletedJobs=" + maxCompletedJobs +
                '}';
    }
}
