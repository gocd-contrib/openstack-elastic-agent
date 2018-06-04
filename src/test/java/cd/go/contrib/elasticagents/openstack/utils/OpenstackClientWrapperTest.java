package cd.go.contrib.elasticagents.openstack.utils;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.junit.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeImageService;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OpenstackClientWrapperTest {

    private String transactionId = UUID.randomUUID().toString();

    @Test
    public void shouldGetImageIdGivenImageName() {

        // Arrange
        String imageName = "ImageName";
        String expectedImageId = "ImageId";
        OSClient client = mock(OSClient.class);
        final ComputeService compute = mock(ComputeService.class);
        when(client.compute()).thenReturn(compute);
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

        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(client);

        // Act
        final String imageId = clientWrapper.getImageId(imageName, transactionId);

        // Assert
        assertEquals(expectedImageId, imageId);
    }

    @Test
    public void shouldGetPreviousImageIdGivenImageName() {

        // Arrange
        String imageName = "ImageName";
        String firstImageId = "firstImageId";
        String secondImageId = "secondImageId";
        String thirdImageId = "thirdImageId";
        OSClient client = mock(OSClient.class);
        final ComputeService compute = mock(ComputeService.class);
        when(client.compute()).thenReturn(compute);
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
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(client, cache, null);
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
        OSClient client = mock(OSClient.class);
        final ComputeService compute = mock(ComputeService.class);
        when(client.compute()).thenReturn(compute);
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
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(client, cache, null);

        // Assert
        verify(imageService, times(0)).list();
        //final String imageId = clientWrapper.getImageId(imageName, transactionId);
        assertEquals(expectedImageId, clientWrapper.getImageId(imageName, transactionId));
        verify(imageService, times(1)).list();
        assertEquals(expectedImageId, clientWrapper.getImageId(imageName, transactionId));
        verify(imageService, times(1)).list();
        System.out.println("sleep 1000");
        Thread.sleep(1000);
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
        OSClient client = mock(OSClient.class);
        final ComputeService compute = mock(ComputeService.class);
        when(client.compute()).thenReturn(compute);
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
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(client, null, cache);

        // Assert
        verify(flavorService, times(0)).list();
        //final String flavorId = clientWrapper.getFlavorId(flavorName, transactionId);
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(1)).list();
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(1)).list();
        System.out.println("sleep 1000");
        Thread.sleep(1000);
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(2)).list();
        assertEquals(expectedFlavorId, clientWrapper.getFlavorId(flavorName, transactionId));
        verify(flavorService, times(2)).list();
    }
}