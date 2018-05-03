package cd.go.contrib.elasticagents.openstack.utils;

import org.junit.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeImageService;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.model.compute.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        // Act
        final OpenstackClientWrapper clientWrapper = new OpenstackClientWrapper(client);
        clientWrapper.resetPreviousImages();

        // Assert
        assertEquals(firstImageId, clientWrapper.getImageId(imageName, transactionId));
        assertEquals("", clientWrapper.getPreviousImageId(imageName, transactionId));

        when(imageWithName.getId()).thenReturn(secondImageId);
        assertEquals(secondImageId, clientWrapper.getImageId(imageName, transactionId));
        assertEquals(firstImageId, clientWrapper.getPreviousImageId(imageName, transactionId));

        when(imageWithName.getId()).thenReturn(thirdImageId);
        assertEquals(thirdImageId, clientWrapper.getImageId(imageName, transactionId));
        assertEquals(secondImageId, clientWrapper.getPreviousImageId(imageName, transactionId));
    }
}