package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class OpenStackInstanceTest {

    private CreateAgentRequest request;
    private PluginSettings settings;
    private HashMap<String, String> properties;

    @Before
    public void SetUpMocks() {
        properties = new HashMap<>();
        JobIdentifier job1 = mock(JobIdentifier.class);
        settings = new PluginSettings();
        request = new CreateAgentRequest("abc-key", properties, job1, "testing");
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