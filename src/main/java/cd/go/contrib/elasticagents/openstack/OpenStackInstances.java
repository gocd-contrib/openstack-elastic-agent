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
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.Period;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cd.go.contrib.elasticagents.openstack.OpenStackPlugin.LOG;

public class OpenStackInstances implements AgentInstances<OpenStackInstance> {


    private final ConcurrentHashMap<String, OpenStackInstance> instances = new ConcurrentHashMap<>();
    private boolean refreshed;

    @Override
    public OpenStackInstance create(CreateAgentRequest request, PluginSettings settings) throws Exception {
        OpenStackInstance op_instance = OpenStackInstance.create(request, settings, os_client(settings));
        register(op_instance);
        return op_instance;
    }

    @Override
    public void refresh(String instanceId, PluginSettings settings) throws Exception {
        if (!instances.containsKey(instanceId)) {
            register(OpenStackInstance.find(os_client(settings), instanceId));
        }
    }


    @Override
    public void terminate(String instanceId, PluginSettings settings) throws Exception {
        OpenStackInstance opInstance = instances.get(instanceId);
        if (opInstance!= null) {
            opInstance.terminate(os_client(settings));
        } else {
            OpenStackPlugin.LOG.warn("Requested to terminate an instance that does not exist " + instanceId);
        }

        instances.remove(instanceId);
    }

    @Override
    public void refreshAll(PluginRequest pluginRequest) throws Exception{
        if (!refreshed) {
            String agentID;
            PluginSettings pluginSettings = pluginRequest.getPluginSettings();
            Agents agents = pluginRequest.listAgents();
            Map<String, String> op_instance_prefix = new HashMap<String, String>();
            op_instance_prefix.put("name",pluginSettings.getOpenstackVmPrefix());
            List<Server> allInstances = (List<Server>) os_client(pluginSettings).compute().servers().list(op_instance_prefix);
            for (Server server : allInstances) {
                if (agents.containsAgentWithId(server.getId())) {
                    register(new OpenStackInstance(server.getId(),
                            server.getCreated(),
                            server.getMetadata().get(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY),
                            OpenStackInstance.populateInstanceProperties(os_client(pluginSettings),server.getId())));
                }else{
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

    public boolean isInstanceAlive(PluginSettings settings, String id) throws Exception {
        return os_client(settings).compute().servers().get(id) == null ? false : true;
    }

    public boolean matchInstance(String id, Map<String, String> properties){
        OpenStackInstance instance = this.find(id);
        return ((instance.properties().get(Constants.OPENSTACK_IMAGE_ID_ARGS).equals(properties.get(Constants.OPENSTACK_IMAGE_ID_ARGS))) &&
                (instance.properties().get(Constants.OPENSTACK_FLAVOR_ID_ARGS).equals(properties.get(Constants.OPENSTACK_FLAVOR_ID_ARGS))));
    }

    private void register(OpenStackInstance op_instance) {
        instances.put(op_instance.id(), op_instance);
    }

    private OSClient os_client(PluginSettings settings) throws Exception {
        return OpenStackClientFactory.os_client(settings);
    }

    private OpenStackInstances unregisteredAfterTimeout(PluginSettings settings, Agents knownAgents) throws Exception {

        String agentID;
        Map<String, String> op_instance_prefix = new HashMap<String, String>();
        op_instance_prefix.put("name",settings.getOpenstackVmPrefix());

        Period period = settings.getAutoRegisterPeriod();
        OpenStackInstances unregisteredInstances = new OpenStackInstances();
        List<Server> allInstances = (List<Server>) os_client(settings).compute().servers().list(op_instance_prefix);

        for (Server server : allInstances) {
            if (knownAgents.containsAgentWithId(server.getId())) {
                continue;
            }
            if (DateUtils.addMinutes(server.getCreated(), period.getMinutes()).before(new Date())) {
                unregisteredInstances.register(new OpenStackInstance(server.getId(),
                        server.getCreated(),
                        server.getMetadata().get(Constants.GOSERVER_PROPERTIES_PREFIX + Constants.ENVIRONMENT_KEY),
                        OpenStackInstance.populateInstanceProperties(os_client(settings),server.getId())));
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

            if (DateUtils.addMinutes(instance.createAt().toDate(), settings.getAutoRegisterPeriod().getMinutes()).before(new Date())) {
                oldAgents.add(agent);
            }
        }
        return new Agents(oldAgents);
    }

    @Override
    public OpenStackInstance find(String agentId) {
        return instances.get(agentId);
    }

}
