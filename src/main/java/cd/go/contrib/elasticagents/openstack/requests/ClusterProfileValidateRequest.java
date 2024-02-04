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

package cd.go.contrib.elasticagents.openstack.requests;

import cd.go.contrib.elasticagents.openstack.RequestExecutor;
import cd.go.contrib.elasticagents.openstack.executors.ClusterProfilePropertiesValidateRequestExecutor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class ClusterProfileValidateRequest {

    private static final Gson GSON = new Gson();
    private Map<String, String> properties;

    public ClusterProfileValidateRequest(Map<String, String> properties) {
        this.properties = properties;
    }

    public static ClusterProfileValidateRequest fromJSON(String json) {
        final Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        final Map<String, String> properties = GSON.fromJson(json, type);
        return new ClusterProfileValidateRequest(properties);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public RequestExecutor executor() {
        return new ClusterProfilePropertiesValidateRequestExecutor(this);
    }
}
