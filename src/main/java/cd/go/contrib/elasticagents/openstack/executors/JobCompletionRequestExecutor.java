package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.Agent;
import cd.go.contrib.elasticagents.openstack.PluginRequest;
import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.client.AgentInstances;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.requests.JobCompletionRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Arrays;
import java.util.List;

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
        ClusterProfileProperties clusterProfileProperties = jobCompletionRequest.getClusterProfileProperties();
        final String elasticAgentId = jobCompletionRequest.getElasticAgentId();
        final OpenStackInstance instance = agentInstances.find(elasticAgentId);
        Agent agent = new Agent(elasticAgentId);
        LOG.info(format("[job-completed] instance {0} has {1}/{2} completed jobs.",
                elasticAgentId, instance.getJobsCompleted(), instance.getMaxCompletedJobs()));
        if (instance.incrementJobsCompleted()) {
            LOG.info(format("[job-completed] Will terminate instance {0} in cluster {1} as it has completed enough jobs.",
                    elasticAgentId, clusterProfileProperties));
            List<Agent> agents = Arrays.asList(agent);
            pluginRequest.disableAgents(agents);
            agentInstances.terminate(elasticAgentId);
            pluginRequest.deleteAgents(agents);
        } else {
            LOG.info(format("[job-completed] Will NOT terminate instance {0} when completed job {1}", elasticAgentId, jobCompletionRequest.jobIdentifier().getRepresentation()));
        }
        return DefaultGoPluginApiResponse.success("");
    }
}