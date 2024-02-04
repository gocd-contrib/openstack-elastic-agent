package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.TestHelper;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstances;
import cd.go.contrib.elasticagents.openstack.requests.UpdateClusterConfigurationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class UpdateClusterConfigurationExecutorTest {
    private UpdateClusterConfigurationRequest request;
    private OpenStackInstances instances;

    @BeforeEach
    public void setUp() throws Exception {
        request = UpdateClusterConfigurationRequest.fromJSON(TestHelper.resourceAsString("/update_cluster_profile.json5"));
        PluginSettings pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.ID1);
        instances = new OpenStackInstances(pluginSettings);
    }

    @Test
    public void execute() throws Exception {
        // Arrange
        final UpdateClusterConfigurationExecutor executor = new UpdateClusterConfigurationExecutor(request, instances);
        assertEquals("20", instances.getPluginSettings().getAgentTTLMin());
        assertEquals(30, instances.getPluginSettings().getAgentTTLMax());

        // Act
        executor.execute();

        // Assert
        assertEquals("10", instances.getPluginSettings().getAgentTTLMin());
        assertEquals(15, instances.getPluginSettings().getAgentTTLMax());
    }
}