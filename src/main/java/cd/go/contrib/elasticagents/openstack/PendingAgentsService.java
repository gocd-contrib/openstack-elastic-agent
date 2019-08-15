package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.client.AgentInstances;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.utils.ServerHealthMessages;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.text.MessageFormat.format;

/**
 * Keeps a record of agent instances which have been requested on OpenStack
 * but not yet registered with GoCD server.
 */

public class PendingAgentsService {
    // FIXME: 2019-08-19 move this class to client namespace accessed via OpenStackInstances
    private static final Logger LOG = Logger.getLoggerFor(PendingAgentsService.class);
    private final ConcurrentHashMap<String, PendingAgent> pendingAgents = new ConcurrentHashMap<>();
    private boolean refreshRunning = false;
    private AgentInstances agentInstances;

    public PendingAgentsService(AgentInstances agentInstances) {
        LOG.debug("new PendingAgentsService, AgentInstances:[{}] ", agentInstances);
        this.agentInstances = agentInstances;
    }

    public void addPending(OpenStackInstance pendingInstance, CreateAgentRequest request) {
        pendingAgents.putIfAbsent(pendingInstance.id(), new PendingAgent(pendingInstance, request));
    }

    public PendingAgent[] getAgents() {
        Collection<PendingAgent> values = pendingAgents.values();
        return values.toArray(new PendingAgent[values.size()]);
    }

    public void refreshAll(PluginRequest pluginRequest, ClusterProfileProperties clusterProfileProperties) throws ServerRequestFailedException {
        if (refreshRunning) {
            LOG.info(format("[refresh-pending] Refresh skipped already running in other thread, total pending agent count = {0}", pendingAgents.size()));
            return;
        }
        refreshRunning = true;
        final long startTimeMillis = System.currentTimeMillis();
        Agents registeredAgents = pluginRequest.listAgents();
        for (Agent agent : registeredAgents.agents()) {
            PendingAgent removed = pendingAgents.remove(agent.elasticAgentId());
            if (removed != null)
                LOG.info(format("[refresh-pending] Agent {0} is registered with GoCD server and is no longer pending", removed));
        }
        for (Iterator<Map.Entry<String, PendingAgent>> iter = pendingAgents.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, PendingAgent> entry = iter.next();
            try {
                String instanceId = entry.getKey();
                if (!agentInstances.doesInstanceExist(instanceId)) {
                    LOG.warn(format("[refresh-pending] Pending agent {0} has disappeared from OpenStack", instanceId));
                    iter.remove();
                } else if (agentInstances.isInstanceInErrorState(instanceId)) {
                    LOG.error(format("[refresh-pending] Pending agent instance {0} is in ERROR state on OpenStack", instanceId));
                    iter.remove();
                    if (clusterProfileProperties.getOpenstackDeleteErrorInstances()) {
                        LOG.error(format("[refresh-pending] Deleting pending agent ERROR instance {0}", instanceId));
                        agentInstances.terminate(instanceId);
                    }
                } else if (agentInstances.hasPendingAgentTimedOut(instanceId)) {
                    final String message = format("Pending agent {0} has been pending for too long, terminating instance", instanceId);
                    LOG.warn("[refresh-pending] " + message);
                    pluginRequest.addServerHealthMessage("AgentTimedOut-" + instanceId, ServerHealthMessages.Type.WARNING, message);
                    iter.remove();
                    agentInstances.terminate(instanceId);
                } else {
                    LOG.debug(format("[refresh-pending] Pending agent {0} is still pending", instanceId));
                }
            } catch (Exception e) {
                LOG.error("Failed to check instance state", e);
            }
        }

        agentInstances.terminateUnregisteredInstances(pluginRequest.listAgents(), pendingAgents);
        LOG.info(format("[refresh-pending] Total pending agent count = {0}", pendingAgents.size()));
        final long durationInMillis = System.currentTimeMillis() - startTimeMillis;
        LOG.info(format("[refresh-pending] refreshing pending agents took {0} millis", durationInMillis));
        refreshRunning = false;
    }
}
