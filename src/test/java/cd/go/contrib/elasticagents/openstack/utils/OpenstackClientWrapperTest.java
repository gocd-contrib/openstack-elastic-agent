package cd.go.contrib.elasticagents.openstack.utils;

import org.junit.Test;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeImageService;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.model.compute.Image;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OpenstackClientWrapperTest {


    @Test
    public void getImageId() {

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
        final String imageId = clientWrapper.getImageId(imageName);

        // Assert
        assertEquals(expectedImageId, imageId);
    }
}