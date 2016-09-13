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

import cd.go.contrib.elasticagents.openstack.utils.Util;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;

import java.util.Collections;

public interface Constants {
    String PLUGIN_ID = Util.pluginId();

    // The type of this extension
    String EXTENSION_TYPE = "elastic-agent";

    // The extension point API version that this plugin understands
    String API_VERSION = "1.0";

    // the identifier of this plugin
    GoPluginIdentifier PLUGIN_IDENTIFIER = new GoPluginIdentifier(EXTENSION_TYPE, Collections.singletonList(API_VERSION));

    // requests that the plugin makes to the server
    String REQUEST_SERVER_PREFIX = "go.processor";
    String PROCESS_DISABLE_AGENTS = REQUEST_SERVER_PREFIX + ".elastic-agents.disable-agents";
    String PROCESS_DELETE_AGENTS = REQUEST_SERVER_PREFIX + ".elastic-agents.delete-agents";
    String REQUEST_SERVER_GET_PLUGIN_SETTINGS = REQUEST_SERVER_PREFIX + ".plugin-settings.get";
    String REQUEST_SERVER_LIST_AGENTS = REQUEST_SERVER_PREFIX + ".elastic-agents.list-agents";

    // internal use only
    String GOAGENT_PROPERTIES_PREFIX = "goagent_";
    String GOSERVER_PROPERTIES_PREFIX = "goserver_";
    String ENVIRONMENT_KEY = "agent.auto.register.environments";
    String PLUGIN_ID_KEY = "agent.auto.register.elasticAgent.pluginId";
    String REGISTER_KEY = "agent.auto.register.key";
    String BOOTSTRAPPER_KEY = "AGENT_BOOTSTRAPPER_ARGS";
    String BOOTSTRAPPER_ARGS = "openstack_go_agent_bootstrapper";
    String GO_SERVER_URL_KEY = "GO_SERVER_URL";
    String GO_SERVER_URL_ARGS = "openstack_go_server_url";
    String OPENSTACK_IMAGE_ID_ARGS = "openstack_image_id";
    String OPENSTACK_NETWORK_ID_ARGS = "openstack_network_id";
    String OPENSTACK_FLAVOR_ID_ARGS = "openstack_flavor_id";

}
