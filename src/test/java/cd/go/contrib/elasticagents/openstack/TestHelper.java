package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestHelper {


    public static final String FLAVOR_ID1 = "c1980bb5-ed59-4573-83c9-8391b53b3a55";
    public static final String IMAGE_ID1 = "7637f039-027d-471f-8d6c-4177635f84f8";
    public static final String FLAVOR_M1_SMALL = "m1.small";
    public static final String IMAGE_UBUNTU_14 = "ubuntu-14";
    public static final String FLAVOR_ID2 = "a883873b-d568-4c1a-bee5-9806996e3a02";

    public static ClusterProfileProperties generateClusterProfileProperties(PROFILE_TYPE type) throws IOException {
        return ClusterProfileProperties.fromJSON(resourceAsString("/plugin-settings_" + type + ".json"));
    }

    public static String resourceAsString(String resourceName) throws IOException {
        try (InputStream is = Objects.requireNonNull(TestHelper.class.getResourceAsStream(resourceName))) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static PluginSettings generatePluginSettings(PROFILE_TYPE type) throws IOException {
        return generateClusterProfileProperties(type);
    }

    public enum PROFILE_TYPE {
        ID1, ID2, NAMES
    }
}
