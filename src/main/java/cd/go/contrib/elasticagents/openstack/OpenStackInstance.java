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
import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;

import java.io.IOException;
import java.nio.channels.InterruptedByTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static cd.go.contrib.elasticagents.openstack.Constants.*;

public class OpenStackInstance {

    private static final Gson GSON = new Gson();
    private String id;
    private final DateTime createdAt;
    private final String environment;
    private final HashMap<String, String> properties;

    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstance.class);


    public String environment() { return environment; }

    public OpenStackInstance(String id, Date createdAt, String environment, HashMap<String, String> properties ) {
        this.id = id;
        this.createdAt = new DateTime(createdAt);
        this.environment = environment;
        this.properties = properties;
    }

    public String id() {
        return id;
    }

    public DateTime createAt() { return createdAt; }

    public HashMap<String, String> properties() { return properties; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpenStackInstance that = (OpenStackInstance) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }


    public static HashMap<String, String> populateInstanceProperties(OSClient os_client, String instanceId) throws InterruptedException, OS4JException {
        Server server = os_client.compute().servers().get(instanceId);
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, server.getImageId());
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, server.getFlavorId());
        return properties;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }


    public static OpenStackInstance find(OSClient os_client, String instanceId) throws InterruptedException, OS4JException {
        Server server = os_client.compute().servers().get(instanceId);
        return server.getId() != null ? new OpenStackInstance(server.getId(),
                server.getCreated(),
                server.getMetadata().get(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY),
                populateInstanceProperties(os_client, server.getId())) : null;
    }

    public void terminate(OSClient os_client) throws InterruptedException, OS4JException {
        os_client.compute().servers().delete(id);
    }


    public static OpenStackInstance create(CreateAgentRequest request, PluginSettings settings, OSClient osclient) throws InterruptedException, OS4JException, IOException {

        String instance_name;

        final byte[] authBytes = (!request.properties().get("openstack_userdata").isEmpty() ? request.properties().get("openstack_userdata") : settings.getOpenstackUserdata() ).getBytes(StandardCharsets.UTF_8);
        final String encoded = Base64.encodeBase64String(authBytes);

        HashMap<String, String> mdata = new HashMap<>();

//      metadata size is 256 characters, can't make json string.
//        mdata.put(CONFIGURATION_LABEL_KEY, GSON.toJson(request.properties()));

        Iterator entries = request.properties().entrySet().iterator();
        while(entries.hasNext()){
            Map.Entry entry = (Map.Entry) entries.next();
            mdata.put((String)entry.getKey(),(String)entry.getValue());
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

        if ( request.properties().containsKey(Constants.BOOTSTRAPPER_ARGS)) {
            mdata.put(Constants.GOAGENT_PROPERTIES_PREFIX + Constants.BOOTSTRAPPER_KEY, request.properties().get(Constants.BOOTSTRAPPER_ARGS));
        }
        mdata.put(Constants.GOAGENT_PROPERTIES_PREFIX + Constants.GO_SERVER_URL_KEY, request.properties().containsValue(Constants.GO_SERVER_URL_ARGS) ? request.properties().get(Constants.GO_SERVER_URL_ARGS) : settings.getGoServerUrl() );


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

        ServerCreate sc = Builders.server()
                .image(StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_IMAGE_ID_ARGS)) ? request.properties().get(Constants.OPENSTACK_IMAGE_ID_ARGS) : settings.getOpenstackImage() )
                .name(instance_name)
                .flavor(StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_FLAVOR_ID_ARGS)) ? request.properties().get(Constants.OPENSTACK_FLAVOR_ID_ARGS) : settings.getOpenstackFlavor() )
                .networks(Arrays.asList(StringUtils.isNotBlank(request.properties().get(Constants.OPENSTACK_NETWORK_ID_ARGS)) ? request.properties().get(Constants.OPENSTACK_NETWORK_ID_ARGS) : settings.getOpenstackNetwork()))
                .addMetadata(mdata)
                .userData(encoded)
                .build();
        Server server = osclient.compute().servers().boot(sc);

        // create instance properties ( image id, network id, etc... ) and pass to OpenstackInstance()

        return new OpenStackInstance(server.getId(), server.getCreated(), request.environment(), populateInstanceProperties(osclient, server.getId()));

    }

}
