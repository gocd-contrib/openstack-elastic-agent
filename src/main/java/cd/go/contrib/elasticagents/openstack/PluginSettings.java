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

package cd.go.contrib.elasticagents.openstack;


import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

public class PluginSettings {
    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .excludeFieldsWithoutExposeAnnotation()
            .create();


    @Expose
    @SerializedName("openstack_endpoint")
    private String openstackEndpoint;

    @Expose
    @SerializedName("openstack_domain")
    private String openstackDomain;

    @Expose
    @SerializedName("openstack_tenant")
    private String openstackTenant;

    @Expose
    @SerializedName("openstack_vm_prefix")
    private String openstackVmPrefix;

    @Expose
    @SerializedName("openstack_user")
    private String openstackUser;

    @Expose
    @SerializedName("openstack_password")
    private String openstackPassword;

    @Expose
    @SerializedName("go_server_url")
    private String goServerUrl;

    @Expose
    @SerializedName("agent_pending_register_timeout")
    private String agentPendingRegisterTimeout;

    @Expose
    @SerializedName("openstack_keystone_version")
    private String openstackKeystoneVersion;

    @Expose
    @SerializedName("auto_register_timeout")
    private String agentTTLMin;

    @Expose
    @SerializedName("agent_ttl_max")
    private String agentTTLMax;

    @Expose
    @SerializedName("default_min_instance_limit")
    private String defaultMinInstanceLimit;

    @Expose
    @SerializedName("default_max_instance_limit")
    private String defaultMaxInstanceLimit;

    @Expose
    @SerializedName("openstack_image")
    private String openstackImage;

    @Expose
    @SerializedName("openstack_image_cache_ttl")
    private String openstackImageCacheTTL;

    @Expose
    @SerializedName("use_previous_openstack_image")
    private Boolean usePreviousOpenstackImage;

    @Expose
    @SerializedName("openstack_flavor")
    private String openstackFlavor;

    @Expose
    @SerializedName("openstack_network")
    private String openstackNetwork;

    @Expose
    @SerializedName("openstack_userdata")
    private String openstackUserdata;

    @Expose
    @SerializedName("ssl_verification_disabled")
    private Boolean sslVerificationDisabled;

    @Expose
    @SerializedName("delete_error_instances")
    private Boolean deleteErrorInstances;

    private Duration agentRegisterPeriod;
    private Duration agentTTLMinPeriod;

    public static PluginSettings fromJSON(String json) {
        return GSON.fromJson(json, PluginSettings.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginSettings)) return false;

        PluginSettings that = (PluginSettings) o;

