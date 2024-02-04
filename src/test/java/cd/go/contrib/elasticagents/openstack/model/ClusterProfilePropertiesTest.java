/*
 * Copyright 2019 Thoughtworks, Inc.
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

package cd.go.contrib.elasticagents.openstack.model;

import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.requests.JobCompletionRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class ClusterProfilePropertiesTest {

    @Test
    public void shouldGenerateSameUUIDForClusterProfileProperties() {
        Map<String, String> clusterProfileConfigurations = Collections.singletonMap("go_server_url", "http://go-server-url/go");
        ClusterProfileProperties clusterProfileProperties = ClusterProfileProperties.fromConfiguration(clusterProfileConfigurations);

        assertThat(clusterProfileProperties.uuid(), is(clusterProfileProperties.uuid()));
    }

    @Test
    public void shouldGenerateSameUUIDForClusterProfilePropertiesAcrossRequests() {
        String createAgentRequestJSON = "{\n" +
                "  \"auto_register_key\": \"secret-key\",\n" +
                "  \"elastic_agent_profile_properties\": {\n" +
                "    \"key1\": \"value1\",\n" +
                "    \"key2\": \"value2\"\n" +
                "  },\n" +
                "  \"cluster_profile_properties\": {\n" +
                "    \"go_server_url\": \"https://foo.com/go\",\n" +
                "    \"docker_uri\": \"unix:///var/run/docker.sock\"\n" +
                "  },\n" +
                "  \"environment\": \"prod\"\n" +
                "}";

        CreateAgentRequest createAgentRequest = CreateAgentRequest.fromJSON(createAgentRequestJSON);

        String jobCompletionRequestJSON = "{\n" +
                "  \"elastic_agent_id\": \"ea1\",\n" +
                "  \"elastic_agent_profile_properties\": {\n" +
                "    \"Image\": \"alpine:latest\"\n" +
                "  },\n" +
                "  \"cluster_profile_properties\": {\n" +
                "    \"go_server_url\": \"https://foo.com/go\", \n" +
                "    \"docker_uri\": \"unix:///var/run/docker.sock\"\n" +
                "  },\n" +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  }\n" +
                "}";

        JobCompletionRequest jobCompletionRequest = JobCompletionRequest.fromJSON(jobCompletionRequestJSON);
        assertThat(jobCompletionRequest.getClusterProfileProperties().uuid(), is(createAgentRequest.clusterProfileProperties().uuid()));
    }

    @Test
    public void shouldBeValidGivenCreateAgentRequest() throws Exception {
        String createAgentRequestJSON = "{\n" +
                "  \"auto_register_key\": \"ee9c36d8-2cff-48c8-80f2-4b0ed2142df3\",\n" +
                "  \"elastic_agent_profile_properties\": {\n" +
                "    \"openstack_image_id\": \"RHEL7JeppesenDockerGoAgent\",\n" +
                "    \"openstack_keypair\": \"gocd-agents-keypair\",\n" +
                "    \"openstack_userdata\": \"\",\n" +
                "    \"openstack_max_instance_limit\": \"10\",\n" +
                "    \"openstack_flavor_id\": \"\",\n" +
                "    \"openstack_security_group\": \"rocket_security_group\",\n" +
                "    \"openstack_network_id\": \"\",\n" +
                "    \"agent_job_limit_max\": \"1\",\n" +
                "    \"openstack_min_instance_limit\": \"1\"\n" +
                "  },\n" +
                "  \"cluster_profile_properties\": {\n" +
                "    \"go_server_url\": \"https://10.64.14.114:8154/go\",\n" +
                "    \"auto_register_timeout\": \"30\",\n" +
                "    \"agent_ttl_max\": \"60\",\n" +
                "    \"default_min_instance_limit\": \"1\",\n" +
                "    \"default_max_instance_limit\": \"10\",\n" +
                "    \"openstack_endpoint\": \"https://gotosp.osp.jeppesensystems.com:13000/v3\",\n" +
                "    \"openstack_keystone_version\": \"3\",\n" +
                "    \"openstack_domain\": \"Default\",\n" +
                "    \"openstack_tenant\": \"gocd\",\n" +
                "    \"openstack_user\": \"gocd\",\n" +
                "    \"openstack_password\": \"jeppesen_gocd\",\n" +
                "    \"openstack_vm_prefix\": \"devel-\",\n" +
                "    \"openstack_image\": \"RHEL7JeppesenDockerGoAgent\",\n" +
                "    \"openstack_image_cache_ttl\": \"60\",\n" +
                "    \"use_previous_openstack_image\": \"true\",\n" +
                "    \"openstack_flavor\": \"m1.small\",\n" +
                "    \"openstack_network\": \"1e72b7d0-23e0-4148-8d80-e996f3854ecd\",\n" +
                "    \"openstack_userdata\": \"\",\n" +
                "    \"ssl_verification_disabled\": \"false\",\n" +
                "    \"delete_error_instances\": \"true\",\n" +
                "    \"agent_pending_register_timeout\": \"10\"\n" +
                "  },\n" +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"elastic-agents\",\n" +
                "    \"pipeline_label\": \"2\",\n" +
                "    \"pipeline_counter\": 2,\n" +
                "    \"stage_name\": \"defaultStage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"defaultJob-runInstance-1\",\n" +
                "    \"job_id\": 26\n" +
                "  }\n" +
                "}";

        CreateAgentRequest createAgentRequest = CreateAgentRequest.fromJSON(createAgentRequestJSON);
        createAgentRequest.clusterProfileProperties().validate("test");
    }

    @Test
    public void shouldBeValidGivenJSON() throws Exception {
        String createAgentRequestJSON = "{\n" +
                "  \"go_server_url\": \"https://10.1.1.1:8154/go\",\n" +
                "  \"auto_register_timeout\": \"30\",\n" +
                "  \"agent_ttl_max\": \"60\",\n" +
                "  \"default_min_instance_limit\": \"1\",\n" +
                "  \"default_max_instance_limit\": \"10\",\n" +
                "  \"openstack_endpoint\": \"https://example.com:13000/v3\",\n" +
                "  \"openstack_keystone_version\": \"3\",\n" +
                "  \"openstack_domain\": \"Default\",\n" +
                "  \"openstack_tenant\": \"gocd\",\n" +
                "  \"openstack_user\": \"gocd\",\n" +
                "  \"openstack_password\": \"password\",\n" +
                "  \"openstack_vm_prefix\": \"devel-\",\n" +
                "  \"openstack_image\": \"RHEL7DockerGoAgent\",\n" +
                "  \"openstack_image_cache_ttl\": \"60\",\n" +
                "  \"use_previous_openstack_image\": \"true\",\n" +
                "  \"openstack_flavor\": \"m1.small\",\n" +
                "  \"openstack_network\": \"1e72b7d0-23e0-4148-8d80-e996f3854ecd\",\n" +
                "  \"openstack_userdata\": null,\n" +
                "  \"ssl_verification_disabled\": \"false\",\n" +
                "  \"delete_error_instances\": \"true\",\n" +
                "  \"agent_pending_register_timeout\": \"20\"\n" +
                "}";

        final ClusterProfileProperties properties = ClusterProfileProperties.fromJSON(createAgentRequestJSON);

        properties.validate("test");
    }

    @Test
    public void shouldNotBeValid() throws Exception {
        String createAgentRequestJSON = "{\n" +
                "  \"auto_register_key\": \"ee9c36d8-2cff-48c8-80f2-4b0ed2142df3\",\n" +
                "  \"elastic_agent_profile_properties\": {\n" +
                "    \"openstack_image_id\": \"RHEL7JeppesenDockerGoAgent\",\n" +
                "    \"openstack_keypair\": \"gocd-agents-keypair\",\n" +
                "    \"openstack_userdata\": \"\",\n" +
                "    \"openstack_max_instance_limit\": \"10\",\n" +
                "    \"openstack_flavor_id\": \"\",\n" +
                "    \"openstack_security_group\": \"rocket_security_group\",\n" +
                "    \"openstack_network_id\": \"\",\n" +
                "    \"agent_job_limit_max\": \"1\",\n" +
                "    \"openstack_min_instance_limit\": \"1\"\n" +
                "  },\n" +
                "  \"cluster_profile_properties\": {},\n" +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"elastic-agents\",\n" +
                "    \"pipeline_label\": \"2\",\n" +
                "    \"pipeline_counter\": 2,\n" +
                "    \"stage_name\": \"defaultStage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"defaultJob-runInstance-1\",\n" +
                "    \"job_id\": 26\n" +
                "  }\n" +
                "}";

        CreateAgentRequest createAgentRequest = CreateAgentRequest.fromJSON(createAgentRequestJSON);

        assertThrows(IllegalArgumentException.class, () -> createAgentRequest.clusterProfileProperties().validate("test"));
    }

    @Test
    public void shouldNotBeValidGivenJSON() throws Exception {
        String createAgentRequestJSON = "{goServerUrl='null', openstackEndpoint='null', openstackKeystoneVersion='null', openstackDomain='null', agentPendingRegisterTimeout='null', agentTTLMin='null', agentTTLMax='null', defaultMinInstanceLimit='null', defaultMaxInstanceLimit='null', openstackTenant='null', openstackUser='null', openstackPassword='null', openstackVmPrefix='null', openstackImage='null', openstackImageCacheTTL='null', usePreviousOpenstackImage=null, openstackFlavor='null', openstackNetwork='null', openstackUserdata='null', sslVerificationDisabled=null, deleteErrorInstances=null, agentRegisterPeriod=null, agentTTLMinPeriod=null}";

        final ClusterProfileProperties properties = ClusterProfileProperties.fromJSON(createAgentRequestJSON);
        try {
            properties.validate("test");
            fail("Should not get here");
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
