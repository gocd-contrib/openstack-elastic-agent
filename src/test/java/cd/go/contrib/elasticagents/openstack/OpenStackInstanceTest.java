package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class OpenStackInstanceTest {

    private String instanceId;
    private OpenStackInstance instance;
    private CreateAgentRequest request;
    private PluginSettings settings;
    private HashMap<String, String> elasticAgentProfileProperties;
    private HashMap<String, String> clusterProfileProperties;

    @Before
    public void SetUpMocks() {
        elasticAgentProfileProperties = new HashMap<>();
        JobIdentifier job1 = mock(JobIdentifier.class);
        settings = new PluginSettings();
        request = new CreateAgentRequest("abc-key", elasticAgentProfileProperties, job1, "testing", clusterProfileProperties);
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

    @Test
    public void shouldReportDoneSecondTimeJobIsRunGivenMax2AllowedTimes() {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        instance.setMaxCompletedJobs("2");
        assertFalse(instance.incrementJobsCompleted());
        assertTrue(instance.incrementJobsCompleted());
        assertTrue(instance.incrementJobsCompleted());
    }

    @Test
    public void shouldNotReportDoneGivenNoMaxSet() {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55");
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
    }

}