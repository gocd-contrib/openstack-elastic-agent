package cd.go.contrib.elasticagents.openstack;


import com.thoughtworks.go.plugin.api.response.GoApiResponse;

import static java.lang.String.format;

public class ServerRequestFailedException extends Exception {

    private ServerRequestFailedException(GoApiResponse response, String request) {
        super(format(
                "The server sent an unexpected status code %d with the response body %s when it was sent a %s message",
                response.responseCode(), response.responseBody(), request
        ));
    }

    public static ServerRequestFailedException disableAgents(GoApiResponse response) {
        return new ServerRequestFailedException(response, "disable agents");
    }

    public static ServerRequestFailedException deleteAgents(GoApiResponse response) {
        return new ServerRequestFailedException(response, "delete agents");
    }

    public static ServerRequestFailedException listAgents(GoApiResponse response) {
        return new ServerRequestFailedException(response, "list agents");
    }

    public static ServerRequestFailedException getPluginSettings(GoApiResponse response) {
        return new ServerRequestFailedException(response, "get plugin settings");
    }
}

