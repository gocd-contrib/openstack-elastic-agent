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

import cd.go.contrib.elasticagents.openstack.requests.ClusterProfileValidateRequest;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Collections;

public class ClusterProfilePropertiesValidateRequestExecutorTest {

    @Test
    public void shouldBarfWhenUnknownKeysArePassed() throws Exception {
        ClusterProfilePropertiesValidateRequestExecutor executor = new ClusterProfilePropertiesValidateRequestExecutor(new ClusterProfileValidateRequest(Collections.singletonMap("foo", "bar")));
        String json = executor.execute().responseBody();
        String expectedStr = "[" +
                "{\"message\":\"Go Server URL must not be blank.\",\"key\":\"go_server_url\"}," +
                "{\"message\":\"Agent TTL minimum (in minutes) must be a positive integer.\",\"key\":\"auto_register_timeout\"}," +
                "{\"message\":\"Agent TTL maximum (in minutes) must be a positive integer.\",\"key\":\"agent_ttl_max\"}," +
                "{\"message\":\"Default Minimum Instance Limit must be a positive integer.\",\"key\":\"default_min_instance_limit\"}," +
                "{\"message\":\"Default Max Instance Limit must be a positive integer.\",\"key\":\"default_max_instance_limit\"}," +
                "{\"message\":\"OpenStack Endpoint must not be blank.\",\"key\":\"openstack_endpoint\"}," +
                "{\"message\":\"OpenStack Keystone Version must not be blank.\",\"key\":\"openstack_keystone_version\"}," +
                "{\"message\":\"OpenStack Domain must not be blank.\",\"key\":\"openstack_domain\"}," +
                "{\"message\":\"OpenStack Tenant must not be blank.\",\"key\":\"openstack_tenant\"}," +
                "{\"message\":\"OpenStack User must not be blank.\",\"key\":\"openstack_user\"}," +
                "{\"message\":\"OpenStack Password must not be blank.\",\"key\":\"openstack_password\"}," +
                "{\"message\":\"OpenStack VM Prefix must not be blank.\",\"key\":\"openstack_vm_prefix\"}," +
                "{\"message\":\"OpenStack Image must not be blank.\",\"key\":\"openstack_image\"}," +
                "{\"message\":\"OpenStack Image Cache TTL (in minutes) must be a positive integer.\",\"key\":\"openstack_image_cache_ttl\"}," +
                "{\"message\":\"Allow Use of Previous Openstack Image must not be blank.\",\"key\":\"use_previous_openstack_image\"}," +
                "{\"message\":\"OpenStack Flavor must not be blank.\",\"key\":\"openstack_flavor\"}," +
                "{\"message\":\"OpenStack Network must not be blank.\",\"key\":\"openstack_network\"}," +
                "{\"message\":\"Disable SSL verification must not be blank.\",\"key\":\"ssl_verification_disabled\"}," +
                "{\"message\":\"Delete error instances must not be blank.\",\"key\":\"delete_error_instances\"}," +
                "{\"message\":\"Agent auto-register Timeout (in minutes) must be a positive integer.\",\"key\":\"agent_pending_register_timeout\"}," +
                "{\"key\":\"foo\",\"message\":\"Is an unknown property\"}" +
                "]";
        JSONAssert.assertEquals(expectedStr, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void shouldValidateMandatoryKeys() throws Exception {
        ClusterProfilePropertiesValidateRequestExecutor executor = new ClusterProfilePropertiesValidateRequestExecutor(new ClusterProfileValidateRequest(Collections.<String, String>emptyMap()));
        String json = executor.execute().responseBody();

        String expectedStr = "[{\"message\":\"Go Server URL must not be blank.\",\"key\":\"go_server_url\"},{\"message\":\"Agent TTL minimum (in minutes) must be a positive integer.\",\"key\":\"auto_register_timeout\"},{\"message\":\"Agent TTL maximum (in minutes) must be a positive integer.\",\"key\":\"agent_ttl_max\"},{\"message\":\"Default Minimum Instance Limit must be a positive integer.\",\"key\":\"default_min_instance_limit\"},{\"message\":\"Default Max Instance Limit must be a positive integer.\",\"key\":\"default_max_instance_limit\"},{\"message\":\"OpenStack Endpoint must not be blank.\",\"key\":\"openstack_endpoint\"},{\"message\":\"OpenStack Keystone Version must not be blank.\",\"key\":\"openstack_keystone_version\"},{\"message\":\"OpenStack Domain must not be blank.\",\"key\":\"openstack_domain\"},{\"message\":\"OpenStack Tenant must not be blank.\",\"key\":\"openstack_tenant\"},{\"message\":\"OpenStack User must not be blank.\",\"key\":\"openstack_user\"},{\"message\":\"OpenStack Password must not be blank.\",\"key\":\"openstack_password\"},{\"message\":\"OpenStack VM Prefix must not be blank.\",\"key\":\"openstack_vm_prefix\"},{\"message\":\"OpenStack Image must not be blank.\",\"key\":\"openstack_image\"},{\"message\":\"OpenStack Image Cache TTL (in minutes) must be a positive integer.\",\"key\":\"openstack_image_cache_ttl\"},{\"message\":\"Allow Use of Previous Openstack Image must not be blank.\",\"key\":\"use_previous_openstack_image\"},{\"message\":\"OpenStack Flavor must not be blank.\",\"key\":\"openstack_flavor\"},{\"message\":\"OpenStack Network must not be blank.\",\"key\":\"openstack_network\"},{\"message\":\"Disable SSL verification must not be blank.\",\"key\":\"ssl_verification_disabled\"},{\"message\":\"Delete error instances must not be blank.\",\"key\":\"delete_error_instances\"},{\"message\":\"Agent auto-register Timeout (in minutes) must be a positive integer.\",\"key\":\"agent_pending_register_timeout\"}]";

        JSONAssert.assertEquals(expectedStr, json, JSONCompareMode.NON_EXTENSIBLE);
    }
}
