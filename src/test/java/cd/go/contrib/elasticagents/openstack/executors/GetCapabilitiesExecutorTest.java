package cd.go.contrib.elasticagents.openstack.executors;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class GetCapabilitiesExecutorTest {
    private static final Gson GSON = new Gson();
    private GetCapabilitiesExecutor executor;

    @Before
    public void SetUp() {
        executor = new GetCapabilitiesExecutor();
    }


    @Test
    public void getCapabilitiesShouldReturnSupportsFields() throws Exception {
        GoPluginApiResponse response = executor.execute();
        Map<String, String> responseJson = (Map<String, String>) GSON.fromJson(response.responseBody(), HashMap.class);

        assertThat(responseJson.containsKey("supports_status_report"), is(true));
        assertThat(responseJson.containsKey("supports_agent_status_report"), is(true));
    }

}