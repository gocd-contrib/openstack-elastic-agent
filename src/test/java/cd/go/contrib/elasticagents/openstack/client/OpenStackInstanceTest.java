package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenStackInstanceTest {

    private String instanceId;
    private OpenStackInstance instance;
    private PluginSettings settings;

    @BeforeEach
    public void setUp() {
        settings = new PluginSettings();
    }

    @Test
    public void shouldReportDoneSecondTimeJobIsRunGivenMax2AllowedTimes() {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", settings);
        instance.setMaxCompletedJobs("2");
        assertFalse(instance.incrementJobsCompleted());
        assertTrue(instance.incrementJobsCompleted());
        assertTrue(instance.incrementJobsCompleted());
    }

    @Test
    public void shouldNotReportDoneGivenNoMaxSet() {
        instanceId = "b45b5658-b093-4a58-bf22-17d898171c95";
        instance = new OpenStackInstance(instanceId, new Date(), "testing",
                "7637f039-027d-471f-8d6c-4177635f84f8", "c1980bb5-ed59-4573-83c9-8391b53b3a55", settings);
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
        assertFalse(instance.incrementJobsCompleted());
    }

}