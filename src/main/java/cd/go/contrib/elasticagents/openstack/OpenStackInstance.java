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

package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.utils.ImageNotFoundException;
import cd.go.contrib.elasticagents.openstack.utils.OpenstackClientWrapper;
import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static cd.go.contrib.elasticagents.openstack.Constants.OPENSTACK_USERDATA_ARGS;

public class OpenStackInstance {

    private static final Gson GSON = new Gson();
    private String id;
    private final DateTime createdAt;
    private final String environment;
    private final String imageId;
    private final String flavorId;

    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstance.class);


    public String environment() {
        return environment;
    }

    public OpenStackInstance(String id, Date createdAt, String environment, String imageId, String flavorId) {
        this.id = id;
        this.createdAt = new DateTime(createdAt);
        this.environment = environment;
        this.imageId = imageId;
        this.flavorId = flavorId;
    }

    public OpenStackInstance(String id, Date createdAt, String environment, OSClient os_client) {
        this.id = id;
        this.createdAt = new DateTime(createdAt);
        this.environment = environment;
        Server server = os_client.compute().servers().get(id);
        this.imageId = server.getImageId();
        this.flavorId = server.getFlavorId();
    }

    public String id() {
        return id;
    }

    public DateTime createAt() {
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


    public static OpenStackInstance find(OSClient os_client, String instanceId) throws InterruptedException, OS4JException {
        Server server = os_client.compute().servers().get(instanceId);
        if (server.getId() == null)
            return null;
        return new OpenStackInstance(server.getId(),
                server.getCreated(),
                server.getMetadata().get(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY),
                os_client);
    }

    public void terminate(OSClient os_client) throws InterruptedException, OS4JException {
        os_client.compute().servers().delete(id);
    }

    public static OpenStackInstance create(CreateAgentRequest request, PluginSettings settings, OSClient osclient, String transactionId) throws OS4JException, ImageNotFoundException {

        String instance_name;

        HashMap<String, String> mdata = new HashMap<>();

        Iterator entries = request.properties().entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            if (!((String) entry.getKey()).equals(OPENSTACK_USERDATA_ARGS)) {
                mdata.put((String) entry.getKey(), (String) entry.getValue());
            }
        }

        if (StringUtils.isNotBlank(request.autoRegisterKey())) {
            mdata.put(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.REGISTER_KEY, request.autoRegisterKey());
        }
        if (!StringUtils.isBlank(request.environment())) {
            mdata.put(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY, request.environment());
        }
        mdata.put(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.PLUGIN_ID_KEY, Constants.PLUGIN_ID);

        // cc_openstack_gocd.py will take care of this
        // /mdata.put("goserver_agent.auto.register.elasticAgent.agentId", opsInstanceName);

        if (request.properties().containsKey(Constants.BOOTSTRAPPER_ARGS)) {
            mdata.put(Constants.GOAGENT_PROPERTIES_PREFIX + Constants.BOOTSTRAPPER_KEY, request.properties().get(Constants.BOOTSTRAPPER_ARGS));
        }
        mdata.put(Constants.GOAGENT_PROPERTIES_PREFIX + Constants.GO_SERVER_URL_KEY, request.properties().containsValue(Constants.GO_SERVER_URL_ARGS) ? request.properties().get(Constants.GO_SERVER_URL_ARGS) : settings.getGoServerUrl());


        instance_name = settings.getOpenstackVmPrefix() + RandomStringUtils.randomAlphanumeric(12).toLowerCase();
        Map<String, String> newInstance = new HashMap<>();
        newInstance.put("name", instance_name);
        while (osclient.compute().servers().list(newInstance).size() > 0) {
            instance_name = settings.getOpenstackVmPrefix() + RandomStringUtils.randomAlphanumeric(12).toLowerCase();
            newInstance.clear();
            newInstance.put("name", instance_name);
        }

        LOG.debug(mdata.toString());
        LOG.debug(request.properties().toString());

        final String encodedUserData = getEncodedUserData(request, settings);
        OpenstackClientWrapper client = new OpenstackClientWrapper(settings);
        String imageNameOrId = getImageIdOrName(request.properties(), settings);
        imageNameOrId = client.getImageId(imageNameOrId, transactionId);
        String flavorNameOrId = getFlavorIdOrName(request.properties(), settings);
        String networkId = StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_NETWORK_ID_ARGS)) ? request.properties().get(Constants.OPENSTACK_NETWORK_ID_ARGS) : settings.getOpenstackNetwork();
        ServerCreateBuilder scb = Builders.server()
                .image(imageNameOrId)
                .name(instance_name)
                .flavor(client.getFlavorId(flavorNameOrId, transactionId))
                .networks(Arrays.asList(networkId))
                .addMetadata(mdata);
        if (encodedUserData != null)
            scb = scb.userData(encodedUserData);

        if (StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_SECURITY_GROUP))) {
            scb.addSecurityGroup(request.properties().get(Constants.OPENSTACK_SECURITY_GROUP));
        }

        if (StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_KEYPAIR))) {
            scb.keypairName(request.properties().get(Constants.OPENSTACK_KEYPAIR));
        }

        Server server = osclient.compute().servers().boot(scb.build());

        // create instance properties ( image id, network id, etc... ) and pass to OpenstackInstance()

        return new OpenStackInstance(server.getId(), server.getCreated(), request.environment(), osclient);

    }

    public static String getFlavorIdOrName(Map<String, String> properties, PluginSettings settings) {
        return StringUtils.isNotBlank(properties.get(Constants.OPENSTACK_FLAVOR_ID_ARGS)) ? properties.get(Constants.OPENSTACK_FLAVOR_ID_ARGS) : settings.getOpenstackFlavor();
    }

    public static String getImageIdOrName(Map<String, String> properties, PluginSettings settings) {
        return StringUtils.isNotBlank(properties.get(Constants.OPENSTACK_IMAGE_ID_ARGS)) ? properties.get(Constants.OPENSTACK_IMAGE_ID_ARGS) : settings.getOpenstackImage();
    }

    public static String getEncodedUserData(CreateAgentRequest request, PluginSettings settings) {
        String userData = getUserData(request, settings);
        if (userData == null)
            return null;
        final byte[] userDataBytes = userData.getBytes(StandardCharsets.UTF_8);
        return Base64.encodeBase64String(userDataBytes);
    }

    public static String getUserData(CreateAgentRequest request, PluginSettings settings) {
        String requestUserData = request.properties().get(Constants.OPENSTACK_USERDATA_ARGS);
        if (StringUtils.isNotBlank(requestUserData))
            return requestUserData;

        return settings.getOpenstackUserdata();
    }

    public String getImageIdOrName() {
        return imageId;
    }

    public String getFlavorIdOrName() {
        return flavorId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("OpenStackInstance{");
        sb.append("id='").append(id).append('\'');
        sb.append(", createdAt=").append(createdAt);
        sb.append(", environment='").append(environment).append('\'');
        sb.append(", imageId='").append(imageId).append('\'');
        sb.append(", flavorId='").append(flavorId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
