package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.Constants;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.junit.Before;
import org.junit.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeImageService;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OpenStackInstancesTest {
    private OpenStackInstances instances;
    private String instanceId;
    private PluginSettings pluginSettings;
    private OpenStackInstance instance;
    private OpenstackClientWrapper client;
    private OpenStackClientFactory clientFactory;
    private String transactionId = UUID.randomUUID().toString();

    @Before
    public void SetUp() throws ImageNotFoundException {
        client = mock(OpenstackClientWrapper.class);
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        pluginSettings = new PluginSettings();
        pluginSettings.setOpenstackEndpoint("http://some/url");
        pluginSettings.setOpenstackFlavor("default-flavor");
        pluginSettings.setOpenstackImage("7637f039-027d-471f-8d6c-4177635f84f8");
        pluginSettings.setOpenstackNetwork("780f2cfc-389b-4cc5-9b85-ed03a73975ee");
        pluginSettings.setOpenstackPassword("secret");
        pluginSettings.setOpenstackUser("user");
        pluginSettings.setOpenstackTenant("tenant");
        pluginSettings.setOpenstackVmPrefix("prefix-");
        pluginSettings.setOpenstackKeystoneVersion("3");
        instance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);

        when(client.getFlavorId("default-flavor", transactionId)).thenReturn("a883873b-d568-4c1a-bee5-9806996e3a02");

        /*
        In tests we assume that
        - image 7637f039-027d-471f-8d6c-4177635f84f8 is called 'ubuntu-14'
        - flavor c1980bb5-ed59-4573-83c9-8391b53b3a55 is called 'm1.small'
         */
        when(client.getFlavorId("c1980bb5-ed59-4573-83c9-8391b53b3a55", transactionId)).thenReturn("c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getFlavorId("m1.small", transactionId)).thenReturn("c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getImageId("7637f039-027d-471f-8d6c-4177635f84f8", transactionId)).thenReturn("7637f039-027d-471f-8d6c-4177635f84f8");
        when(client.getImageId("ubuntu-14", transactionId)).thenReturn("7637f039-027d-471f-8d6c-4177635f84f8");
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasNoPropertiesButSettingsMatchAfterResolvingNames() throws Exception {
        pluginSettings.setOpenstackFlavor("m1.small");
        pluginSettings.setOpenstackImage("ubuntu-14");
        assertThat(instances.matchInstance(instanceId, new HashMap<String, String>(), null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasNoPropertiesButSettingsMatchById() throws Exception {
        pluginSettings.setOpenstackFlavor(instance.getFlavorIdOrName());
        pluginSettings.setOpenstackImage(instance.getImageIdOrName());
        assertThat(instances.matchInstance(instanceId, new HashMap<String, String>(), null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasPropertiesThatMatchById() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenRequestHasPropertiesThatMatchAfterResolvingNames() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "ubuntu-14");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "m1.small");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenRequestHasNoPropertiesAndSettingsDontMatch() throws Exception {
        assertThat(instances.matchInstance(instanceId, new HashMap<String, String>(), null, transactionId, false), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenImageIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getImageId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenFlavorIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenEnvironmentIsNotEqual() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(false));
    }


    @Test
    public void matchInstanceShouldReturnTrueWhenFlavorIdAndImageIsEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenEnvironmentIsEqual() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, "testing", transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenEnvironmentIsNull() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        when(client.getFlavorId("5db97077-b9f0-46f9-a992-15708ad3b83d", transactionId)).thenReturn("5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, false), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenFallbackImageIsUsedFromRequest() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "ubuntu-14");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "m1.small");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
        when(client.getImageId("ubuntu-14", transactionId)).thenReturn("1a248c96-672b-4983-96ed-c3418a4be602");
        when(client.getPreviousImageId("ubuntu-14", transactionId)).thenReturn("7637f039-027d-471f-8d6c-4177635f84f8");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenPreviousImageIsUsedFromRequestNoClientWrapperMock() throws Exception {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "ubuntu-14");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "m1.small");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
        when(client.getImageId("ubuntu-14", transactionId)).thenReturn("1a248c96-672b-4983-96ed-c3418a4be602");
        when(client.getPreviousImageId("ubuntu-14", transactionId)).thenReturn("7637f039-027d-471f-8d6c-4177635f84f8");
        assertThat(instances.matchInstance(instanceId, properties, null, transactionId, true), is(true));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenFallbackImageIsUsedFromPluginSettings() throws Exception {

        // Arrange
        final String imageName = "ubuntu-14";
        String firstImageId = "firstImageId";
        String secondImageId = "secondImageId";
        String thirdImageId = "thirdImageId";
        OSClient osClient = mock(OSClient.class);
        final ComputeService compute = mock(ComputeService.class);
        when(osClient.compute()).thenReturn(compute);
        clientFactory = mock(OpenStackClientFactory.class);
        when(clientFactory.createClient(any())).thenReturn(osClient);
//        final Token token = mock(Token.class);
//        when(osClient.getToken()).thenReturn(token);
//        when(token.getExpires()).thenReturn(new Date());
//        when(osClient.getAccess()).thenReturn(mock(Access.class));
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

        final String flavorId = "a883873b-d568-4c1a-bee5-9806996e3a02";
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
                "1a248c96-672b-4983-96ed-c3418a4be602", "c1980bb5-ed59-4573-83c9-8391b53b3a55", pluginSettings);
        instances = new OpenStackInstances(pluginSettings, client);
        instances.register(instance);
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "ubuntu-14");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "m1.small");
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
}