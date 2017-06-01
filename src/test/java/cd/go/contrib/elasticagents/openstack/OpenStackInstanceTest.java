package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import org.junit.Before;
import org.junit.Test;
import org.openstack4j.api.OSClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class OpenStackInstanceTest {

    private CreateAgentRequest request;
    private PluginSettings settings;
    private OSClient client;
    HashMap<String, String> properties;

    @Before
    public void SetUpMocks() {
        client = mock(OSClient.class);
        properties = new HashMap<>();
        settings = new PluginSettings();
        request = new CreateAgentRequest("abc-key", properties,"testing");
    }

    @Test
    public void getUserDataWhenNoneSpecified() {
        String result = OpenStackInstance.getUserData(request, settings);
        assertNull(result);
    }

    @Test
    public void getUserDataWhenSpecifiedOnlyInPluginSettings() {
        settings.setOpenstackUserdata("script");
        String result = OpenStackInstance.getUserData(request, settings);
        assertThat(result, is("script"));
    }

    @Test
    public void getUserDataWhenSpecifiedOnlyInCreateAgentRequest() {
        request.properties().put("openstack_userdata", "script");
        String result = OpenStackInstance.getUserData(request, settings);
        assertThat(result, is("script"));
    }

    @Test
    public void getEncodedUserDataWhenNoneSpecified() {
        String result = OpenStackInstance.getEncodedUserData(request, settings);
        assertNull(result);
    }

}