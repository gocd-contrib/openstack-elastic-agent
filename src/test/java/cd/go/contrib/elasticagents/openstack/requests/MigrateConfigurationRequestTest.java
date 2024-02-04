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

package cd.go.contrib.elasticagents.openstack.requests;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.TestHelper;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfile;
import cd.go.contrib.elasticagents.openstack.model.ElasticAgentProfile;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class MigrateConfigurationRequestTest {

    @Test
    public void shouldCreateMigrationConfigRequestFromRequestBody() {
        String requestBody = "{" +
                "    \"plugin_settings\":{" +
                "        \"go_server_url\":\"https://127.0.0.1:8154/go\", " +
                "        \"auto_register_timeout\":\"20\"" +
                "    }," +
                "    \"cluster_profiles\":[" +
                "        {" +
                "            \"id\":\"cluster_profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"properties\":{" +
                "                \"go_server_url\":\"https://127.0.0.1:8154/go\", " +
                "                \"auto_register_timeout\":\"20\"" +
                "            }" +
                "         }" +
                "    ]," +
                "    \"elastic_agent_profiles\":[" +
                "        {" +
                "            \"id\":\"profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"cluster_profile_id\":\"cluster_profile_id\"," +
                "            \"properties\":{" +
                "                \"some_key\":\"some_value\"," +
                "                \"some_key2\":\"some_value2\"" +
                "            }" +
                "        }" +
                "    ]" +
                "}\n";

        MigrateConfigurationRequest request = MigrateConfigurationRequest.fromJSON(requestBody);

        PluginSettings pluginSettings = new PluginSettings();
//        pluginSettings.setGoServerUrl("https://127.0.0.1:8154/go");
//        pluginSettings.setAgentTTLMin("20");

        ClusterProfile clusterProfile = new ClusterProfile();
        clusterProfile.setId("cluster_profile_id");
        clusterProfile.setPluginId("plugin_id");
        clusterProfile.setClusterProfileProperties(pluginSettings);

        ElasticAgentProfile elasticAgentProfile = new ElasticAgentProfile();
        elasticAgentProfile.setId("profile_id");
        elasticAgentProfile.setPluginId("plugin_id");
        elasticAgentProfile.setClusterProfileId("cluster_profile_id");
        HashMap<String, String> properties = new HashMap<>();
        properties.put("some_key", "some_value");
        properties.put("some_key2", "some_value2");
        elasticAgentProfile.setProperties(properties);

        assertThat(pluginSettings, is(request.getPluginSettings()));
        assertThat(Arrays.asList(clusterProfile), is(request.getClusterProfiles()));
        assertThat(Arrays.asList(elasticAgentProfile), is(request.getElasticAgentProfiles()));
    }

    @Test
    public void shouldCreateMigrationConfigRequestWhenNoConfigurationsAreSpecified() {
        String requestBody = "{" +
                "    \"plugin_settings\":{}," +
                "    \"cluster_profiles\":[]," +
                "    \"elastic_agent_profiles\":[]" +
                "}\n";

        MigrateConfigurationRequest request = MigrateConfigurationRequest.fromJSON(requestBody);

        assertThat(new PluginSettings(), is(request.getPluginSettings()));
        assertThat(Arrays.asList(), is(request.getClusterProfiles()));
        assertThat(Arrays.asList(), is(request.getElasticAgentProfiles()));
    }

    @Test
    public void shouldSerializeToJSONFromMigrationConfigRequest() throws JSONException, IOException {
        PluginSettings pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.ID1);

        ClusterProfile clusterProfile = new ClusterProfile();
        clusterProfile.setId("cluster_profile_id");
        clusterProfile.setPluginId("plugin_id");
        clusterProfile.setClusterProfileProperties(pluginSettings);

        ElasticAgentProfile elasticAgentProfile = new ElasticAgentProfile();
        elasticAgentProfile.setId("profile_id");
        elasticAgentProfile.setPluginId("plugin_id");
        elasticAgentProfile.setClusterProfileId("cluster_profile_id");
        HashMap<String, String> properties = new HashMap<>();
        properties.put("some_key", "some_value");
        properties.put("some_key2", "some_value2");
        elasticAgentProfile.setProperties(properties);

        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(clusterProfile), Arrays.asList(elasticAgentProfile));

        String actual = request.toJSON();

        String expected = "{" +
                "    \"plugin_settings\":{" +
                "        \"go_server_url\":\"https://gocd.example.com:8154/go\", " +
                "        \"auto_register_timeout\":\"20\"" +
                "    }," +
                "    \"cluster_profiles\":[" +
                "        {" +
                "            \"id\":\"cluster_profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"properties\":{" +
                "                \"go_server_url\":\"https://gocd.example.com:8154/go\", " +
                "                \"auto_register_timeout\":\"20\"" +
                "            }" +
                "         }" +
                "    ]," +
                "    \"elastic_agent_profiles\":[" +
                "        {" +
                "            \"id\":\"profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"cluster_profile_id\":\"cluster_profile_id\"," +
                "            \"properties\":{" +
                "                \"some_key\":\"some_value\"," +
                "                \"some_key2\":\"some_value2\"" +
                "            }" +
                "        }" +
                "    ]" +
                "}\n";
        System.out.println(actual);
        JSONAssert.assertEquals(expected, actual, false);
    }
}
