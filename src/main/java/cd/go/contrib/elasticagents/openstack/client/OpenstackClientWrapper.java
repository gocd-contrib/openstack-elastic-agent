package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.OS4JException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.text.MessageFormat.format;

/**
 * Common functions which call OpenStack API few times to get some job.
 * Easier to mock than actual client.
 */
class OpenstackClientWrapper {
    public static final Logger LOG = Logger.getLoggerFor(OpenstackClientWrapper.class);
    private final PluginSettings pluginSettings;
    private final Map<String, List<String>> previousImageIds = new ConcurrentHashMap<>();
    private final OpenStackClientFactory clientFactory;
    private Cache<String, String> imageCache;
    private Cache<String, String> flavorCache;
    private int imageCacheTTL = 30;

    OpenstackClientWrapper(PluginSettings pluginSettings, OpenStackClientFactory clientFactory, Cache<String, String> imageCache, Cache<String, String> flavorCache) {
        this.pluginSettings = pluginSettings;
        this.clientFactory = clientFactory;
        this.imageCache = imageCache;
        this.flavorCache = flavorCache;
    }

    OpenstackClientWrapper(PluginSettings pluginSettings) {
        LOG.debug(format("new OpenstackClientWrapper, PluginSettings:[{0}] ", pluginSettings));
        this.pluginSettings = pluginSettings;
        this.clientFactory = new OpenStackClientFactory();
        initCache(Integer.parseInt(pluginSettings.getOpenstackImageCacheTTL()));
    }

    String getImageId(String nameOrId, String transactionId) throws ImageNotFoundException {
        LOG.debug(format("[{0}] [getImageId] nameOrId [{1}] for {2}", transactionId, nameOrId, pluginSettings.getOpenstackEndpoint()));
        String imageId = imageCache.get(nameOrId);
        if (imageId != null) {
            LOG.info(format("[{0}] [getImageId] found [{1}] with imageId [{2}] in imageCache", transactionId, nameOrId, imageId));
            return imageId;
        }
        final OSClient client = clientFactory.createClient(pluginSettings);
        LOG.info(format("[{0}] [getImageId] NOT found [{1}] in imageCache", transactionId, nameOrId));
        Image image = client.compute().images().get(nameOrId);
        if (image == null) {
            for (Image tmpImage : client.compute().images().list()) {
                String imageName = tmpImage.getName();
                if (imageName != null && imageName.equals(nameOrId)) {
                    if (!previousImageIds.containsKey(imageName)) {
                        LOG.debug(format("[{0}] [getImageId] initiate list of previous image is for name [{1}]", transactionId, imageName));
                        previousImageIds.put(tmpImage.getName(), new ArrayList<>());
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

    String getPreviousImageId(String imageName, String transactionId) {
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

    String getFlavorId(String nameOrId, String transactionId) {
        LOG.debug(format("[{0}] [getFlavorId] nameOrId [{1}]", transactionId, nameOrId));
        String flavorId = flavorCache.get(nameOrId);
        if (flavorId != null) {
            LOG.info(format("[{0}] [getFlavorId] found [{1}] with flavorId [{2}] in flavorCache", transactionId, nameOrId, flavorId));
            return flavorId;
        }
        final OSClient client = clientFactory.createClient(pluginSettings);
        LOG.info(format("[{0}] [getFlavorId] NOT found [{1}] in flavorCache", transactionId, nameOrId));
        Flavor flavor = null;
        try {
            flavor = client.compute().flavors().get(nameOrId);
        } catch (Exception ex) {
            LOG.warn(format("[{0}] [getFlavorId] nameOrId [{1}] Exception thrown ", transactionId, nameOrId), ex);
        }
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

    void resetPreviousImages() {
        previousImageIds.clear();
    }

    private synchronized void initCache(int minutesTTL) {

        if (this.imageCache == null || imageCacheTTL != minutesTTL) {
            LOG.info(format("[initCache] with TTL [{0}] minutes", minutesTTL));
            this.imageCacheTTL = minutesTTL;
            this.imageCache = new Cache2kBuilder<String, String>() {
            }
                    .expireAfterWrite(minutesTTL, TimeUnit.MINUTES)
                    .entryCapacity(100)
                    .build();

            this.flavorCache = new Cache2kBuilder<String, String>() {
            }
                    .expireAfterWrite(24, TimeUnit.HOURS)
                    .entryCapacity(100)
                    .build();

        }
    }

    boolean instanceNameExists(String instance_name) {
        final OSClient client = clientFactory.createClient(pluginSettings);
        Map<String, String> newInstance = new HashMap<>();
        newInstance.put("name", instance_name);
        return !client.compute().servers().list(newInstance).isEmpty();
    }

    synchronized Server bootServer(ServerCreate build) {
        final OSClient client = clientFactory.createClient(pluginSettings);
        return client.compute().servers().boot(build);
    }

    Server getServer(String id) throws InstanceNotFoundException {
        LOG.debug("[getServer] id=[{}]", id);
        final OSClient client = clientFactory.createClient(pluginSettings);
        Server server;
        try {
            server = client.compute().servers().get(id);
            LOG.debug("[getServer] server=[{}]", server);
        } catch (Exception ex) {
            LOG.error("[getServer] id=[{}] Exception=[{}]", id, ex);
            throw ex;
        }
        if (server == null) {
            throw new InstanceNotFoundException("Server is null");
        }
        return server;
    }

    ActionResponse terminate(String id) throws OS4JException {
        LOG.debug("[terminate] id=[{}]", id);
        final OSClient client = clientFactory.createClient(pluginSettings);
        final ActionResponse response = client.compute().servers().delete(id);
        LOG.debug("[terminate] id=[{}] response.isSuccess()=[{}] response.getFault()=[{}] response.getCode()=[{}]",
                id, response.isSuccess(), response.getFault(), response.getCode());
        return response;
    }

    List<Server> listServers(String prefix) {
        LOG.debug("[listServers] prefix=[{}]", prefix);
        final OSClient client = clientFactory.createClient(pluginSettings);
        Map<String, String> op_instance_prefix = new HashMap<>();
        op_instance_prefix.put("name", prefix);
        return (List<Server>) client.compute().servers().list(op_instance_prefix);
    }

    boolean isInstanceInErrorState(String id) {
        final OSClient client = clientFactory.createClient(pluginSettings);
        final Server server = client.compute().servers().get(id);
        if (server == null) {
            return false;
        }
        return server.getStatus() == Server.Status.ERROR;
    }
}
