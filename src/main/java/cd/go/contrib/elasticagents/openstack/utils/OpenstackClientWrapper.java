package cd.go.contrib.elasticagents.openstack.utils;

import cd.go.contrib.elasticagents.openstack.OpenStackClientFactory;
import cd.go.contrib.elasticagents.openstack.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;

/**
 * Common functions which call openstack API few times to get some job.
 * Easier to mock than actual client.
 */
public class OpenstackClientWrapper {
    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstance.class);

    private final OSClient client;
    private static final Map<String, List<String>> previousImageIds = new ConcurrentHashMap<>();

    private static Cache<String, String> imageCache;
    private static Cache<String, String> flavorCache;
    private static int imageCacheTTL = 30;

    public OpenstackClientWrapper(OSClient os, Cache<String, String> imageCache, Cache<String, String> flavorCache) {
        this.client = os;
        OpenstackClientWrapper.imageCache = imageCache;
        OpenstackClientWrapper.flavorCache = flavorCache;
    }

    public OpenstackClientWrapper(PluginSettings settings) {
        client = OpenStackClientFactory.os_client(settings);
        initCache(Integer.parseInt(settings.getOpenstackImageCacheTTL()));
    }

    public OSClient getClient() {
        return client;
    }

    public String getImageId(String nameOrId, String transactionId) throws ImageNotFoundException {
        LOG.debug(format("[{0}] [getImageId] nameOrId [{1}]", transactionId, nameOrId));
        String imageId = imageCache.get(nameOrId);
        if (imageId != null) {
            LOG.info(format("[{0}] [getImageId] found [{1}] with imageId [{2}] in imageCache", transactionId, nameOrId, imageId));
            return imageId;
        }
        LOG.info(format("[{0}] [getImageId] NOT found [{1}] in imageCache", transactionId, nameOrId));
        Image image = client.compute().images().get(nameOrId);
        if (image == null) {
            for (Image tmpImage : client.compute().images().list()) {
                String imageName = tmpImage.getName();
                if (imageName != null && imageName.equals(nameOrId)) {
                    if (!previousImageIds.containsKey(imageName)) {
                        LOG.debug(format("[{0}] [getImageId] initiate list of previous image is for name [{1}]", transactionId, imageName));
                        previousImageIds.put(tmpImage.getName(), new ArrayList<String>());
                    }
                    final List<String> usedImageIds = previousImageIds.get(imageName);
                    imageId = tmpImage.getId();
                    if (!usedImageIds.contains(imageId)) {
                        LOG.debug(format("[{0}] [getImageId] for image name [{1}] add id [{2}]", transactionId, imageName, imageId));
                        usedImageIds.add(imageId);
                    }
                    imageCache.put(nameOrId, imageId);
                    return imageId;
                }
            }
            LOG.error("Failed to find image by ID " + nameOrId);
            throw new ImageNotFoundException("Failed to find image " + nameOrId);
        } else {
            LOG.debug("Found image by ID " + nameOrId);
            imageCache.put(nameOrId, nameOrId);
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

    public String getFlavorId(String nameOrId, String transactionId) {
        LOG.debug(format("[{0}] [getFlavorId] nameOrId [{1}]", transactionId, nameOrId));
        String flavorId = flavorCache.get(nameOrId);
        if (flavorId != null) {
            LOG.info(format("[{0}] [getFlavorId] found [{1}] with flavorId [{2}] in flavorCache", transactionId, nameOrId, flavorId));
            return flavorId;
        }
        LOG.info(format("[{0}] [getFlavorId] NOT found [{1}] in flavorCache", transactionId, nameOrId));
        Flavor flavor = client.compute().flavors().get(nameOrId);
        if (flavor == null) {
            for (Flavor someFlavor : client.compute().flavors().list()) {
                String flavorName = someFlavor.getName();
                if (flavorName != null && flavorName.equals(nameOrId)) {
                    flavorCache.put(flavorName, someFlavor.getId());
                    return someFlavor.getId();
                }
            }
            throw new RuntimeException("Failed to find flavor by name " + nameOrId);
        } else {
            LOG.warn("Failed to find flavor by ID " + nameOrId);
            return nameOrId;
        }
    }

    public void resetPreviousImages() {
        previousImageIds.clear();
    }

    private synchronized void initCache(int minutesTTL) {

        if (OpenstackClientWrapper.imageCache == null || imageCacheTTL != minutesTTL) {
            LOG.info(format("[initCache] with TTL [{0}] minutes", minutesTTL));
            OpenstackClientWrapper.imageCacheTTL = minutesTTL;
            OpenstackClientWrapper.imageCache = new Cache2kBuilder<String, String>() {
            }
                    .expireAfterWrite(minutesTTL, TimeUnit.MINUTES)
                    .entryCapacity(100)
                    .build();

            OpenstackClientWrapper.flavorCache = new Cache2kBuilder<String, String>() {
            }
                    .expireAfterWrite(24, TimeUnit.HOURS)
                    .entryCapacity(100)
                    .build();

        }
    }
}
