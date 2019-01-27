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
import cd.go.contrib.elasticagents.openstack.utils.Util;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.Period;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;

public class OpenStackInstances implements AgentInstances<OpenStackInstance> {

    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstances.class);

    private final Map<String, OpenStackInstance> instances = new ConcurrentHashMap<>();
    private boolean refreshed;

    @Override
    public OpenStackInstance create(CreateAgentRequest request, PluginSettings settings, String transactionId) throws Exception {
        OpenStackInstance op_instance = OpenStackInstance.create(request, settings, os_client(settings), transactionId);
        op_instance.setMaxCompletedJobs(request.properties().get(Constants.AGENT_JOB_LIMIT_MAX));
        LOG.info(format("[create agent] properties: .", request.properties()));

        register(op_instance);
        return op_instance;
    }

    @Override
    public void refresh(String instanceId, PluginSettings settings) throws Exception {
        if (!instances.containsKey(instanceId)) {
            register(OpenStackInstance.find(os_client(settings), instanceId));
        }
    }

    @Deprecated
    private OSClient os_client(PluginSettings settings) throws Exception {
        return new OpenstackClientWrapper(settings).getClient();
    }


    @Override
    public void terminate(String instanceId, PluginSettings settings) throws Exception {
        OpenStackInstance opInstance = instances.get(instanceId);
        if (opInstance != null) {
            LOG.info(format("[terminate] OpenStack instance [{0}]", instanceId));
            opInstance.terminate(os_client(settings));
        } else {
            OpenStackPlugin.LOG.warn("Requested to terminate an instance that does not exist " + instanceId);
        }

        instances.remove(instanceId);
    }

    @Override
    public void refreshAll(PluginRequest pluginRequest) throws Exception {
        if (!refreshed) {
            PluginSettings pluginSettings = pluginRequest.getPluginSettings();
            if (pluginSettings == null) {
                LOG.warn("OpenStack elastic agents plugin settings are empty");
                return;
            }
            Agents agents = pluginRequest.listAgents();
            Map<String, String> op_instance_prefix = new HashMap<>();
            op_instance_prefix.put("name", pluginSettings.getOpenstackVmPrefix());
            List<Server> allInstances = (List<Server>) os_client(pluginSettings).compute().servers().list(op_instance_prefix);
            for (Server server : allInstances) {
                if (agents.containsAgentWithId(server.getId())) {
                    register(new OpenStackInstance(server.getId(),
                            server.getCreated(),
                            server.getMetadata().get(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY),
                            os_client(pluginSettings)));
                } else {
                    os_client(pluginSettings).compute().servers().delete(server.getId());
                }
            }
            refreshed = true;
        }
    }

    @Override
    public void terminateUnregisteredInstances(PluginSettings settings, Agents agents) throws Exception {
        OpenStackInstances toTerminate = unregisteredAfterTimeout(settings, agents);

        if (toTerminate.instances.isEmpty()) {
            return;
        }

        for (OpenStackInstance opInstance : toTerminate.instances.values()) {
            terminate(opInstance.id(), settings);
        }
    }

    public boolean doesInstanceExist(PluginSettings settings, String id) throws Exception {
        return os_client(settings).compute().servers().get(id) != null;
    }

    public boolean matchInstance(String id, Map<String, String> properties, String requestEnvironment, PluginSettings pluginSettings, OpenstackClientWrapper
            client, String transactionId, boolean usePreviousImageId) {
        LOG.debug(format("[{0}] [matchInstance] Instance: {1}", transactionId, id));
        OpenStackInstance instance = this.find(id);
        if (instance == null) {
            LOG.warn(format("[{0}] [matchInstance] Instance NOT found in Openstack: {1}", transactionId, id));
            return false;
        }
        LOG.info(format("[{0}] [matchInstance] Found instance: {1}", transactionId, instance));

        requestEnvironment = stripToEmpty(requestEnvironment);
        final String agentEnvironment = stripToEmpty(instance.environment());
        if (!requestEnvironment.equalsIgnoreCase(agentEnvironment)) {
            LOG.debug(format("[{0}] [matchInstance] Request environment [{1}] did NOT match agent's environment: [{2}]", transactionId, requestEnvironment,
                    agentEnvironment));
            return false;
        }
        LOG.debug(format("[{0}] [matchInstance] Request environment [{1}] did match agent's environment: [{2}]", transactionId, requestEnvironment,
                agentEnvironment));

        String proposedImageIdOrName = OpenStackInstance.getImageIdOrName(properties, pluginSettings);

        LOG.debug(format("[{0}] [matchInstance] Trying to match image name/id: [{1}] with instance image: [{2}]", transactionId,
                proposedImageIdOrName, instance.getImageIdOrName()));
        if (!proposedImageIdOrName.equals(instance.getImageIdOrName())) {
            LOG.debug(format("[{0}] [matchInstance] image name/id: [{1}] did NOT match with instance image: [{2}]", transactionId,
                    proposedImageIdOrName, instance.getImageIdOrName()));
            String proposedImageId = null;
            try {
                proposedImageId = client.getImageId(proposedImageIdOrName, transactionId);
            } catch (ImageNotFoundException e) {

                return false;
            }
            LOG.debug(format("[{0}] [matchInstance] Trying to match image id: [{1}] with instance image: [{2}]", transactionId,
                    proposedImageId, instance.getImageIdOrName()));
            if (!proposedImageId.equals(instance.getImageIdOrName())) {
                LOG.debug(format("[{0}] [matchInstance] image id: [{1}] did NOT match with instance image: [{2}]", transactionId,
                        proposedImageId, instance.getImageIdOrName()));
                if (usePreviousImageId) {
                    proposedImageId = stripToEmpty(client.getPreviousImageId(proposedImageIdOrName, transactionId));
                    LOG.debug(format("[{0}] [matchInstance] Trying to match previous image id: [{1}] with instance image: [{2}]", transactionId,
                            proposedImageId, instance.getImageIdOrName()));
                    if (!proposedImageId.equals(instance.getImageIdOrName())) {
                        LOG.debug(format("[{0}] [matchInstance] previous image id: [{1}] did NOT match with instance image: [{2}]", transactionId,
                                proposedImageId, instance.getImageIdOrName()));
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        String proposedFlavorIdOrName = OpenStackInstance.getFlavorIdOrName(properties, pluginSettings);
        LOG.debug(format("[{0}] [matchInstance] Trying to match flavor name: [{1}] with instance flavor: [{2}]", transactionId,
                proposedFlavorIdOrName, instance.getFlavorIdOrName()));
        if (!proposedFlavorIdOrName.equals(instance.getFlavorIdOrName())) {
            LOG.debug(format("[{0}] [matchInstance] flavor name: [{1}] did NOT match with instance flavor: [{2}]", transactionId,
                    proposedFlavorIdOrName, instance.getFlavorIdOrName()));
            proposedFlavorIdOrName = client.getFlavorId(proposedFlavorIdOrName, transactionId);
            LOG.debug(format("[{0}] [matchInstance] Trying to match flavor name: [{1}] with instance flavor: [{2}]", transactionId,
                    proposedFlavorIdOrName, instance.getFlavorIdOrName()));
            if (!proposedFlavorIdOrName.equals(instance.getFlavorIdOrName())) {
                LOG.debug(format("[{0}] [matchInstance] flavor name: [{1}] did NOT match with instance flavor: [{2}]", transactionId,
                        proposedFlavorIdOrName, instance.getFlavorIdOrName()));
                return false;
            }
        }

        LOG.info(format("[{0}] [matchInstance] Found matching instance: {1}", transactionId, instance));
        return true;
    }

    void register(OpenStackInstance op_instance) {
        instances.put(op_instance.id(), op_instance);
    }

    private OpenStackInstances unregisteredAfterTimeout(PluginSettings settings, Agents knownAgents) throws Exception {

        String agentID;
        Map<String, String> op_instance_prefix = new HashMap<>();
        op_instance_prefix.put("name", settings.getOpenstackVmPrefix());

        Period period = settings.getAutoRegisterPeriod();
        OpenStackInstances unregisteredInstances = new OpenStackInstances();
        OpenstackClientWrapper client = new OpenstackClientWrapper(settings);
        List<Server> allInstances = (List<Server>) client.getClient().compute().servers().list(op_instance_prefix);

        for (Server server : allInstances) {
            if (knownAgents.containsAgentWithId(server.getId())) {
                continue;
            }
            if(!doesInstanceExist(settings, server.getId()))
                continue;
            if (DateUtils.addMinutes(server.getCreated(), period.getMinutes()).before(new Date())) {
                unregisteredInstances.register(new OpenStackInstance(server.getId(),
                        server.getCreated(),
                        server.getMetadata().get(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY),
                        client.getClient()));
            }
        }
        return unregisteredInstances;
    }

    @Override
    public Agents instancesCreatedAfterTimeout(PluginSettings settings, Agents agents) {
        ArrayList<Agent> oldAgents = new ArrayList<>();
        for (Agent agent : agents.agents()) {

            OpenStackInstance instance = instances.get(agent.elasticAgentId());
            if (instance == null) {
                continue;
            }

            LOG.debug(format("[instancesCreatedAfterTimeout] agentTTLMin: [{0}] agentTTLMax: [{1}]", settings.getAutoRegisterPeriod().getMinutes(), settings.getAgentTTLMax()));
            int minutesTTL = Util.calculateTTL(settings.getAutoRegisterPeriod().getMinutes(), settings.getAgentTTLMax());
            Date expireDate = DateUtils.addMinutes(instance.createAt().toDate(), minutesTTL);
            LOG.debug(format("[instancesCreatedAfterTimeout] Agent: [{0}] with minutesTTL: [{1}]", agent.elasticAgentId(), minutesTTL));
            if (expireDate.before(new Date())) {
                LOG.info(format("[instancesCreatedAfterTimeout] Agent: [{0}] to be terminated with minutesTTL: [{1}]", agent.elasticAgentId(), minutesTTL));
                oldAgents.add(agent);
            }
        }
        return new Agents(oldAgents);
    }

    @Override
    public OpenStackInstance find(String agentId) {
        return instances.get(agentId);
    }

    @Override
    public boolean isInstanceInErrorState(PluginSettings settings, String id) throws Exception {
        Server instance = os_client(settings).compute().servers().get(id);
        return instance != null && instance.getStatus() != null && instance.getStatus().equals(Server.Status.ERROR);
    }

}
