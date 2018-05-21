package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;

import java.util.Map;

import static cd.go.contrib.elasticagents.openstack.OpenStackPlugin.LOG;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang.StringUtils.stripToEmpty;

public class PendingAgent {
    private final String pendingInstanceImageId;
    private final String pendingInstanceFlavorId;
    private OpenStackInstance pendingInstance;
    private CreateAgentRequest createRequest;

    public PendingAgent(OpenStackInstance pendingInstance, CreateAgentRequest request) {
        this.pendingInstance = pendingInstance;
        this.createRequest = request;
        this.pendingInstanceImageId = pendingInstance.getImageIdOrName();
        this.pendingInstanceFlavorId = pendingInstance.getFlavorIdOrName();
    }

    public AgentMatchResult match(String transactionId, String proposedImageIdOrName, String proposedFlavorIdOrName, String requestEnvironment, Map<String, Object> job) {
        String id = this.elasticAgentId();
        LOG.debug(format("[{0}] [matchPendingInstance] Instance: {1}", transactionId, id));

        final String agentEnvironment = stripToEmpty(createRequest.environment());
        requestEnvironment = stripToEmpty(requestEnvironment);
        if (!requestEnvironment.equalsIgnoreCase(agentEnvironment)) {
            LOG.debug(format("[{0}] [matchPendingInstance] Request environment [{1}] did NOT match agent's environment: [{2}]", transactionId, requestEnvironment,
                    agentEnvironment));
            return new AgentMatchResult(false, false);
        }
        LOG.debug(format("[{0}] [matchPendingInstance] Request environment [{1}] did match agent's environment: [{2}]", transactionId, requestEnvironment,
                agentEnvironment));

        LOG.debug(format("[{0}] [matchPendingInstance] Trying to match image name/id: [{1}] with instance image: [{2}]", transactionId,
                proposedImageIdOrName, pendingInstanceImageId));
        if (!proposedImageIdOrName.equals(pendingInstanceImageId)) {
            LOG.debug(format("[{0}] [matchPendingInstance] image name/id: [{1}] did NOT match with instance image: [{2}]", transactionId,
                    proposedImageIdOrName, pendingInstanceImageId));
            return new AgentMatchResult(false, false);
        }

        LOG.debug(format("[{0}] [matchPendingInstance] Trying to match flavor name: [{1}] with instance flavor: [{2}]", transactionId,
                proposedFlavorIdOrName, pendingInstanceFlavorId));
        if (!proposedFlavorIdOrName.equals(pendingInstanceFlavorId)) {
            LOG.debug(format("[{0}] [matchPendingInstance] flavor name: [{1}] did NOT match with instance flavor: [{2}]", transactionId,
                    proposedFlavorIdOrName, pendingInstanceFlavorId));
            return new AgentMatchResult(false, false);
        }

        boolean jobsMatch = createRequest.jobMatches(job);
        LOG.info(format("[{0}] [matchPendingInstance] Found matching instance: {1} ByProfile=true, ByJob={2}", transactionId, id, jobsMatch));
        return new AgentMatchResult(jobsMatch, true);
    }

    public String elasticAgentId() {
        return pendingInstance.id();
    }
}
