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

import cd.go.contrib.elasticagents.openstack.Agent;
import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.client.AgentInstances;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.executors.ShouldAssignWorkRequestExecutor;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ShouldAssignWorkRequest {
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @Expose
    @SerializedName("job_identifier")
    private JobIdentifier jobIdentifier;
    @Expose
    @SerializedName("agent")
    private Agent agent;
    @Expose
    @SerializedName("environment")
    private String environment;
    @Expose
    @SerializedName("elastic_agent_profile_properties")
    private Map<String, String> elasticAgentProfileProperties;
    @Expose
    @SerializedName("cluster_profile_properties")
    private ClusterProfileProperties clusterProfileProperties;

    public static ShouldAssignWorkRequest fromJSON(String json) {
        return GSON.fromJson(json, ShouldAssignWorkRequest.class);
    }

    public JobIdentifier jobIdentifier() {
        return jobIdentifier;
    }

    public Agent agent() {
        return agent;
    }

    public String environment() {
        return environment;
    }

    public Map<String, String> elasticAgentProfileProperties() {
        return elasticAgentProfileProperties;
    }

    public ClusterProfileProperties clusterProfileProperties() {
        return clusterProfileProperties;
    }

    public RequestExecutor executor(AgentInstances<OpenStackInstance> agentInstances) {
        return new ShouldAssignWorkRequestExecutor(this, agentInstances, clusterProfileProperties);
    }

    public String toJson() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setPrettyPrinting()
                .create();
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return "ShouldAssignWorkRequest{" +
                "jobIdentifier=" + jobIdentifier +
                ", agent=" + agent +
                ", environment='" + environment + '\'' +
                ", elasticAgentProfileProperties=" + elasticAgentProfileProperties +
                ", clusterProfileProperties=" + clusterProfileProperties +
                '}';
    }
}



