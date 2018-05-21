package cd.go.contrib.elasticagents.openstack.executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

public class GetCapabilitiesExecutor {
    public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    private static final boolean SUPPORTS_STATUS_REPORT = false;
    private static final boolean SUPPORTS_AGENT_STATUS_REPORT = false;

    public GoPluginApiResponse execute() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("supports_status_report", SUPPORTS_STATUS_REPORT);
        jsonObject.addProperty("supports_agent_status_report", SUPPORTS_AGENT_STATUS_REPORT);
        return DefaultGoPluginApiResponse.success(GSON.toJson(jsonObject));
    }
}
