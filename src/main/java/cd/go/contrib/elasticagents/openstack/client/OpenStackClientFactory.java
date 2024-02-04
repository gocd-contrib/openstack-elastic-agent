/*
 * Copyright 2019 Thoughtworks, Inc.
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
package cd.go.contrib.elasticagents.openstack.client;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import com.thoughtworks.go.plugin.api.logging.Logger;
import org.openstack4j.api.OSClient;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

class OpenStackClientFactory {

    public static final Logger LOG = Logger.getLoggerFor(OpenStackClientFactory.class);

    private Config createConfig(PluginSettings pluginSettings) {
        LOG.debug("createConfig: PluginSettings={} ", pluginSettings);
        Config config = Config.newConfig();
        if (pluginSettings.getSSLVerificationDisabled()) {
            config.withSSLVerificationDisabled();
        }
        return config;
    }

    OSClient createClient(PluginSettings pluginSettings) {
        LOG.debug("createClient: PluginSettings={} ", pluginSettings);
        if (pluginSettings.getOpenstackKeystoneVersion().equals("3")) {
            LOG.debug("OpenStack Authentication V3" + " Endpoint: " + pluginSettings.getOpenstackEndpoint()
                    + " User: " + pluginSettings.getOpenstackUser()
                    + " Domain: " + pluginSettings.getOpenstackDomain()
                    + " Tenant: " + pluginSettings.getOpenstackTenant());
            return OSFactory.builderV3()
                    .endpoint(pluginSettings.getOpenstackEndpoint())
                    .credentials(pluginSettings.getOpenstackUser(), pluginSettings.getOpenstackPassword(), Identifier.byName(pluginSettings.getOpenstackDomain()))
                    .scopeToProject(Identifier.byName(pluginSettings.getOpenstackTenant()), Identifier.byName(pluginSettings.getOpenstackDomain()))
                    .withConfig(createConfig(pluginSettings))
                    .authenticate();

        } else {
            LOG.debug("OpenStack Authentication V2" + " Endpoint : " + pluginSettings.getOpenstackEndpoint()
                    + " User : " + pluginSettings.getOpenstackUser() + " Tenant : " + pluginSettings.getOpenstackTenant());
            return OSFactory.builderV2()
                    .endpoint(pluginSettings.getOpenstackEndpoint())
                    .credentials(pluginSettings.getOpenstackUser(), pluginSettings.getOpenstackPassword())
                    .tenantName(pluginSettings.getOpenstackTenant())
                    .withConfig(createConfig(pluginSettings))
                    .authenticate();
        }
    }

}
