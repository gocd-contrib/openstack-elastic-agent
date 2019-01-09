package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.*;
import cd.go.contrib.elasticagents.openstack.requests.JobCompletionRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import static java.text.MessageFormat.format;

public class JobCompletionRequestExecutor implements RequestExecutor {
    public static final Logger LOG = Logger.getLoggerFor(JobCompletionRequestExecutor.class);

    private final JobCompletionRequest jobCompletionRequest;
    private final AgentInstances<OpenStackInstance> agentInstances;
    private final PluginRequest pluginRequest;

    public JobCompletionRequestExecutor(JobCompletionRequest jobCompletionRequest, AgentInstances<OpenStackInstance> agentInstances, PluginRequest pluginRequest) {
        this.jobCompletionRequest = jobCompletionRequest;
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        // no-op for now
        LOG.info(format("[job-completed] Will NOT terminate instance {0} when completed job {1}", jobCompletionRequest.getElasticAgentId(), jobCompletionRequest.jobIdentifier().getRepresentation()));
        //agentInstances.terminate(jobCompletionRequest.getElasticAgentId(), pluginRequest.getPluginSettings());
        return new DefaultGoPluginApiResponse(200);
    }
}