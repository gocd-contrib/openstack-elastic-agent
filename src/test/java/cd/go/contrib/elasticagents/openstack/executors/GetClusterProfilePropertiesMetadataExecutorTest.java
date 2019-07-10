/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.model.Metadata;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.lang.reflect.Type;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class GetClusterProfilePropertiesMetadataExecutorTest {

    @Test
    public void shouldSerializeAllFields() throws Exception {
        GoPluginApiResponse response = new GetClusterProfileMetadataExecutor().execute();
        final Type type = new TypeToken<List<Metadata>>() {
        }.getType();

        List<Metadata> list = new Gson().fromJson(response.responseBody(), type);
        assertEquals(list.size(), GetClusterProfileMetadataExecutor.FIELDS.size());
    }

    @Test
    public void assertJsonStructure() throws Exception {
        GoPluginApiResponse response = new GetClusterProfileMetadataExecutor().execute();

        assertThat(response.responseCode(), is(200));
        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"go_server_url\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Agent TTL minimum (in minutes)\",\n" +
                "    \"default-value\": \"10\",\n" +
                "    \"display-order\": \"1\",\n" +
                "    \"key\": \"auto_register_timeout\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Agent TTL maximum (in minutes)\",\n" +
                "    \"default-value\": \"0\",\n" +
                "    \"display-order\": \"2\",\n" +
                "    \"key\": \"agent_ttl_max\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Default Minimum Instance Limit\",\n" +
                "    \"default-value\": \"1\",\n" +
                "    \"display-order\": \"15\",\n" +
                "    \"key\": \"default_min_instance_limit\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Default Max Instance Limit\",\n" +
                "    \"default-value\": \"10\",\n" +
                "    \"display-order\": \"16\",\n" +
                "    \"key\": \"default_max_instance_limit\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Endpoint\",\n" +
                "    \"display-order\": \"3\",\n" +
                "    \"key\": \"openstack_endpoint\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Keystone Version\",\n" +
                "    \"display-order\": \"4\",\n" +
                "    \"key\": \"openstack_keystone_version\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Domain\",\n" +
                "    \"display-order\": \"5\",\n" +
                "    \"key\": \"openstack_domain\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Tenant\",\n" +
                "    \"display-order\": \"6\",\n" +
                "    \"key\": \"openstack_tenant\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack User\",\n" +
                "    \"display-order\": \"7\",\n" +
                "    \"key\": \"openstack_user\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Password\",\n" +
                "    \"display-order\": \"8\",\n" +
                "    \"key\": \"openstack_password\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": true\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack VM Prefix\",\n" +
                "    \"display-order\": \"9\",\n" +
                "    \"key\": \"openstack_vm_prefix\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Image\",\n" +
                "    \"display-order\": \"10\",\n" +
                "    \"key\": \"openstack_image\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Image Cache TTL (in minutes)\",\n" +
                "    \"default-value\": \"30\",\n" +
                "    \"display-order\": \"11\",\n" +
                "    \"key\": \"openstack_image_cache_ttl\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Allow Use of Previous Openstack Image\",\n" +
                "    \"default-value\": \"false\",\n" +
                "    \"display-order\": \"12\",\n" +
                "    \"key\": \"use_previous_openstack_image\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Flavor\",\n" +
                "    \"display-order\": \"13\",\n" +
                "    \"key\": \"openstack_flavor\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Network\",\n" +
                "    \"display-order\": \"14\",\n" +
                "    \"key\": \"openstack_network\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"OpenStack Userdata\",\n" +
                "    \"display-order\": \"17\",\n" +
                "    \"key\": \"openstack_userdata\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Disable SSL verification\",\n" +
                "    \"default-value\": \"false\",\n" +
                "    \"display-order\": \"18\",\n" +
                "    \"key\": \"ssl_verification_disabled\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Delete error instances\",\n" +
                "    \"default-value\": \"false\",\n" +
                "    \"display-order\": \"19\",\n" +
                "    \"key\": \"delete_error_instances\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"display-name\": \"Agent auto-register Timeout (in minutes)\",\n" +
                "    \"default-value\": \"10\",\n" +
                "    \"display-order\": \"1\",\n" +
                "    \"key\": \"agent_pending_register_timeout\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  }\n" +
                "]";
        System.out.println(response.responseBody());
        JSONAssert.assertEquals(expectedJSON, response.responseBody(), true);
    }
}
