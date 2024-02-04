/*
 * Copyright 2016 Thoughtworks, Inc.
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

import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.ArrayList;
import java.util.List;

public class GetClusterProfileMetadataExecutor implements RequestExecutor {
    public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static final Metadata GO_SERVER_URL = new GoServerURLMetadata();
    public static final Field AGENT_TTL_MIN = new PositiveNumberField("auto_register_timeout", "Agent TTL minimum (in minutes)", "10", true, false, "1");
    public static final Field AGENT_TTL_MAX = new PositiveNumberField("agent_ttl_max", "Agent TTL maximum (in minutes)", "0", true,
            false, "2");
    public static final Field OPENSTACK_ENDPOINT = new NonBlankField("openstack_endpoint", "OpenStack Endpoint", null, true, false, "3");
    public static final Field OPENSTACK_KEYSTONE_VERSION = new NonBlankField("openstack_keystone_version", "OpenStack Keystone Version", null, true, false, "4");
    public static final Field OPENSTACK_DOMAIN = new NonBlankField("openstack_domain", "OpenStack Domain", null, true, false, "5");
    public static final Field OPENSTACK_TENANT = new NonBlankField("openstack_tenant", "OpenStack Tenant", null, true, false, "6");
    public static final Field OPENSTACK_USER = new NonBlankField("openstack_user", "OpenStack User", null, true, false, "7");
    public static final Field OPENSTACK_PASSWORD = new NonBlankField("openstack_password", "OpenStack Password", null, true, true, "8");
    public static final Field OPENSTACK_VM_PREFIX = new NonBlankField("openstack_vm_prefix", "OpenStack VM Prefix", null, true, false, "9");
    public static final Field OPENSTACK_IMAGE = new NonBlankField("openstack_image", "OpenStack Image", null, true, false, "10");
    public static final Field OPENSTACK_IMAGE_CACHE_TTL = new PositiveNumberField("openstack_image_cache_ttl", "OpenStack Image Cache TTL (in minutes)", "30", true, false, "11");
    public static final Field USE_PREVIOUS_OPENSTACK_IMAGE = new NonBlankField("use_previous_openstack_image", "Allow Use of Previous Openstack Image", "false", true, false, "12");
    public static final Field OPENSTACK_FLAVOR = new NonBlankField("openstack_flavor", "OpenStack Flavor", null, true, false, "13");
    public static final Field OPENSTACK_NETWORK = new NonBlankField("openstack_network", "OpenStack Network", null, true, false, "14");
    public static final Field DEFAULT_MIN_INSTANCE_LIMIT = new PositiveNumberField("default_min_instance_limit", "Default Minimum Instance Limit", "1", true,
            false, "15");
    public static final Field DEFAULT_MAX_INSTANCE_LIMIT = new PositiveNumberField("default_max_instance_limit", "Default Max Instance Limit", "10", true,
            false, "16");
    public static final Field OPENSTACK_USERDATA = new Field("openstack_userdata", "OpenStack Userdata", null, false, false, "17");
    public static final Field SSL_VERIFICATION_DISABLED = new NonBlankField("ssl_verification_disabled", "Disable SSL verification", "false", true, false, "18");
    public static final Field DELETE_ERROR_INSTANCES = new NonBlankField("delete_error_instances", "Delete error instances", "false", false, false, "19");
    public static final Field AGENT_PENDING_REGISTER_TIMEOUT = new PositiveNumberField("agent_pending_register_timeout", "Agent auto-register Timeout (in minutes)", "10", true, false, "1");

    //public static final Field AGENT_RESOURCES = new Field("resources", "Agent Resources", null, false, false, "11");
    //public static final Field AGENT_ENVIRONMENTS = new Field("environments", "Environments", null, false, false, "12");

    public static final List<Metadata> FIELDS = new ArrayList<>();

    static {
        FIELDS.add(GO_SERVER_URL);
        FIELDS.add(AGENT_TTL_MIN);
        FIELDS.add(AGENT_TTL_MAX);
        FIELDS.add(DEFAULT_MIN_INSTANCE_LIMIT);
        FIELDS.add(DEFAULT_MAX_INSTANCE_LIMIT);
        FIELDS.add(OPENSTACK_ENDPOINT);
        FIELDS.add(OPENSTACK_KEYSTONE_VERSION);
        FIELDS.add(OPENSTACK_DOMAIN);
        FIELDS.add(OPENSTACK_TENANT);
        FIELDS.add(OPENSTACK_USER);
        FIELDS.add(OPENSTACK_PASSWORD);
        FIELDS.add(OPENSTACK_VM_PREFIX);
        FIELDS.add(OPENSTACK_IMAGE);
        FIELDS.add(OPENSTACK_IMAGE_CACHE_TTL);
        FIELDS.add(USE_PREVIOUS_OPENSTACK_IMAGE);
        FIELDS.add(OPENSTACK_FLAVOR);
        FIELDS.add(OPENSTACK_NETWORK);
        FIELDS.add(OPENSTACK_USERDATA);
        FIELDS.add(SSL_VERIFICATION_DISABLED);
        FIELDS.add(DELETE_ERROR_INSTANCES);
        FIELDS.add(AGENT_PENDING_REGISTER_TIMEOUT);

        // agent spec
        //FIELDS.add( AGENT_RESOURCES);
        //FIELDS.add( AGENT_ENVIRONMENTS);
    }

    public GoPluginApiResponse execute() {
        return new DefaultGoPluginApiResponse(200, GSON.toJson(FIELDS));
    }
}
