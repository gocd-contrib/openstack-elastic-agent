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

package cd.go.contrib.elasticagents.openstack.model;

import cd.go.contrib.elasticagents.openstack.PluginSettings;
import cd.go.contrib.elasticagents.openstack.requests.ClusterProfileValidateRequest;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Map;
import java.util.Objects;

public class ClusterProfileProperties extends PluginSettings {
    public static ClusterProfileProperties fromJSON(String json) {
        return GSON.fromJson(json, ClusterProfileProperties.class);
    }

    public static ClusterProfileProperties fromConfiguration(Map<String, String> clusterProfileProperties) {
        return GSON.fromJson(GSON.toJson(clusterProfileProperties), ClusterProfileProperties.class);
    }

    public String uuid() {
        return Integer.toHexString(Objects.hash(this));
    }

    public String toJson() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setPrettyPrinting()
                .create();
        return gson.toJson(this);
    }

    public void validate(String message) throws Exception {
        final GoPluginApiResponse response = ClusterProfileValidateRequest.fromJSON(toJson()).executor().execute();
        if (!"[]".equals(response.responseBody()))
            throw new IllegalArgumentException(message + ": " + response.responseBody());
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
