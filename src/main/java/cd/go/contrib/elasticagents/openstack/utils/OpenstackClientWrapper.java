package cd.go.contrib.elasticagents.openstack.utils;

import cd.go.contrib.elasticagents.openstack.OpenStackClientFactory;
import cd.go.contrib.elasticagents.openstack.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.text.MessageFormat.format;

/**
 * Common functions which call openstack API few times to get some job.
 * Easier to mock than actual client.
 */
public class OpenstackClientWrapper {
    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstance.class);

    private final OSClient client;
    private static final Map<String, List<String>> previousImageIds = new ConcurrentHashMap<>();

    public OpenstackClientWrapper(OSClient os) {
        this.client = os;
    }

    public OpenstackClientWrapper(PluginSettings settings) throws Exception {
        client = OpenStackClientFactory.os_client(settings);
    }

    public OSClient getClient() {
        return client;
    }

    public String getImageId(String nameOrId, String transactionId) {
        LOG.debug(format("[{0}] [getImageIdOrName] nameOrId [{1}]", transactionId, nameOrId));
        Image image = client.compute().images().get(nameOrId);
        if (image == null) {
            for (Image tmpImage : client.compute().images().list()) {
                String imageName = tmpImage.getName();
                if (tmpImage.getName() != null && imageName.equals(nameOrId)) {
                    if (!previousImageIds.containsKey(imageName)) {
                        LOG.debug(format("[{0}] [getImageIdOrName] initiate list of previous image is for name [{1}]", transactionId, imageName));
                        previousImageIds.put(tmpImage.getName(), new ArrayList<String>());
                    }
                    final List<String> usedImageIds = previousImageIds.get(imageName);
                    final String imageId = tmpImage.getId();
                    if (!usedImageIds.contains(imageId)) {
                        LOG.debug(format("[{0}] [getImageIdOrName] for image name [{1}] add id [{2}]", transactionId, imageName, imageId));
                        usedImageIds.add(imageId);
                    }
                    return imageId;
                }
            }
            throw new RuntimeException("Failed to find image " + nameOrId);
        } else {
            LOG.warn("Failed to find image by ID " + nameOrId);
            return nameOrId;
        }
    }

    public String getPreviousImageId(String imageName, String transactionId) {
        LOG.debug(format("[{0}] [getPreviousImageId] get id for image name [{1}]", transactionId, imageName));
        if (previousImageIds.containsKey(imageName))
            if (!previousImageIds.get(imageName).isEmpty()) {
                final List<String> list = previousImageIds.get(imageName);
                LOG.debug(format("[{0}] [getPreviousImageId] for image name [{1}] list: [{2}]", transactionId, imageName, list));
                if (list.size() > 1) {
                    return list.get(list.size() - 2);
                }
            }
        return "";
    }

    public String getFlavorId(String flavorNameOrId) {
        Flavor flavor = client.compute().flavors().get(flavorNameOrId);
        if (flavor == null) {
            for (Flavor someFlavor : client.compute().flavors().list()) {
                if (someFlavor.getName().equals(flavorNameOrId))
                    return someFlavor.getId();
            }
            throw new RuntimeException("Failed to find flavor by name " + flavorNameOrId);
        } else {
            LOG.warn("Failed to find flavor by ID " + flavorNameOrId);
            return flavorNameOrId;
        }
    }

    public void resetPreviousImages() {
        previousImageIds.clear();
    }
}
