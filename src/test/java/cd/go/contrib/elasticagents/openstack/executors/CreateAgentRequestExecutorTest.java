package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.*;
import cd.go.contrib.elasticagents.openstack.requests.CreateAgentRequest;
import cd.go.contrib.elasticagents.openstack.utils.OpenstackClientWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CreateAgentRequestExecutorTest {
    private CreateAgentRequest createAgentRequest;
    private AgentInstances agentInstances;
    private PluginRequest pluginRequest;
    private Agents agents;
    private PluginSettings pluginSettings;
    private OpenstackClientWrapper openstackClientWrapper;

    @Before
    public void SetUp() {
        createAgentRequest = mock(CreateAgentRequest.class);
        agentInstances = mock(AgentInstances.class);
        pluginRequest = mock(PluginRequest.class);
        agents = new Agents();
        openstackClientWrapper = mock(OpenstackClientWrapper.class);
        pluginSettings = new PluginSettings();
        pluginSettings.setOpenstackEndpoint("http://some/url");
        pluginSettings.setOpenstackFlavor("default-flavor");
        pluginSettings.setOpenstackImage("7637f039-027d-471f-8d6c-4177635f84f8");
        pluginSettings.setOpenstackNetwork("780f2cfc-389b-4cc5-9b85-ed03a73975ee");
        pluginSettings.setOpenstackPassword("secret");
        pluginSettings.setOpenstackUser("user");
        pluginSettings.setOpenstackTenant("tenant");
        pluginSettings.setOpenstackVmPrefix("prefix-");

    }

    @Test
    public void executeShouldCreateAgentWhenNoAgentsExist() throws Exception {
        // Arrange
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

    @Test
    public void executeShouldCreateAgentWhenOnlyBuildingAgentsExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

    @Test
    public void executeShouldNotCreateAgentWhenOnlyIdleAgentExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(0)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

    @Test
    public void executeShouldNotCreateAgentWhenIdleAndBuildingAgentExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(0)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

    @Test
    public void executeShouldNotCreateAgentWhenMaximumAgentsExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "3");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, "testing");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(0)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

    @Test
    public void executeShouldNotThrowNumberFormatExceptionWhenLimitIsEmptyString() throws Exception {
        // Arrange
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, "testing");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(1)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

    @Test
    public void executeShouldNotCreateAgentWhenMoreThanMaximumAgentsExist() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id4", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id5", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id6", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        properties.put(Constants.OPENSTACK_MAX_INSTANCE_LIMIT, "3");
        createAgentRequest = new CreateAgentRequest("abc-key", properties, "testing");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(0)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

    @Test
    public void executeShouldNotCreateAgentWhenMoreThanMaximumAgentsExistUsingPluginDefaultValue() throws Exception {
        // Arrange
        agents.add(new Agent("id1", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id2", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id3", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id4", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id5", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id6", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id7", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id8", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id9", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id10", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        agents.add(new Agent("id11", Agent.AgentState.Building, Agent.BuildState.Building, Agent.ConfigState.Enabled));
        when(pluginRequest.listAgents()).thenReturn(agents);
        Map<String, String> properties = new HashMap<>();
        createAgentRequest = new CreateAgentRequest("abc-key", properties, "testing");
        when(pluginRequest.getPluginSettings()).thenReturn(pluginSettings);
        when(agentInstances.matchInstance(anyString(), ArgumentMatchers.<String, String>anyMap(), any(PluginSettings.class), any(OpenstackClientWrapper
                .class))).thenReturn(true);
        CreateAgentRequestExecutor executor = new CreateAgentRequestExecutor(createAgentRequest, agentInstances, pluginRequest, openstackClientWrapper);

        // Act
        executor.execute();

        // Assert
        verify(agentInstances, times(0)).create(any(CreateAgentRequest.class), any(PluginSettings.class));
    }

}