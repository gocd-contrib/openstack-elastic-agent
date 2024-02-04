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

package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.Constants;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.TestHelper;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfile;
import cd.go.contrib.elasticagents.openstack.model.ElasticAgentProfile;
import cd.go.contrib.elasticagents.openstack.requests.MigrateConfigurationRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class MigrateConfigurationRequestExecutorTest {

    private PluginSettings pluginSettings;
    private ClusterProfile clusterProfile;
    private ElasticAgentProfile elasticAgentProfile;
    private HashMap<String, String> properties;

    @BeforeEach
    public void setUp() throws Exception {
        pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.ID1);

        clusterProfile = new ClusterProfile();
        clusterProfile.setId("cluster_profile_id");
        clusterProfile.setPluginId(Constants.PLUGIN_ID);
        clusterProfile.setClusterProfileProperties(pluginSettings);

        elasticAgentProfile = new ElasticAgentProfile();
        elasticAgentProfile.setId("profile_id");
        elasticAgentProfile.setPluginId(Constants.PLUGIN_ID);
        elasticAgentProfile.setClusterProfileId("cluster_profile_id");
        properties = new HashMap<>();
        properties.put("some_key", "some_value");
        properties.put("some_key2", "some_value2");
        elasticAgentProfile.setProperties(properties);
    }

    @Test
    public void shouldNotMigrateConfigWhenNoPluginSettingsAreConfigured() throws Exception {
        MigrateConfigurationRequest request = new MigrateConfigurationRequest(new PluginSettings(), Arrays.asList(clusterProfile), Arrays.asList(elasticAgentProfile));
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(new PluginSettings()));
        assertThat(responseObject.getClusterProfiles(), is(Arrays.asList(clusterProfile)));
        assertThat(responseObject.getElasticAgentProfiles(), is(Arrays.asList(elasticAgentProfile)));
    }

    @Test
    public void shouldNotMigrateConfigWhenClusterProfileIsAlreadyConfigured() throws Exception {
        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(clusterProfile), Arrays.asList(elasticAgentProfile));
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(pluginSettings));
        assertThat(responseObject.getClusterProfiles(), is(Arrays.asList(clusterProfile)));
        assertThat(responseObject.getElasticAgentProfiles(), is(Arrays.asList(elasticAgentProfile)));
    }

    @Test
    public void shouldPopulateNoOpClusterProfileWithPluginSettingsConfigurations() throws Exception {
        ClusterProfile emptyClusterProfile = new ClusterProfile(String.format("no-op-cluster-for-%s", Constants.PLUGIN_ID), Constants.PLUGIN_ID, new PluginSettings());
        elasticAgentProfile.setClusterProfileId(emptyClusterProfile.getId());
        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(emptyClusterProfile), Arrays.asList(elasticAgentProfile));
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(pluginSettings));
        List<ClusterProfile> actual = responseObject.getClusterProfiles();
        ClusterProfile actualClusterProfile = actual.get(0);

        assertThat(actualClusterProfile.getId(), is(not(String.format("no-op-cluster-for-%s", Constants.PLUGIN_ID))));
        this.clusterProfile.setId(actualClusterProfile.getId());

        assertThat(actual, is(Arrays.asList(this.clusterProfile)));
        assertThat(responseObject.getElasticAgentProfiles(), is(Arrays.asList(elasticAgentProfile)));

        assertThat(elasticAgentProfile.getClusterProfileId(), is(actualClusterProfile.getId()));
    }

    @Test
    public void shouldPopulateNoOpClusterProfileWithPluginSettingsConfigurations_WithoutChangingClusterProfileIdIfItsNotNoOp() throws Exception {
        String clusterProfileId = "i-renamed-no-op-cluster-to-something-else";
        ClusterProfile emptyClusterProfile = new ClusterProfile(clusterProfileId, Constants.PLUGIN_ID, new PluginSettings());
        elasticAgentProfile.setClusterProfileId(emptyClusterProfile.getId());
        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(emptyClusterProfile), Arrays.asList(elasticAgentProfile));
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(pluginSettings));
        List<ClusterProfile> actual = responseObject.getClusterProfiles();
        ClusterProfile actualClusterProfile = actual.get(0);

        assertThat(actualClusterProfile.getId(), is(clusterProfileId));
        this.clusterProfile.setId(actualClusterProfile.getId());

        assertThat(actual, is(Arrays.asList(this.clusterProfile)));
        assertThat(responseObject.getElasticAgentProfiles(), is(Arrays.asList(elasticAgentProfile)));

        assertThat(elasticAgentProfile.getClusterProfileId(), is(clusterProfileId));
    }

    @Test
    public void shouldMigratePluginSettingsToClusterProfile_WhenNoElasticAgentProfilesAreConfigured() throws Exception {
        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Collections.emptyList(), Collections.emptyList());
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(pluginSettings));
        List<ClusterProfile> actual = responseObject.getClusterProfiles();
        ClusterProfile actualClusterProfile = actual.get(0);
        this.clusterProfile.setId(actualClusterProfile.getId());

        assertThat(actual, is(Arrays.asList(this.clusterProfile)));
        assertThat(responseObject.getElasticAgentProfiles(), is(Collections.emptyList()));
    }

    @Test
    public void ShouldMigrateEmptyClusterProfiles_WhenMultipleEmptyClusterProfilesExists() throws Exception {
        ClusterProfile emptyCluster1 = new ClusterProfile("cluster_profile_1", Constants.PLUGIN_ID, new PluginSettings());
        ClusterProfile emptyCluster2 = new ClusterProfile("cluster_profile_2", Constants.PLUGIN_ID, new PluginSettings());
        System.out.println(Constants.PLUGIN_ID);

        ElasticAgentProfile elasticAgentProfile1 = new ElasticAgentProfile();
        elasticAgentProfile1.setId("profile_id_1");
        elasticAgentProfile1.setPluginId(Constants.PLUGIN_ID);
        elasticAgentProfile1.setClusterProfileId(emptyCluster1.getId());

        ElasticAgentProfile elasticAgentProfile2 = new ElasticAgentProfile();
        elasticAgentProfile2.setId("profile_id_2");
        elasticAgentProfile2.setPluginId(Constants.PLUGIN_ID);
        elasticAgentProfile2.setClusterProfileId(emptyCluster2.getId());

        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(emptyCluster1, emptyCluster2), Arrays.asList(elasticAgentProfile1, elasticAgentProfile2));
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(pluginSettings));

        this.clusterProfile.setId(responseObject.getClusterProfiles().get(0).getId());
        assertThat(responseObject.getClusterProfiles().get(0), is(clusterProfile));

        this.clusterProfile.setId(responseObject.getClusterProfiles().get(1).getId());
        assertThat(responseObject.getClusterProfiles().get(1), is(clusterProfile));

        assertThat(responseObject.getElasticAgentProfiles().get(0).getClusterProfileId(), is(emptyCluster1.getId()));
        assertThat(responseObject.getElasticAgentProfiles().get(1).getClusterProfileId(), is(emptyCluster2.getId()));
    }

    @Test
    public void ShouldNotMigrateEmptyAndUnassociatedClusterProfiles() throws Exception {
        ClusterProfile emptyCluster1 = new ClusterProfile("cluster_profile_1", Constants.PLUGIN_ID, new PluginSettings());
        ClusterProfile emptyCluster2 = new ClusterProfile("cluster_profile_2", Constants.PLUGIN_ID, new PluginSettings());

        ElasticAgentProfile elasticAgentProfile1 = new ElasticAgentProfile();
        elasticAgentProfile1.setId("profile_id_1");
        elasticAgentProfile1.setPluginId(Constants.PLUGIN_ID);
        elasticAgentProfile1.setClusterProfileId(emptyCluster1.getId());

        ElasticAgentProfile elasticAgentProfile2 = new ElasticAgentProfile();
        elasticAgentProfile2.setId("profile_id_2");
        elasticAgentProfile2.setPluginId(Constants.PLUGIN_ID);
        elasticAgentProfile2.setClusterProfileId(emptyCluster1.getId());

        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(emptyCluster1, emptyCluster2), Arrays.asList(elasticAgentProfile1, elasticAgentProfile2));
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(pluginSettings));

        this.clusterProfile.setId(responseObject.getClusterProfiles().get(0).getId());
        assertThat(responseObject.getClusterProfiles().get(0), is(clusterProfile));

        //verify cluster is empty.. not migrated
        assertThat(responseObject.getClusterProfiles().get(1), is(emptyCluster2));

        assertThat(responseObject.getElasticAgentProfiles().get(0).getClusterProfileId(), is(emptyCluster1.getId()));
        assertThat(responseObject.getElasticAgentProfiles().get(1).getClusterProfileId(), is(emptyCluster1.getId()));
    }

    @Test
    public void shouldNotMigrateConfigWhenMultipleClusterProfilesAreAlreadyMigrated() throws Exception {
        ClusterProfile cluster1 = new ClusterProfile("cluster_profile_1", Constants.PLUGIN_ID, pluginSettings);
        ClusterProfile cluster2 = new ClusterProfile("cluster_profile_2", Constants.PLUGIN_ID, pluginSettings);

        ElasticAgentProfile elasticAgentProfile1 = new ElasticAgentProfile();
        elasticAgentProfile1.setId("profile_id_1");
        elasticAgentProfile1.setPluginId(Constants.PLUGIN_ID);
        elasticAgentProfile1.setClusterProfileId(cluster1.getId());

        ElasticAgentProfile elasticAgentProfile2 = new ElasticAgentProfile();
        elasticAgentProfile2.setId("profile_id_2");
        elasticAgentProfile2.setPluginId(Constants.PLUGIN_ID);
        elasticAgentProfile2.setClusterProfileId(cluster2.getId());

        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(cluster1, cluster2), Arrays.asList(elasticAgentProfile1, elasticAgentProfile2));
        MigrateConfigurationRequestExecutor executor = new MigrateConfigurationRequestExecutor(request);

        GoPluginApiResponse response = executor.execute();

        MigrateConfigurationRequest responseObject = MigrateConfigurationRequest.fromJSON(response.responseBody());

        assertThat(responseObject.getPluginSettings(), is(pluginSettings));

        assertThat(responseObject.getClusterProfiles(), is(Arrays.asList(cluster1, cluster2)));

        assertThat(responseObject.getElasticAgentProfiles(), is(Arrays.asList(elasticAgentProfile1, elasticAgentProfile2)));
    }
}
