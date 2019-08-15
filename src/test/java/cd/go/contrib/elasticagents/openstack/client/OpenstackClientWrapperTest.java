package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeImageService;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OpenstackClientWrapperTest {

    private String transactionId = UUID.randomUUID().toString();
    private PluginSettings pluginSettings;
    private OSClient client;
    private OpenStackClientFactory clientFactory;
    private ComputeService compute;

    @Before
    public void setUp() throws Exception {
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
        client = mock(OSClient.class);
        clientFactory = mock(OpenStackClientFactory.class);
        when(clientFactory.createClient(any())).thenReturn(client);
        compute = mock(ComputeService.class);
        when(client.compute()).thenReturn(compute);
//        final Token token = mock(Token.class);
//        when(client.getToken()).thenReturn(token);
//        when(token.getExpires()).thenReturn(new Date());
//        when(client.getAccess()).thenReturn(mock(Access.class));
    }

    @Test
    public void shouldGetImageIdGivenImageName() throws ImageNotFoundException {

        // Arrange
        String imageName = "ImageName";
        String expectedImageId = "ImageId";
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
        when(imageWithName.getId()).thenReturn(expectedImageId);
        images.add(imageWithName);

        doReturn(images).when(imageService).list();

        Cache<String, String> imageCache = new Cache2kBuilder<String, String>() {
        }.entryCapacity(100).build();

        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(pluginSettings, clientFactory, imageCache, null);

        // Act
        final String imageId = clientWrapper.getImageId(imageName, transactionId);

        // Assert
        assertEquals(expectedImageId, imageId);
    }

    @Test
    public void shouldGetPreviousImageIdGivenImageName() throws ImageNotFoundException {

        // Arrange
        String imageName = "ImageName";
        String firstImageId = "firstImageId";
        String secondImageId = "secondImageId";
        String thirdImageId = "thirdImageId";
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
        Cache<String, String> cache = new Cache2kBuilder<String, String>() {
        }
                .entryCapacity(100)
                .build();

        // Act
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(pluginSettings, clientFactory, cache, null);
        clientWrapper.resetPreviousImages();

        // Assert
        assertEquals(firstImageId, clientWrapper.getImageId(imageName, transactionId));
        assertEquals("", clientWrapper.getPreviousImageId(imageName, transactionId));

        when(imageWithName.getId()).thenReturn(secondImageId);
        cache.clear();
        assertEquals(secondImageId, clientWrapper.getImageId(imageName, transactionId));
        assertEquals(firstImageId, clientWrapper.getPreviousImageId(imageName, transactionId));

        when(imageWithName.getId()).thenReturn(thirdImageId);
        cache.clear();
        assertEquals(thirdImageId, clientWrapper.getImageId(imageName, transactionId));
        assertEquals(secondImageId, clientWrapper.getPreviousImageId(imageName, transactionId));
    }

    @Test
    public void testImageIdCache() throws Exception {
        // Arrange
        String imageName = "ImageName";
        String expectedImageId = "ImageId";
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
        when(imageWithName.getId()).thenReturn(expectedImageId);
        images.add(imageWithName);

        doReturn(images).when(imageService).list();

        Cache<String, String> cache = new Cache2kBuilder<String, String>() {
        }
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .entryCapacity(100)
                .build();

        // Act
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(pluginSettings, clientFactory, cache, null);

        // Assert
        verify(imageService, times(0)).list();
        //final String imageId = clientWrapper.getImageId(imageName, transactionId);
        assertEquals(expectedImageId, clientWrapper.getImageId(imageName, transactionId));
        verify(imageService, times(1)).list();
        assertEquals(expectedImageId, clientWrapper.getImageId(imageName, transactionId));
        verify(imageService, times(1)).list();
        System.out.println("sleep 1100");
        Thread.sleep(1100);
        assertEquals(expectedImageId, clientWrapper.getImageId(imageName, transactionId));
        verify(imageService, times(2)).list();
        assertEquals(expectedImageId, clientWrapper.getImageId(imageName, transactionId));
        verify(imageService, times(2)).list();

    }

    @Test
    public void testFlavorIdCache() throws Exception {
        // Arrange
        String flavorName = "m1.medium";
        String expectedFlavorId = "289349234";
        final FlavorService flavorService = mock(FlavorService.class);
        when(compute.flavors()).thenReturn(flavorService);

        when(flavorService.get(anyString())).thenReturn(null);

        List<Flavor> flavors = new ArrayList<>();
        Flavor flavorWithNullName = mock(Flavor.class);
        when(flavorWithNullName.getName()).thenReturn(null);
        when(flavorWithNullName.getId()).thenReturn("flavorWithNullNameId");
        flavors.add(flavorWithNullName);

        Flavor flavorWithName = mock(Flavor.class);
        when(flavorWithName.getName()).thenReturn(flavorName);
        when(flavorWithName.getId()).thenReturn(expectedFlavorId);
        flavors.add(flavorWithName);

        doReturn(flavors).when(flavorService).list();

        Cache<String, String> cache = new Cache2kBuilder<String, String>() {
        }
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .entryCapacity(100)
                .build();

        // Act
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(pluginSettings, clientFactory, null, cache);

        // Assert
        verify(flavorService, times(0)).list();
        //final String flavorId = clientWrapper.getFlavorId(flavorName, transactionId);
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(1)).list();
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(1)).list();
        System.out.println("sleep 1100");
        Thread.sleep(1100);
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(2)).list();
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(2)).list();
    }

    @Test
    @Ignore
    public void listServersInOSP13() {
        pluginSettings = new PluginSettings();
        pluginSettings.setOpenstackEndpoint("https://gotosp13.osp.jeppesensystems.com:13000/v3");
        pluginSettings.setOpenstackFlavor("m1.small");
        pluginSettings.setOpenstackImage("7637f039-027d-471f-8d6c-4177635f84f8");
        pluginSettings.setOpenstackNetwork("780f2cfc-389b-4cc5-9b85-ed03a73975ee");
        pluginSettings.setOpenstackPassword("jeppesen_gocd");
        pluginSettings.setOpenstackUser("gocd");
        pluginSettings.setOpenstackTenant("gocd");
        pluginSettings.setOpenstackVmPrefix("devel-13-");
        pluginSettings.setOpenstackKeystoneVersion("3");
        pluginSettings.setOpenstackDomain("Default");
        pluginSettings.setOpenstackImageCacheTTL("10");
        long startTimeMillis = System.currentTimeMillis();
        OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(pluginSettings);
        System.out.println("Create Client " + (System.currentTimeMillis() - startTimeMillis));

        startTimeMillis = System.currentTimeMillis();
        List<Server> allInstances = clientWrapper.listServers(pluginSettings.getOpenstackVmPrefix());
        System.out.println("listServers " + (System.currentTimeMillis() - startTimeMillis));
        String allInstancesAsString = allInstances.stream()
                .map(n -> n.getName())
                .collect(Collectors.joining(","));
        System.out.println(System.currentTimeMillis() - startTimeMillis);
        System.out.println("allInstances.size=[" + allInstances.size() + "] [" + allInstancesAsString + "]");

        for (int i = 0; i < 10; i++) {

            startTimeMillis = System.currentTimeMillis();
            clientWrapper = new OpenstackClientWrapper(pluginSettings);
            System.out.println("Create Client " + (System.currentTimeMillis() - startTimeMillis));

            startTimeMillis = System.currentTimeMillis();
            allInstances = clientWrapper.listServers(pluginSettings.getOpenstackVmPrefix());
            System.out.println("listServers " + (System.currentTimeMillis() - startTimeMillis));
        }

    }
}