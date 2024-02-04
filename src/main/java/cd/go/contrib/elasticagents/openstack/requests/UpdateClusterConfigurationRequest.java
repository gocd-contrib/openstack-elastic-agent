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

package cd.go.contrib.elasticagents.openstack.requests;

import cd.go.contrib.elasticagents.openstack.client.OpenStackInstances;
import cd.go.contrib.elasticagents.openstack.executors.UpdateClusterConfigurationExecutor;
import cd.go.contrib.elasticagents.openstack.model.ClusterProfileProperties;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class UpdateClusterConfigurationRequest {
    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Expose
    @SerializedName("status")
    private String status;

    @Expose
    @SerializedName("old_cluster_profiles_properties")
    private ClusterProfileProperties oldClusterProfile;

    @Expose
    @SerializedName("cluster_profiles_properties")
    private ClusterProfileProperties newClusterProfile;

    public UpdateClusterConfigurationRequest() {
    }

    public UpdateClusterConfigurationRequest(String status, ClusterProfileProperties oldClusterProfile, ClusterProfileProperties newClusterProfile) {
        this.status = status;
        this.oldClusterProfile = oldClusterProfile;
        this.newClusterProfile = newClusterProfile;
    }

    public static UpdateClusterConfigurationRequest fromJSON(String requestBody) {
        UpdateClusterConfigurationRequest request = GSON.fromJson(requestBody, UpdateClusterConfigurationRequest.class);
        return request;
    }

    public String toJSON() {
        return GSON.toJson(this);
    }

    public UpdateClusterConfigurationExecutor executor(OpenStackInstances pluginSettings) {
        return new UpdateClusterConfigurationExecutor(this, pluginSettings);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ClusterProfileProperties getOldClusterProfile() {
        return oldClusterProfile;
    }

    public void setOldClusterProfile(ClusterProfileProperties oldClusterProfile) {
        this.oldClusterProfile = oldClusterProfile;
    }

    public ClusterProfileProperties getNewClusterProfile() {
        return newClusterProfile;
    }

    public void setNewClusterProfile(ClusterProfileProperties newClusterProfile) {
        this.newClusterProfile = newClusterProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateClusterConfigurationRequest that = (UpdateClusterConfigurationRequest) o;
        return Objects.equals(status, that.status) &&
                Objects.equals(oldClusterProfile, that.oldClusterProfile) &&
                Objects.equals(newClusterProfile, that.newClusterProfile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, oldClusterProfile, newClusterProfile);
    }

    @Override
    public String toString() {
        return "MigrateConfigurationRequest{" +
                "status=" + status +
                ", clusterProfiles=" + oldClusterProfile +
                ", elasticAgentProfiles=" + newClusterProfile +
                '}';
    }
}
