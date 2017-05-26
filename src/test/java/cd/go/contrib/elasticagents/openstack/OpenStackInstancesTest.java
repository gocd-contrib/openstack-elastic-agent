package cd.go.contrib.elasticagents.openstack;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class OpenStackInstancesTest {
    private OpenStackInstances instances;
    private String instanceId;

    @Before
    public void SetUp() {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        OpenStackInstance instance = new OpenStackInstance(instanceId, new Date(), null,
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        instances = new OpenStackInstances();
        instances.register(instance);
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenNoProperties() throws Exception {
        assertThat(instances.matchInstance(instanceId, new HashMap<String, String>()), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenImageIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        assertThat(instances.matchInstance(instanceId, properties), is(false));
    }

    @Test
    public void matchInstanceShouldReturnFalseWhenFlavorIdIsNotEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "5db97077-b9f0-46f9-a992-15708ad3b83d");
        assertThat(instances.matchInstance(instanceId, properties), is(false));
    }

    @Test
    public void matchInstanceShouldReturnTrueWhenFlavorIdAndImageIsEqual() throws Exception {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_IMAGE_ID_ARGS, "7637f039-027d-471f-8d6c-4177635f84f8");
        properties.put(Constants.OPENSTACK_FLAVOR_ID_ARGS, "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        assertThat(instances.matchInstance(instanceId, properties), is(true));
    }

}