        if (openstackEndpoint != null ? !openstackEndpoint.equals(that.openstackEndpoint) : that.openstackEndpoint != null)
            return false;
        if (openstackDomain != null ? !openstackDomain.equals(that.openstackDomain) : that.openstackDomain != null)
            return false;
        if (openstackTenant != null ? !openstackTenant.equals(that.openstackTenant) : that.openstackTenant != null)
            return false;
        if (openstackVmPrefix != null ? !openstackVmPrefix.equals(that.openstackVmPrefix) : that.openstackVmPrefix != null)
            return false;
        return openstackUser != null ? openstackUser.equals(that.openstackUser) : that.openstackUser == null;
    }

    @Override
    public int hashCode() {
        int result = openstackEndpoint != null ? openstackEndpoint.hashCode() : 0;
        result = 31 * result + (openstackDomain != null ? openstackDomain.hashCode() : 0);
        result = 31 * result + (openstackTenant != null ? openstackTenant.hashCode() : 0);
        result = 31 * result + (openstackVmPrefix != null ? openstackVmPrefix.hashCode() : 0);
        result = 31 * result + (openstackUser != null ? openstackUser.hashCode() : 0);
        return result;
    }

    public String uuid() {
        return String.valueOf(hashCode());
    }

    public Duration getAgentPendingRegisterPeriod() {
        if (this.agentRegisterPeriod == null) {
            this.agentRegisterPeriod = Duration.ofMinutes(Integer.parseInt(getAgentPendingRegisterTimeout()));
        }
        return this.agentRegisterPeriod;
    }

    private String getAgentPendingRegisterTimeout() {
        if (agentPendingRegisterTimeout == null) {
            agentPendingRegisterTimeout = "10";
        }
        return agentPendingRegisterTimeout;
    }

    public Duration getAgentTTLMinPeriod() {
        if (this.agentTTLMinPeriod == null) {
            this.agentTTLMinPeriod = Duration.ofMinutes(Integer.parseInt(getAgentTTLMin()));
        }
        return this.agentTTLMinPeriod;
    }

    public String getAgentTTLMin() {
        if (agentTTLMin == null) {
            agentTTLMin = "10";
        }
        return agentTTLMin;
    }

    public String getDefaultMinInstanceLimit() {
        if (defaultMinInstanceLimit == null) {
            defaultMinInstanceLimit = "1";
        }
        return defaultMinInstanceLimit;
    }

    public int getAgentTTLMax() {
        int result;
        try {
            result = Integer.parseInt(agentTTLMax);
        } catch (NumberFormatException nfe) {
            result = 0;
        }
        return result;
    }

    public String getDefaultMaxInstanceLimit() {
        if (defaultMaxInstanceLimit == null) {
            defaultMaxInstanceLimit = "10";
        }
        return defaultMaxInstanceLimit;
    }

    public String getGoServerUrl() {
        return goServerUrl;
    }

    public String getOpenstackEndpoint() {
        return openstackEndpoint;
    }

    public String getOpenstackKeystoneVersion() {
        openstackKeystoneVersion = (openstackKeystoneVersion == null) ? "2" : openstackKeystoneVersion;
        return openstackKeystoneVersion;
    }

    public String getOpenstackTenant() {
        return openstackTenant;
    }

    public String getOpenstackDomain() {
        openstackDomain = (openstackDomain == null) ? "Default" : openstackDomain;
        return openstackDomain;
    }

    public String getOpenstackUser() {
        return openstackUser;
    }

    public String getOpenstackPassword() {
        return openstackPassword;
    }

    public String getOpenstackVmPrefix() {
        return openstackVmPrefix;
    }

    public String getOpenstackImage() {
        return openstackImage;
    }

    public String getOpenstackImageCacheTTL() {
        return openstackImageCacheTTL;
    }

    public Boolean getUsePreviousOpenstackImage() {
        usePreviousOpenstackImage = (usePreviousOpenstackImage == null) ? Boolean.FALSE : usePreviousOpenstackImage;
        return usePreviousOpenstackImage;
    }

    public String getOpenstackFlavor() {
        return openstackFlavor;
    }

    public String getOpenstackNetwork() {
        return openstackNetwork;
    }

    public String getOpenstackUserdata() {
        return StringUtils.isBlank(openstackUserdata) ? null : openstackUserdata;
    }

    public void setOpenstackUserdata(String openstackUserdata) {
        this.openstackUserdata = openstackUserdata;
    }

    public Boolean getSSLVerificationDisabled() {
        sslVerificationDisabled = (sslVerificationDisabled == null) ? Boolean.FALSE : sslVerificationDisabled;
        return sslVerificationDisabled;
    }

    public Boolean getOpenstackDeleteErrorInstances() {
        if (deleteErrorInstances == null)
            return false;
        return deleteErrorInstances;
    }

    public void setDeleteErrorInstances(boolean deleteErrorInstances) {
        this.deleteErrorInstances = deleteErrorInstances;
    }

    @Override
    public String toString() {
        return "PluginSettings{" +
                "openstackEndpoint='" + openstackEndpoint + '\'' +
                ", openstackDomain='" + openstackDomain + '\'' +
                ", openstackTenant='" + openstackTenant + '\'' +
                ", openstackVmPrefix='" + openstackVmPrefix + '\'' +
                ", openstackUser='" + openstackUser + '\'' +
                ", openstackPassword='" + openstackPassword + '\'' +
                ", goServerUrl='" + goServerUrl + '\'' +
                ", agentPendingRegisterTimeout='" + agentPendingRegisterTimeout + '\'' +
                ", openstackKeystoneVersion='" + openstackKeystoneVersion + '\'' +
                ", agentTTLMin='" + agentTTLMin + '\'' +
                ", agentTTLMax='" + agentTTLMax + '\'' +
                ", defaultMinInstanceLimit='" + defaultMinInstanceLimit + '\'' +
                ", defaultMaxInstanceLimit='" + defaultMaxInstanceLimit + '\'' +
                ", openstackImage='" + openstackImage + '\'' +
                ", openstackImageCacheTTL='" + openstackImageCacheTTL + '\'' +
                ", usePreviousOpenstackImage=" + usePreviousOpenstackImage +
                ", openstackFlavor='" + openstackFlavor + '\'' +
                ", openstackNetwork='" + openstackNetwork + '\'' +
                ", openstackUserdata='" + openstackUserdata + '\'' +
                ", sslVerificationDisabled=" + sslVerificationDisabled +
                ", deleteErrorInstances=" + deleteErrorInstances +
                ", agentRegisterPeriod=" + agentRegisterPeriod +
                ", agentTTLMinPeriod=" + agentTTLMinPeriod +
                '}';
    }
}
