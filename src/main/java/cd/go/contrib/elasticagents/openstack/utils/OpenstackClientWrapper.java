package cd.go.contrib.elasticagents.openstack.utils;

import cd.go.contrib.elasticagents.openstack.OpenStackClientFactory;
import cd.go.contrib.elasticagents.openstack.OpenStackInstance;
import cd.go.contrib.elasticagents.openstack.PluginSettings;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;

/**
 * Common functions which call openstack API few times to get some job.
 * Easier to mock than actual client.
 */
public class OpenstackClientWrapper {
    public static final Logger LOG = Logger.getLoggerFor(OpenStackInstance.class);

    private final OSClient client;

    public OpenstackClientWrapper(OSClient os) {
        this.client = os;
    }

    public OpenstackClientWrapper(PluginSettings settings) throws Exception {
        client = OpenStackClientFactory.os_client(settings);
    }

    public OSClient getClient() {
        return client;
    }

    public String getImageId(String nameOrId) {
        Image image = client.compute().images().get(nameOrId);
        if (image == null) {
            for (Image tmpImage : client.compute().images().list()){
                if (tmpImage.getName() != null && tmpImage.getName().equals(nameOrId)) {
                    return tmpImage.getId();
                }
            }
            throw new RuntimeException("Failed to find image " + nameOrId);
        } else {
            LOG.warn("Failed to find image by ID " + nameOrId);
            return nameOrId;
        }
    }

    public String getFlavorId(String flavorNameOrId) {
        Flavor flavor = client.compute().flavors().get(flavorNameOrId);
        if(flavor == null) {
            for(Flavor someFlavor : client.compute().flavors().list()) {
                if(someFlavor.getName().equals(flavorNameOrId))
                    return someFlavor.getId();
            }
            throw new RuntimeException("Failed to find flavor by name " + flavorNameOrId);
        }
        else {
            LOG.warn("Failed to find flavor by ID " + flavorNameOrId);
            return flavorNameOrId;
        }
    }
}
