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

package cd.go.contrib.elasticagents.openstack.model;

import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class GoServerURLMetadata extends Metadata {
    private static String GO_SERVER_URL = "go_server_url";
    private static String GO_SERVER_URL_DISPLAY_VALUE = "Go Server URL";

    public GoServerURLMetadata() {
        super(GO_SERVER_URL, true, false);
    }

    @Override
    public String doValidate(String input) {
        if (isBlank(input)) {
            return GO_SERVER_URL_DISPLAY_VALUE + " must not be blank.";
        }

        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(input);
        } catch (URISyntaxException e) {
            return GO_SERVER_URL_DISPLAY_VALUE + " must be a valid URL (https://example.com:8154/go)";
        }

        if (isBlank(uriBuilder.getScheme())) {
            return GO_SERVER_URL_DISPLAY_VALUE + " must be a valid URL (https://example.com:8154/go)";
        }

        if (!uriBuilder.getScheme().equalsIgnoreCase("https")) {
            return GO_SERVER_URL_DISPLAY_VALUE + " must be a valid HTTPs URL (https://example.com:8154/go)";
        }

        if (uriBuilder.getHost().equalsIgnoreCase("localhost") || uriBuilder.getHost().equalsIgnoreCase("127.0.0.1")) {
            return GO_SERVER_URL_DISPLAY_VALUE + " must not be localhost, since this gets resolved on the agents";
        }

        if (!(uriBuilder.getPath().endsWith("/go") || uriBuilder.getPath().endsWith("/go/"))) {
            return GO_SERVER_URL_DISPLAY_VALUE + " must be a valid URL ending with '/go' (https://example.com:8154/go)";
        }

        return null;
    }
}
