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

package cd.go.contrib.elasticagents.openstack.requests;

import cd.go.contrib.elasticagents.openstack.PendingAgentsService;
import cd.go.contrib.elasticagents.openstack.PluginRequest;
import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.client.AgentInstances;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.executors.CreateAgentRequestExecutor;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.Map;

/**
 * see https://plugin-api.gocd.io/current/elastic-agents/#create-agent
 */
public class CreateAgentRequest {
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private String autoRegisterKey;
    private Map<String, String> elasticAgentProfileProperties;
    @Expose
    private JobIdentifier jobIdentifier;
    private String environment;
    private ClusterProfileProperties clusterProfileProperties;

    public CreateAgentRequest() {

    }

    public CreateAgentRequest(String autoRegisterKey, Map<String, String> elasticAgentProfileProperties, JobIdentifier jobIdentifier,
                              String environment, Map<String, String> clusterProfileProperties) {
        this.autoRegisterKey = autoRegisterKey;
        this.jobIdentifier = jobIdentifier;
        this.elasticAgentProfileProperties = elasticAgentProfileProperties;
        this.environment = environment;
        this.clusterProfileProperties = ClusterProfileProperties.fromConfiguration(clusterProfileProperties);
    }

    public CreateAgentRequest(String autoRegisterKey, Map<String, String> elasticAgentProfileProperties, JobIdentifier jobIdentifier,
                              String environment, ClusterProfileProperties clusterProfileProperties) {
        this.autoRegisterKey = autoRegisterKey;
        this.elasticAgentProfileProperties = elasticAgentProfileProperties;
        this.environment = environment;
        this.jobIdentifier = jobIdentifier;
        this.clusterProfileProperties = clusterProfileProperties;
    }

    public static CreateAgentRequest fromJSON(String json) {
        return GSON.fromJson(json, CreateAgentRequest.class);
    }

    public String autoRegisterKey() {
        return autoRegisterKey;
    }

    public Map<String, String> properties() {
        return elasticAgentProfileProperties;
    }

    public ClusterProfileProperties clusterProfileProperties() {
        return clusterProfileProperties;
    }

    public String environment() {
        return environment;
    }

    public RequestExecutor executor(PendingAgentsService pendingAgents, AgentInstances<OpenStackInstance> agentInstances, PluginRequest pluginRequest) throws Exception {
        return new CreateAgentRequestExecutor(this, agentInstances, pluginRequest, pendingAgents);
    }

    @Override
    public String toString() {
        return "CreateAgentRequest{" +
                "autoRegisterKey='" + autoRegisterKey + '\'' +
                ", elasticAgentProfileProperties=" + elasticAgentProfileProperties +
                ", jobIdentifier=" + jobIdentifier +
                ", environment='" + environment + '\'' +
                ", clusterProfileProperties=" + clusterProfileProperties +
                '}';
    }

    public JobIdentifier job() {
        return jobIdentifier;
    }

    public boolean jobMatches(JobIdentifier otherJob) {
        return jobIdentifier.equals(otherJob);
    }
}
