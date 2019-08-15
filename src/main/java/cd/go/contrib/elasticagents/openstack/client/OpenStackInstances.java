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

import cd.go.contrib.elasticagents.openstack.*;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.utils.ServerHealthMessages;
import cd.go.contrib.elasticagents.openstack.utils.Util;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.Period;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Server;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;

public class OpenStackInstances implements AgentInstances<OpenStackInstance> {

    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstances.class);

    private final String uuid;
    private final Map<String, OpenStackInstance> instances = new ConcurrentHashMap<>();
    private final OpenstackClientWrapper clientWrapper;
    private final PluginSettings pluginSettings;
    private boolean refreshed = false;
    private boolean refreshRunning = false;

    public OpenStackInstances(PluginSettings pluginSettings) {
        LOG.debug("new OpenStackInstances, PluginSettings:[{}] ", pluginSettings);
        this.pluginSettings = pluginSettings;
        this.clientWrapper = new OpenstackClientWrapper(pluginSettings);
        this.uuid = pluginSettings.uuid();
    }

    public OpenStackInstances(PluginSettings pluginSettings, OpenstackClientWrapper client) {
        this.pluginSettings = pluginSettings;
        clientWrapper = client;
        this.uuid = pluginSettings.uuid();
    }

    @Override
    public OpenStackInstance create(CreateAgentRequest request, String transactionId) throws Exception {
        LOG.info("[{}] [create Agent] Processing request for {}", transactionId, request.job().represent());
        String imageNameOrId = getImageIdOrName(request.properties());
        imageNameOrId = clientWrapper.getImageId(imageNameOrId, transactionId);
        String flavorNameOrId = getFlavorIdOrName(request.properties());
        flavorNameOrId = clientWrapper.getFlavorId(flavorNameOrId, transactionId);
        final String encodedUserData = getEncodedUserData(request.properties());

        OpenStackInstance op_instance = OpenStackInstance.create(generateInstanceName(), imageNameOrId, flavorNameOrId, encodedUserData, transactionId, pluginSettings, request, clientWrapper);
        op_instance.setMaxCompletedJobs(request.properties().get(Constants.AGENT_JOB_LIMIT_MAX));
        LOG.info("[create agent] properties: {}", request.properties());

        register(op_instance);
        return op_instance;
    }

    @Override
    public synchronized boolean terminate(String instanceId) {
        boolean terminated = false;
        OpenStackInstance opInstance = instances.get(instanceId);
        try {
            if (opInstance == null) {
                LOG.warn("[terminate] Requested to terminate an instance [{}] that does not exist in plugin state," +
                        " trying anyway.", instanceId);
            }
            LOG.info("[terminate] OpenStack instance [{}]", instanceId);
            ActionResponse response = clientWrapper.terminate(instanceId);
            if (response.isSuccess()) {
                terminated = true;
            } else {
                try {
                    final String message = clientWrapper.getServer(instanceId).getFault().getMessage();
                    LOG.warn("[terminate] Failed to terminate instance [{}] with message [{}], trying again.",
                            instanceId, message);
                    response = clientWrapper.terminate(instanceId);
                    if (response.isSuccess()) {
                        terminated = true;
                        LOG.info("[terminate] Succeeded to terminate instance [{}] second time.", instanceId);
                    } else {
                        terminated = false;
                        LOG.error("[terminate] Failed to terminate instance [{}] second time.", instanceId);
                    }
                } catch (InstanceNotFoundException e) {
                    terminated = false;
                    LOG.warn("[terminate] Failed to get instance [{}].", instanceId);
                }
            }
            instances.remove(instanceId);
        } catch (RuntimeException ex) {
            LOG.warn("[terminate] Exception when trying to terminate an instance {}, {}",
                    instanceId, ex.getLocalizedMessage());
        }
        return terminated;
    }

    @Override
    public synchronized void refreshAll(PluginRequest pluginRequest) {
        LOG.debug("[refreshAll]: [{}] uuid=[{}] clusterURL={}, refreshRunning=[{}] refreshed=[{}]",
                this, uuid, pluginSettings.getOpenstackEndpoint(), refreshRunning, refreshed);
        if (refreshRunning) {
            LOG.info("[refreshAll] Refresh skipped already running in other thread");
            return;
        }
        refreshRunning = true;
        final long startTimeMillis = System.currentTimeMillis();
        LOG.debug("[refreshAll]: [{}] uuid=[{}] clusterURL={}, startTimeMillis=[{}] refreshed=[{}], ",
                this, uuid, pluginSettings.getOpenstackEndpoint(), startTimeMillis, refreshed);
        if (!refreshed) {
            if (isEmpty(pluginSettings.toString())) {
                LOG.warn("[refreshAll]: OpenStack elastic agents plugin settings are empty");
                return;
            }
            try {
                Agents agents = pluginRequest.listAgents();
                List<Server> allInstances = clientWrapper.listServers(pluginSettings.getOpenstackVmPrefix());
                for (Server server : allInstances) {
                    if (agents.containsAgentWithId(server.getId())) {
                        LOG.debug("[refreshAll] add instance that is already registered id=[{}]", server.getId());
                        register(new OpenStackInstance(server.getId(), server.getCreated(),
                                server.getMetadata().get(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY),
                                server.getImageId(), server.getFlavorId(), pluginSettings));
                    } else {
                        LOG.debug("[refreshAll] [{}] uuid=[{}] clusterURL={}, terminate instance that is not registered agent id=[{}]",
                                this, uuid, pluginSettings.getOpenstackEndpoint(), server.getId());
                        clientWrapper.terminate(server.getId());
                    }
                }
                refreshed = true;
            } catch (Exception e) {
                refreshRunning = false;
                pluginRequest.addServerHealthMessage(uuid, ServerHealthMessages.Type.ERROR, e.getLocalizedMessage());
                LOG.debug("[refreshAll]: [{}] uuid=[{}] clusterURL={}, startTimeMillis=[{}] refreshed=[{}], ",
                        this, uuid, pluginSettings.getOpenstackEndpoint(), startTimeMillis, refreshed);
                return;
            }
        }
        final long durationInMillis = System.currentTimeMillis() - startTimeMillis;
        LOG.info("[refreshAll] [{}] uuid=[{}] clusterURL={}, refreshing instances took {} millis",
                this, uuid, pluginSettings.getOpenstackEndpoint(), durationInMillis);
        refreshRunning = false;
    }

    @Override
    public void terminateUnregisteredInstances(Agents knownAgents, Map<String, PendingAgent> pendingAgents) {
        Period period = pluginSettings.getAgentTTLMinPeriod();
        List<Server> allInstances = clientWrapper.listServers(pluginSettings.getOpenstackVmPrefix());
        String allInstancesAsString = allInstances.stream()
                .map(n -> n.getName())
                .collect(Collectors.joining(","));
        final long startTimeMillis = System.currentTimeMillis();
        LOG.debug("[terminateUnregisteredInstances]: [{}] uuid=[{}] clusterURL={}, startTimeMillis=[{}] allInstances.size=[{}] [{}], ",
                this, uuid, pluginSettings.getOpenstackEndpoint(), startTimeMillis, allInstances.size(), allInstancesAsString);

        for (Server server : allInstances) {
            final String instanceId = server.getId();
            if (knownAgents.containsAgentWithId(instanceId)) {
                LOG.debug("[terminateUnregisteredInstances]: [{}] uuid=[{}] keeping known agent instance=[{}]",
                        this, uuid, server.getName());
            } else if (pendingAgents.containsKey(instanceId)) {
                LOG.debug("[terminateUnregisteredInstances]: [{}] uuid=[{}] keeping pending agent instance=[{}]",
                        this, uuid, server.getName());
            } else {
                LOG.debug("[terminateUnregisteredInstances]: [{}] uuid=[{}] terminating agent instance=[{}], since instance not registered nor pending.", this, uuid, server.getName());
                final boolean terminated = terminate(instanceId);
                if (!terminated) {
                    LOG.warn("[terminateUnregisteredInstances]: [{}] uuid=[{}] Exception when terminating agent instance=[{}].",
                            this, uuid, server.getName());
                }
            }
        }
    }

    public boolean doesInstanceExist(String id) {
        try {
            return clientWrapper.getServer(id) != null;
        } catch (InstanceNotFoundException e) {
            return false;
        }
    }

    public boolean matchInstance(String id, Map<String, String> properties, String requestEnvironment, String transactionId, boolean usePreviousImageId) {
        LOG.debug("[{}] [matchInstance] Instance: {}", transactionId, id);
        OpenStackInstance instance = this.find(id);
        if (instance == null) {
            LOG.info("[{}] [matchInstance] Instance {} NOT found in OpenStack cluster: {}",
                    transactionId, id, pluginSettings.getOpenstackEndpoint());
            return false;
        }
        LOG.info("[{}] [matchInstance] Found instance: {}", transactionId, instance);

        requestEnvironment = stripToEmpty(requestEnvironment);
        final String agentEnvironment = stripToEmpty(instance.environment());
        if (!requestEnvironment.equalsIgnoreCase(agentEnvironment)) {
            LOG.debug("[{}] [matchInstance] Request environment [{}] did NOT match agent's environment: [{}]", transactionId, requestEnvironment,
                    agentEnvironment);
            return false;
        }
        LOG.debug("[{}] [matchInstance] Request environment [{}] did match agent's environment: [{}]", transactionId, requestEnvironment,
                agentEnvironment);

        String proposedImageIdOrName = getImageIdOrName(properties);

        LOG.debug("[{}] [matchInstance] Trying to match image name/id: [{}] with instance image: [{}]", transactionId,
                proposedImageIdOrName, instance.getImageIdOrName());
        if (!proposedImageIdOrName.equals(instance.getImageIdOrName())) {
            LOG.debug("[{}] [matchInstance] image name/id: [{}] did NOT match with instance image: [{}]", transactionId,
                    proposedImageIdOrName, instance.getImageIdOrName());
            String proposedImageId = null;
            try {
                proposedImageId = clientWrapper.getImageId(proposedImageIdOrName, transactionId);
            } catch (ImageNotFoundException e) {

                return false;
            }
            LOG.debug("[{}] [matchInstance] Trying to match image id: [{}] with instance image: [{}]", transactionId,
                    proposedImageId, instance.getImageIdOrName());
            if (!proposedImageId.equals(instance.getImageIdOrName())) {
                LOG.debug("[{}] [matchInstance] image id: [{}] did NOT match with instance image: [{}]", transactionId,
                        proposedImageId, instance.getImageIdOrName());
                if (usePreviousImageId) {
                    proposedImageId = stripToEmpty(clientWrapper.getPreviousImageId(proposedImageIdOrName, transactionId));
                    LOG.debug("[{}] [matchInstance] Trying to match previous image id: [{}] with instance image: [{}]", transactionId,
                            proposedImageId, instance.getImageIdOrName());
                    if (!proposedImageId.equals(instance.getImageIdOrName())) {
                        LOG.debug("[{}] [matchInstance] previous image id: [{}] did NOT match with instance image: [{}]", transactionId,
                                proposedImageId, instance.getImageIdOrName());
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        String proposedFlavorIdOrName = getFlavorIdOrName(properties);
        LOG.debug("[{}] [matchInstance] Trying to match flavor name: [{}] with instance flavor: [{}]", transactionId,
                proposedFlavorIdOrName, instance.getFlavorIdOrName());
        if (!proposedFlavorIdOrName.equals(instance.getFlavorIdOrName())) {
            LOG.debug("[{}] [matchInstance] flavor name: [{}] did NOT match with instance flavor: [{}]", transactionId,
                    proposedFlavorIdOrName, instance.getFlavorIdOrName());
            proposedFlavorIdOrName = clientWrapper.getFlavorId(proposedFlavorIdOrName, transactionId);
            LOG.debug("[{}] [matchInstance] Trying to match flavor name: [{}] with instance flavor: [{}]", transactionId,
                    proposedFlavorIdOrName, instance.getFlavorIdOrName());
            if (!proposedFlavorIdOrName.equals(instance.getFlavorIdOrName())) {
                LOG.debug("[{}] [matchInstance] flavor name: [{}] did NOT match with instance flavor: [{}]", transactionId,
                        proposedFlavorIdOrName, instance.getFlavorIdOrName());
                return false;
            }
        }

        LOG.info("[{}] [matchInstance] Found matching instance: {}", transactionId, instance);
        return true;
    }

    void register(OpenStackInstance instance) {
        instances.put(instance.id(), instance);
    }

    @Override
    public Agents fetchExpiredAgents(Agents agents) {
        LOG.debug("[instancesCreatedAfterTTL] agentTTLMin: [{}] agentTTLMax: [{}] agents.agents().size(): [{}]",
                pluginSettings.getAgentTTLMinPeriod().getMinutes(), pluginSettings.getAgentTTLMax(), agents.agents().size());
        List<Agent> oldAgents = new ArrayList<>();
        for (Agent agent : agents.agents()) {

            OpenStackInstance instance = instances.get(agent.elasticAgentId());
            if (instance == null) {
                continue;
            }

            LOG.debug("[instancesCreatedAfterTTL] agentTTLMin: [{}] agentTTLMax: [{}]",
                    pluginSettings.getAgentTTLMinPeriod().getMinutes(), pluginSettings.getAgentTTLMax());
            int minutesTTL = Util.calculateTTL(pluginSettings.getAgentTTLMinPeriod().getMinutes(), pluginSettings.getAgentTTLMax());
            Date expireDate = DateUtils.addMinutes(instance.createAt().toDate(), minutesTTL);
            LOG.debug("[instancesCreatedAfterTTL] Agent: [{}] with minutesTTL: [{}]", agent.elasticAgentId(), minutesTTL);
            if (expireDate.before(new Date())) {
                LOG.info("[instancesCreatedAfterTTL] Agent: [{}] to be terminated with minutesTTL: [{}]", agent.elasticAgentId(), minutesTTL);
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
    public boolean hasInstance(String elasticAgentId) {
        return find(elasticAgentId) != null;
    }

    @Override
    public void performCleanup(PluginRequest pluginRequest) {
        final long startTimeMillis = System.currentTimeMillis();
        LOG.debug("[performCleanup] clusterURL={}, startTimeMillis=[{}] ",
                pluginSettings.getOpenstackEndpoint(), startTimeMillis);
        Agents allAgents = null;
        try {

            allAgents = pluginRequest.listAgents();
            Agents expiredAgents = fetchExpiredAgents(allAgents);
            Collection<Agent> agentsToDisable = expiredAgents.findAgentsToDisable();
            LOG.debug("[performCleanup] agentsToDisable={}", agentsToDisable);
            pluginRequest.disableAgents(agentsToDisable);

            allAgents = pluginRequest.listAgents();
            Collection<Agent> toBeDeleted = allAgents.findAgentsToTerminate();
            LOG.debug("[performCleanup] toBeDeleted={}", toBeDeleted);

            for (Agent agent : toBeDeleted) {
                terminate(agent.elasticAgentId());
            }
            pluginRequest.deleteAgents(toBeDeleted);


            // moved to cd.go.contrib.elasticagents.openstack.PendingAgentsService
//            allAgents = pluginRequest.listAgents();
//            terminateUnregisteredInstances(allAgents, pendingAgents);
            final long durationInMillis = System.currentTimeMillis() - startTimeMillis;
            LOG.info("[performCleanup] clusterURL={}, refreshing instances took {} millis",
                    pluginSettings.getOpenstackEndpoint(), durationInMillis);
        } catch (ServerRequestFailedException e) {
            LOG.error("[performCleanup] Exception [{}]", e);
        }
    }

    @Override
    public boolean isInstanceInErrorState(String id) throws Exception {
        return clientWrapper.isInstanceInErrorState(id);
    }

    @Override
    public boolean hasPendingAgentTimedOut(String id) {
        OpenStackInstance instance = instances.get(id);
        if (instance == null) {
            return false;
        }

        final int timeoutInMinutes = pluginSettings.getAgentPendingRegisterPeriod().getMinutes();
        final Date createDate = instance.createAt().toDate();
        Date timeoutDate = DateUtils.addMinutes(createDate, timeoutInMinutes);
        LOG.info("[hasAgentRegisterTimedOut] Agent: [{}] was created {} and will time out {}, with timeoutInMinutes: [{}]",
                id, createDate, timeoutDate, timeoutInMinutes);
        if (timeoutDate.before(new Date())) {
            LOG.info("[hasAgentRegisterTimedOut] Agent: [{}] has timed out, with timeoutInMinutes: [{}]", id, timeoutInMinutes);
            return true;
        }
        return false;
    }

    @Override
    public String getImageId(Map<String, String> properties, String transactionId) throws ImageNotFoundException {
        return clientWrapper.getImageId(getImageIdOrName(properties), transactionId);
    }

    @Override
    public String getFlavorId(Map<String, String> properties, String transactionId) {
        return clientWrapper.getFlavorId(getFlavorIdOrName(properties), transactionId);
    }

    private String generateInstanceName() {
        String instanceName = pluginSettings.getOpenstackVmPrefix() + RandomStringUtils.randomAlphanumeric(12).toLowerCase();
        while (clientWrapper.instanceNameExists(instanceName)) {
            instanceName = pluginSettings.getOpenstackVmPrefix() + RandomStringUtils.randomAlphanumeric(12).toLowerCase();
        }
        return instanceName;
    }

    public String getImageIdOrName(Map<String, String> properties) {
        LOG.debug("getImageIdOrName properties={}, settings={}", properties, pluginSettings);
        return StringUtils.isNotBlank(properties.get(Constants.OPENSTACK_IMAGE_ID_ARGS)) ? properties.get(Constants.OPENSTACK_IMAGE_ID_ARGS) : pluginSettings.getOpenstackImage();
    }

    public String getFlavorIdOrName(Map<String, String> properties) {
        return StringUtils.isNotBlank(properties.get(Constants.OPENSTACK_FLAVOR_ID_ARGS)) ? properties.get(Constants.OPENSTACK_FLAVOR_ID_ARGS) : pluginSettings.getOpenstackFlavor();
    }

    public String getEncodedUserData(Map<String, String> properties) {
        String userData = getUserData(properties);
        if (userData == null)
            return null;
        final byte[] userDataBytes = userData.getBytes(StandardCharsets.UTF_8);
        return Base64.encodeBase64String(userDataBytes);
    }

    public String getUserData(Map<String, String> properties) {
        String requestUserData = properties.get(Constants.OPENSTACK_USERDATA_ARGS);
        if (StringUtils.isNotBlank(requestUserData))
            return requestUserData;

        return pluginSettings.getOpenstackUserdata();
    }
}
