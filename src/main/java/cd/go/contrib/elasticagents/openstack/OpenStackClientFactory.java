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

import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

import java.util.Date;

import static cd.go.contrib.elasticagents.openstack.OpenStackPlugin.LOG;
import org.openstack4j.core.transport.Config;

public class OpenStackClientFactory {

    private static OSClient client;
    private static PluginSettings pluginSettings;

    public static synchronized OSClient os_client(PluginSettings pluginSettings) throws Exception {
        if (pluginSettings.equals(OpenStackClientFactory.pluginSettings) && OpenStackClientFactory.client != null) {
            if (OpenStackClientFactory.client.getToken().getExpires().after(new Date(System.currentTimeMillis() + 5 * 60 * 1000))) {
                LOG.debug("OpenStackClientFactory - token is still valid : " + OpenStackClientFactory.client.getToken().getExpires().toString());
                return OSFactory.clientFromAccess(client.getAccess(), createConfig(pluginSettings));
            }
        }
        OpenStackClientFactory.pluginSettings = pluginSettings;
        OpenStackClientFactory.client = createClient(pluginSettings);
        return OpenStackClientFactory.client;
    }
    
    private static Config createConfig(PluginSettings pluginSettings) {
        Config config = Config.newConfig();
        if (pluginSettings.getSSLVerificationDisabled()) {
            config.withSSLVerificationDisabled();
        }
        return config;
    }

    private static OSClient createClient(PluginSettings pluginSettings) throws Exception {
        if (OpenStackClientFactory.client == null) {
            LOG.debug("OpenStackClientFactory - get new token from OpenStack");
        } else {
            LOG.debug("OpenStackClientFactory - token expired or will expire ("
                    + OpenStackClientFactory.client.getToken().getExpires().toString() + "), get a new token from OpenStack");
        }
        if (pluginSettings.getOpenstackKeystoneVersion().equals("3")) {
            LOG.debug("Openstack Authentication V3");
            LOG.debug("Endpoint : " + pluginSettings.getOpenstackEndpoint());
            LOG.debug("User : " + pluginSettings.getOpenstackUser());
            LOG.debug("Domain : " + pluginSettings.getOpenstackDomain());
            LOG.debug("Tenant : " + pluginSettings.getOpenstackTenant());
            return OSFactory.builderV3()
                    .endpoint(pluginSettings.getOpenstackEndpoint())
                    .credentials(pluginSettings.getOpenstackUser(), pluginSettings.getOpenstackPassword(), Identifier.byName(pluginSettings.getOpenstackDomain()))
                    .scopeToProject(Identifier.byName(pluginSettings.getOpenstackTenant()), Identifier.byName(pluginSettings.getOpenstackDomain()))
                    .withConfig(createConfig(pluginSettings))
                    .authenticate();

        } else {
            LOG.debug("Openstack Authentication V2");
            LOG.debug("Endpoint : " + pluginSettings.getOpenstackEndpoint());
            LOG.debug("User : " + pluginSettings.getOpenstackUser());
            LOG.debug("Tenant : " + pluginSettings.getOpenstackTenant());
            return OSFactory.builder()
                    .endpoint(pluginSettings.getOpenstackEndpoint())
                    .credentials(pluginSettings.getOpenstackUser(), pluginSettings.getOpenstackPassword())
                    .tenantName(pluginSettings.getOpenstackTenant())
                    .withConfig(createConfig(pluginSettings))
                    .authenticate();
        }
    }

}
