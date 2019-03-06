/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.AgentInstances;
import cd.go.contrib.elasticagents.openstack.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.PluginRequest;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import cd.go.contrib.elasticagents.openstack.requests.JobCompletionRequest;
import org.junit.Test;

import java.util.Date;

import static org.mockito.Mockito.*;

public class JobCompletionRequestExecutorTest {

    @Test
    public void shouldNotTerminateAgentAfterInsufficientJobCompletes() throws Exception {
        String elasticAgentId = "agent-id";
        JobCompletionRequest request = new JobCompletionRequest(elasticAgentId, new JobIdentifier());
        AgentInstances agentInstances = mock(AgentInstances.class);
        PluginRequest pluginRequest = mock(PluginRequest.class);
        PluginSettings settings = mock(PluginSettings.class);
        String instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        OpenStackInstance opInstance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(pluginRequest.getPluginSettings()).thenReturn(settings);
        when(agentInstances.find(anyString())).thenReturn(opInstance);
        new JobCompletionRequestExecutor(request, agentInstances, pluginRequest).execute();

        verify(agentInstances, never()).terminate(elasticAgentId, settings);
    }

    @Test
    public void shouldTerminateAgentAfterEnoughJobCompletes() throws Exception {
        String elasticAgentId = "agent-id";
        JobCompletionRequest request = new JobCompletionRequest(elasticAgentId, new JobIdentifier());
        AgentInstances agentInstances = mock(AgentInstances.class);
        PluginRequest pluginRequest = mock(PluginRequest.class);
        PluginSettings settings = mock(PluginSettings.class);
        String instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        OpenStackInstance opInstance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        opInstance.setMaxCompletedJobs("2");
        when(pluginRequest.getPluginSettings()).thenReturn(settings);
        when(agentInstances.find(anyString())).thenReturn(opInstance);
        new JobCompletionRequestExecutor(request, agentInstances, pluginRequest).execute();
        new JobCompletionRequestExecutor(request, agentInstances, pluginRequest).execute();

        verify(agentInstances).terminate(elasticAgentId, settings);
    }
}