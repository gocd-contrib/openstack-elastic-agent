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

package cd.go.contrib.elasticagents.openstack.executors;

import cd.go.contrib.elasticagents.openstack.Constants;
import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.ArrayList;
import java.util.List;

public class GetProfileMetadataExecutor implements RequestExecutor {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static final Metadata OPENSTACK_IMAGE_ID = new Metadata(Constants.OPENSTACK_IMAGE_ID_ARGS, true, false);
    public static final Metadata OPENSTACK_FLAVOR_ID = new Metadata(Constants.OPENSTACK_FLAVOR_ID_ARGS, true, false);
    public static final Metadata OPENSTACK_NETWORK_ID = new Metadata(Constants.OPENSTACK_NETWORK_ID_ARGS, true, false);
    public static final Metadata OPENSTACK_SECURITY_GROUP = new Metadata(Constants.OPENSTACK_SECURITY_GROUP, false, false);
    public static final Metadata OPENSTACK_KEYPAIR = new Metadata(Constants.OPENSTACK_KEYPAIR, false, false);
    public static final Metadata OPENSTACK_USERDATA = new Metadata(Constants.OPENSTACK_USERDATA_ARGS, false, false);

    public static final List<Metadata> FIELDS = new ArrayList<>();

    static {
        FIELDS.add(OPENSTACK_IMAGE_ID);
        FIELDS.add(OPENSTACK_FLAVOR_ID);
        FIELDS.add(OPENSTACK_NETWORK_ID);
        FIELDS.add(OPENSTACK_SECURITY_GROUP);
        FIELDS.add(OPENSTACK_KEYPAIR);
        FIELDS.add(OPENSTACK_USERDATA);
    }

    @Override

    public GoPluginApiResponse execute() throws Exception {
        return new DefaultGoPluginApiResponse(200, GSON.toJson(FIELDS));
    }
}