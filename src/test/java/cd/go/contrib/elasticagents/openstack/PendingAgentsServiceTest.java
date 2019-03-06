package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.model.JobIdentifier;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PendingAgentsServiceTest {
    private final AgentInstances agentInstances;
    private final PendingAgentsService service;
    private final OpenStackInstance osInstance;
    private final JobIdentifier job1;
    private final PluginSettings pluginSettings;
    private PluginRequest pluginRequest;
    Map<String, String> props = new HashMap<>();

    private static final String IMAGE_ID = "7637f039-027d-471f-8d6c-4177635f84f8";
    private static final String FLAVOR_ID = "5";

    String instanceId = "84ea0a14-008b-48bb-a995-22f700282221";

    public PendingAgentsServiceTest() throws ServerRequestFailedException {
        agentInstances = mock(AgentInstances.class);
        pluginRequest = mock(PluginRequest.class);
        service = new PendingAgentsService(agentInstances);
        when(pluginRequest.listAgents()).thenReturn(new Agents());
        osInstance = mock(OpenStackInstance.class);
        when(osInstance.getImageIdOrName()).thenReturn(IMAGE_ID);
        when(osInstance.getFlavorIdOrName()).thenReturn(FLAVOR_ID);
        job1 = mock(JobIdentifier.class);
        pluginSettings = new PluginSettings();
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
    }

    @Test
    public void refreshAllShouldNotRemoveOkInstancesWhenDeleteEnabled() throws Exception {
        pluginSettings.setDeleteErrorInstances(true);
        when(osInstance.id()).thenReturn(instanceId);
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null);
        service.addPending(osInstance, originalRequest);
        when(agentInstances.doesInstanceExist(anyObject(), eq(instanceId))).thenReturn(true);
        when(agentInstances.isInstanceInErrorState(anyObject(), eq(instanceId))).thenReturn(false);
        service.refreshAll(pluginRequest);
        verify(agentInstances, times(0)).terminate(eq(instanceId), any(PluginSettings.class));
    }

    @Test
    public void refreshAllShouldRemoveInstancesInErrorStateWhenDeleteEnabled() throws Exception {
        pluginSettings.setDeleteErrorInstances(true);
        when(osInstance.id()).thenReturn(instanceId);
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null);
        service.addPending(osInstance, originalRequest);
        when(agentInstances.doesInstanceExist(anyObject(), eq(instanceId))).thenReturn(true);
        when(agentInstances.isInstanceInErrorState(anyObject(), eq(instanceId))).thenReturn(true);
        service.refreshAll(pluginRequest);
        verify(agentInstances, times(1)).terminate(eq(instanceId), any(PluginSettings.class));
    }

    @Test
    public void refreshAllShouldNotRemoveInstancesInErrorStateWhenDeleteDisabled() throws Exception {
        pluginSettings.setDeleteErrorInstances(false);
        when(osInstance.id()).thenReturn(instanceId);
        CreateAgentRequest originalRequest = new CreateAgentRequest("123", props, job1, null);
        service.addPending(osInstance, originalRequest);
        when(agentInstances.doesInstanceExist(anyObject(), eq(instanceId))).thenReturn(true);
        when(agentInstances.isInstanceInErrorState(anyObject(), eq(instanceId))).thenReturn(true);
        service.refreshAll(pluginRequest);
        verify(agentInstances, times(0)).terminate(eq(instanceId), any(PluginSettings.class));
    }
}
