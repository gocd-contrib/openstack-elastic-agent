package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.utils.OpenstackClientWrapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenStackInstancesTest {
    private OpenStackInstances instances;
    private String instanceId;
    private PluginSettings pluginSettings;
    private OpenStackInstance instance;
    private OpenstackClientWrapper client;

    @Before
    public void SetUp() {
        client = mock(OpenstackClientWrapper.class);
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        instances = new OpenStackInstances();
        instances.register(instance);
        pluginSettings = new PluginSettings();
        pluginSettings.setOpenstackEndpoint("http://some/url");
        pluginSettings.setOpenstackFlavor("default-flavor");
        pluginSettings.setOpenstackImage("7637f039-027d-471f-8d6c-4177635f84f8");
        pluginSettings.setOpenstackNetwork("780f2cfc-389b-4cc5-9b85-ed03a73975ee");
        pluginSettings.setOpenstackPassword("secret");
        pluginSettings.setOpenstackUser("user");
        pluginSettings.setOpenstackTenant("tenant");
        pluginSettings.setOpenstackVmPrefix("prefix-");

        when(client.getFlavorId("default-flavor")).thenReturn("a883873b-d568-4c1a-bee5-9806996e3a02");

        /*
        In tests we assume that
        - image 7637f039-027d-471f-8d6c-4177635f84f8 is called 'ubuntu-14'
        - flavor c1980bb5-ed59-4573-83c9-8391b53b3a55 is called 'm1.small'
         */
        when(client.getFlavorId("c1980bb5-ed59-4573-83c9-8391b53b3a55")).thenReturn("c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getFlavorId("m1.small")).thenReturn("c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getImageId("7637f039-027d-471f-8d6c-4177635f84f8")).thenReturn("7637f039-027d-471f-8d6c-4177635f84f8");
        when(client.getImageId("ubuntu-14")).thenReturn("7637f039-027d-471f-8d6c-4177635f84f8");
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasNoPropertiesButSettingsMatchAfterResolvingNames() throws Exception {
        pluginSettings.setOpenstackFlavor("m1.small");
        pluginSettings.setOpenstackImage("ubuntu-14");
        assertThat(instances.matchInstance(instanceId, new HashMap<String, String>(), pluginSettings, client), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasNoPropertiesButSettingsMatchById() throws Exception {
        pluginSettings.setOpenstackFlavor(instance.getFlavorId());
        pluginSettings.setOpenstackImage(instance.getImageId());
        assertThat(instances.matchInstance(instanceId, new HashMap<String, String>(), pluginSettings, client), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasPropertiesThatMatchById() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        assertThat(instances.matchInstance(instanceId, properties, pluginSettings, client), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasPropertiesThatMatchAfterResolvingNames() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "ubuntu-14");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "m1.small");
        assertThat(instances.matchInstance(instanceId, properties, pluginSettings, client), is(true));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenRequestHasNoPropertiesAndSettingsDontMatch() throws Exception {
        assertThat(instances.matchInstance(instanceId, new HashMap<String, String>(), pluginSettings, client), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenImageIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getImageId("5db97077-b9f0-46f9-a992-15708ad3b83d")).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, pluginSettings, client), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenFlavorIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d")).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, pluginSettings, client), is(false));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenFlavorIdAndImageIsEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        assertThat(instances.matchInstance(instanceId, properties, pluginSettings, client), is(true));
    }

}