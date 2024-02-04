/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cd.go.contrib.elasticagents.openstack;

import cd.go.contrib.elasticagents.openstack.model.Agent;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import java.util.*;

public class Agents {

    private final Map<String, Agent> agents = new HashMap<>();

    // Filter for agents that can be disabled safely
    private static final Predicate<Agent> AGENT_IDLE_PREDICATE = new Predicate<>() {
        @Override
        public boolean apply(Agent metadata) {
            Agent.AgentState agentState = metadata.agentState();
            return metadata.configState().equals(Agent.ConfigState.Enabled) && (agentState.equals(Agent.AgentState.Idle) || agentState.equals(Agent.AgentState.Missing) || agentState.equals(Agent.AgentState.LostContact));
        }
    };

    // Filter for agents that can be terminated safely
    private static final Predicate<Agent> AGENT_DISABLED_PREDICATE = new Predicate<>() {
        @Override
        public boolean apply(Agent metadata) {
            Agent.AgentState agentState = metadata.agentState();
            return metadata.configState().equals(Agent.ConfigState.Disabled) && (agentState.equals(Agent.AgentState.Idle) || agentState.equals(Agent.AgentState.Missing) || agentState.equals(Agent.AgentState.LostContact));
        }
    };

    public Agents() {

    }

    public Agents(Collection<Agent> toCopy) {
        for (Agent agent : toCopy) {
            agents.put(agent.elasticAgentId(), agent);
        }
    }

    public void addAll(Collection<Agent> toAdd) {
        for (Agent agent : toAdd) {
            add(agent);
        }
    }

    public void addAll(Agents agents) {
        addAll(agents.agents());
    }

    public Collection<Agent> findAgentsToDisable() {
        return FluentIterable.from(agents.values()).filter(AGENT_IDLE_PREDICATE).toList();
    }

    public Collection<Agent> findAgentsToTerminate() {
        return FluentIterable.from(agents.values()).filter(AGENT_DISABLED_PREDICATE).toList();
    }

    public Set<String> agentIds() {
        return new LinkedHashSet<>(agents.keySet());
    }

    public boolean containsAgentWithId(String agentId) {
        return agents.containsKey(agentId);
    }

    public Collection<Agent> agents() {
        return new ArrayList<>(agents.values());
    }

    public void add(Agent agent) {
        agents.put(agent.elasticAgentId(), agent);
    }

}
