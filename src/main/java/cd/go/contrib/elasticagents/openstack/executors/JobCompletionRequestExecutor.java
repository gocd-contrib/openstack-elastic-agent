package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.AgentInstances;
import cd.go.contrib.elasticagents.openstack.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.PluginRequest;
import cd.go.contrib.elasticagents.openstack.RequestExecutor;
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
        final OpenStackInstance instance = agentInstances.find(jobCompletionRequest.getElasticAgentId());
        LOG.info(format("[job-completed] instance {0} has {1}/{2} completed jobs.",
                jobCompletionRequest.getElasticAgentId(), instance.getJobsCompleted(), instance.getMaxCompletedJobs()));
        if (instance.incrementJobsCompleted()) {
            LOG.info(format("[job-completed] Will terminate instance {0} as it has completed enough jobs.", jobCompletionRequest.getElasticAgentId()));
            agentInstances.terminate(jobCompletionRequest.getElasticAgentId(), pluginRequest.getPluginSettings());
        } else {
            LOG.info(format("[job-completed] Will NOT terminate instance {0} when completed job {1}", jobCompletionRequest.getElasticAgentId(), jobCompletionRequest.jobIdentifier().getRepresentation()));
        }
        return new DefaultGoPluginApiResponse(200);
    }
}