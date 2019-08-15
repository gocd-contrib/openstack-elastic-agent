package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.TestHelper;
import cd.go.contrib.elasticagents.openstack.client.OpenStackInstances;
import cd.go.contrib.elasticagents.openstack.requests.UpdateClusterConfigurationRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class UpdateClusterConfigurationExecutorTest {
    private UpdateClusterConfigurationRequest request;
    private OpenStackInstances instances;

    @Before
    public void setUp() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource("update_cluster_profile.json5")).getFile());
        request = UpdateClusterConfigurationRequest.fromJSON(FileUtils.readFileToString(file));
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
        final GoPluginApiResponse execute = executor.execute();

        // Assert
        assertEquals("10", instances.getPluginSettings().getAgentTTLMin());
        assertEquals(15, instances.getPluginSettings().getAgentTTLMax());
    }
}