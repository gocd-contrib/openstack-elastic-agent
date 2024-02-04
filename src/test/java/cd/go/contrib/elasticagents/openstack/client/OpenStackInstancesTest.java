package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.*;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeImageService;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OpenStackInstancesTest {
    Map<String, String> props = new HashMap<>();
    private OpenStackInstances instances;
    private String instanceId;
    private PluginSettings pluginSettings;
    private OpenStackInstance instance;
    private OpenstackClientWrapper client;
    private OpenStackClientFactory clientFactory;
    private String transactionId = UUID.randomUUID().toString();
    private JobIdentifier job1;
    private PluginRequest pluginRequest;

    @BeforeEach
    public void SetUp() throws ImageNotFoundException, ServerRequestFailedException, IOException {
        client = mock(OpenstackClientWrapper.class);
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.ID1);
        instance = new OpenStackInstance(instanceId, new Date(), null, TestHelper.IMAGE_ID1, TestHelper.FLAVOR_ID1, pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);

        when(client.getFlavorId(TestHelper.FLAVOR_M1_SMALL, transactionId)).thenReturn(TestHelper.FLAVOR_ID2);
        job1 = mock(JobIdentifier.class);
        pluginRequest = mock(PluginRequest.class);
        when(pluginRequest.listAgents()).thenReturn(new Agents());

        /*
        In tests we assume that
        - image 7637f039-027d-471f-8d6c-4177635f84f8 is called 'ubuntu-14'
        - flavor c1980bb5-ed59-4573-83c9-8391b53b3a55 is called 'm1.small'
         */
        when(client.getFlavorId(TestHelper.FLAVOR_ID1, transactionId)).thenReturn(TestHelper.FLAVOR_ID1);
        when(client.getFlavorId(TestHelper.FLAVOR_M1_SMALL, transactionId)).thenReturn(TestHelper.FLAVOR_ID1);
        when(client.getImageId(TestHelper.IMAGE_ID1, transactionId)).thenReturn(TestHelper.IMAGE_ID1);
        when(client.getImageId(TestHelper.IMAGE_UBUNTU_14, transactionId)).thenReturn(TestHelper.IMAGE_ID1);
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasNoPropertiesButSettingsMatchAfterResolvingNames() throws Exception {
        pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.NAMES);
        assertThat(instances.matchInstance(instanceId, new HashMap<>(), null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasNoPropertiesButSettingsMatchById() throws Exception {
        pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.ID1);
        assertThat(instances.matchInstance(instanceId, new HashMap<>(), null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasPropertiesThatMatchById() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_ID1);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_ID1);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasPropertiesThatMatchAfterResolvingNames() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_UBUNTU_14);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_M1_SMALL);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenRequestHasNoPropertiesAndSettingsDontMatch() throws Exception {
        pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.ID1);
        instance = new OpenStackInstance(instanceId, new Date(), null, TestHelper.IMAGE_ID1, TestHelper.FLAVOR_ID1, pluginSettings);
        pluginSettings = TestHelper.generatePluginSettings(TestHelper.PROFILE_TYPE.ID2);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        when(client.getFlavorId(anyString(), anyString())).thenReturn(TestHelper.FLAVOR_ID2);
        assertThat(instances.matchInstance(instanceId, new HashMap<>(), null, transactionId, false), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenImageIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_ID1);
        when(client.getImageId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenFlavorIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_ID1);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenEnvironmentIsNotEqual() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                TestHelper.IMAGE_ID1, TestHelper.FLAVOR_ID1, pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_ID1);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_ID1);
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(false));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenFlavorIdAndImageIsEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_ID1);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_ID1);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenEnvironmentIsEqual() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                TestHelper.IMAGE_ID1, TestHelper.FLAVOR_ID1, pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_ID1);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_ID1);
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, "testing", transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenEnvironmentIsNull() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                TestHelper.IMAGE_ID1, TestHelper.FLAVOR_ID1, pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_ID1);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_ID1);
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenFallbackImageIsUsedFromRequest() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                TestHelper.IMAGE_ID1, TestHelper.FLAVOR_ID1, pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_UBUNTU_14);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_M1_SMALL);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
        when(client.getImageId(TestHelper.IMAGE_UBUNTU_14, transactionId)).thenReturn("1a248c96-672b-4983-96ed-c3418a4be602");
        when(client.getPreviousImageId(TestHelper.IMAGE_UBUNTU_14, transactionId)).thenReturn(TestHelper.IMAGE_ID1);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenPreviousImageIsUsedFromRequestNoClientWrapperMock() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                TestHelper.IMAGE_ID1, TestHelper.FLAVOR_ID1, pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_UBUNTU_14);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_M1_SMALL);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
        when(client.getImageId(TestHelper.IMAGE_UBUNTU_14, transactionId)).thenReturn("1a248c96-672b-4983-96ed-c3418a4be602");
        when(client.getPreviousImageId(TestHelper.IMAGE_UBUNTU_14, transactionId)).thenReturn(TestHelper.IMAGE_ID1);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenFallbackImageIsUsedFromPluginSettings() throws Exception {

        // Arrange
        final String imageName = TestHelper.IMAGE_UBUNTU_14;
        String firstImageId = "firstImageId";
        String secondImageId = "secondImageId";
        String thirdImageId = "thirdImageId";
        OSClient osClient = mock(OSClient.class);
        final ComputeService compute = mock(ComputeService.class);
        when(osClient.compute()).thenReturn(compute);
        clientFactory = mock(OpenStackClientFactory.class);
        when(clientFactory.createClient(any())).thenReturn(osClient);
        final ComputeImageService imageService = mock(ComputeImageService.class);
        when(compute.images()).thenReturn(imageService);

        when(imageService.get(anyString())).thenReturn(null);

        List<Image> images = new ArrayList<>();
        Image imageWithNullName = mock(Image.class);
        when(imageWithNullName.getName()).thenReturn(null);
        when(imageWithNullName.getId()).thenReturn("imageWithNullNameId");
        images.add(imageWithNullName);

        Image imageWithName = mock(Image.class);
        when(imageWithName.getName()).thenReturn(imageName);
        when(imageWithName.getId()).thenReturn(firstImageId);
        images.add(imageWithName);

        doReturn(images).when(imageService).list();

        final String flavorId = TestHelper.FLAVOR_ID2;
        final Flavor flavor = mock(Flavor.class);
        when(flavor.getId()).thenReturn(flavorId);
        final FlavorService flavorService = mock(FlavorService.class);
        when(compute.flavors()).thenReturn(flavorService);
        when(flavorService.get(anyString())).thenReturn(flavor);
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                firstImageId, flavorId, pluginSettings);

        Cache<String, String> cache = new Cache2kBuilder<String, String>() {
        }
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .entryCapacity(100)
                .build();
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(pluginSettings, clientFactory, cache, null);
        instances = new OpenStackInstances(pluginSettings, clientWrapper);
        instances.register(instance);

        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, imageName);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, flavorId);

        // Act
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
        cache.clear();

        // Assert
        when(imageWithName.getId()).thenReturn(secondImageId);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
        cache.clear();

        when(imageWithName.getId()).thenReturn(thirdImageId);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(false));

    }

    @Test
    public void matchInstanceShouldReturnFalseWhenFallbackImageIsNotUsed() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                "1a248c96-672b-4983-96ed-c3418a4be602", TestHelper.FLAVOR_ID1, pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, TestHelper.IMAGE_UBUNTU_14);
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, TestHelper.FLAVOR_M1_SMALL);
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(false));
    }

    @Test
    public void getUserDataWhenNoneSpecified() {
        String result = instances.getUserData(new HashMap<>());
        assertNull(result);
    }

    @Test
    public void getUserDataWhenSpecifiedOnlyInPluginSettings() {
        pluginSettings.setOpenstackUserdata("script");
        String result = instances.getUserData(new HashMap<>());
        assertThat(result, is("script"));
    }

    @Test
    public void getUserDataWhenSpecifiedOnlyInCreateAgentRequest() {
        final Map<String, String> properties = new HashMap<>();
        properties.put("openstack_userdata", "script");
        String result = instances.getUserData(properties);
        assertThat(result, is("script"));
    }

    @Test
    public void getEncodedUserDataWhenNoneSpecified() {
        String result = instances.getEncodedUserData(new HashMap<>());
        assertNull(result);
    }

    @Test
    public void refreshPendingShouldNotRemoveOkInstancesWhenDeleteEnabled() throws Exception {
        // Arrange
        Server server = mock(Server.class);
        pluginSettings.setDeleteErrorInstances(true);
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null, new ClusterProfileProperties());
        System.out.println(instances.getPendingAgents().length);
        instances.addPending(instance, originalRequest);
        System.out.println(instances.getPendingAgents().length);
        when(client.getServer(eq(instanceId))).thenReturn(server);
        when(client.isInstanceInErrorState(eq(instanceId))).thenReturn(false);
        assertEquals(1, instances.getPendingAgents().length);

        // Act
        instances.refreshPending(pluginRequest);
        System.out.println(instances.getPendingAgents().length);

        // Assert
        assertEquals(1, instances.getPendingAgents().length);
        verify(client, times(0)).terminate(eq(instanceId));
    }

    @Test
    public void refreshPendingShouldRemoveInstancesInErrorStateWhenDeleteEnabled() throws Exception {
        // Arrange
        Server server = mock(Server.class);
        pluginSettings.setDeleteErrorInstances(true);
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null, new ClusterProfileProperties());
        System.out.println(instances.getPendingAgents().length);
        instances.addPending(instance, originalRequest);
        System.out.println(instances.getPendingAgents().length);
        when(client.getServer(eq(instanceId))).thenReturn(server);
        when(client.isInstanceInErrorState(eq(instanceId))).thenReturn(true);

        // Act
        instances.refreshPending(pluginRequest);
        System.out.println(instances.getPendingAgents().length);

        // Assert
        assertEquals(0, instances.getPendingAgents().length);
        verify(client, times(1)).terminate(eq(instanceId));
    }

    @Test
    public void refreshPendingShouldNotRemoveInstancesInErrorStateWhenDeleteDisabled() throws Exception {
        // Arrange
        Server server = mock(Server.class);
        pluginSettings.setDeleteErrorInstances(false);
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null, new ClusterProfileProperties());
        System.out.println(instances.getPendingAgents().length);
        instances.addPending(instance, originalRequest);
        System.out.println(instances.getPendingAgents().length);
        when(client.getServer(eq(instanceId))).thenReturn(server);
        when(client.isInstanceInErrorState(eq(instanceId))).thenReturn(true);
        assertEquals(1, instances.getPendingAgents().length);

        // Act
        instances.refreshPending(pluginRequest);
        System.out.println(instances.getPendingAgents().length);

        // Assert
        assertEquals(0, instances.getPendingAgents().length);
        verify(client, times(0)).terminate(eq(instanceId));
    }
}