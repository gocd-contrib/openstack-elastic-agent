package cd.go.contrib.elasticagents.openstack.utils;

import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cd.go.contrib.elasticagents.openstack.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerHealthMessagesTest {
    private Cache<String, Map<String, String>> cache;

    @BeforeEach
    public void setUp() {
        cache = new Cache2kBuilder<String, Map<String, String>>() {
        }
                .entryCapacity(100)
                .build();
    }

    @Test
    public void shouldAddMessagesGivenIdAndMessage() {
        // Arrange
        ServerHealthMessages healthMessages = new ServerHealthMessages(cache);

        // Act
        healthMessages.add("errortest1", ServerHealthMessages.Type.ERROR, "error test 1");
        healthMessages.add("errortest2", ServerHealthMessages.Type.ERROR, "error test 2");
        healthMessages.add("warningtest1", ServerHealthMessages.Type.WARNING, "warning test 1");

        // Assert
        int expected = 3;
        assertEquals(expected, healthMessages.size());
    }

    @Test
    public void shouldRemoveMessagesGivenId() {

        // Arrange
        ServerHealthMessages healthMessages = new ServerHealthMessages(cache);

        healthMessages.add("errortest1", ServerHealthMessages.Type.ERROR, "error test 1");
        healthMessages.add("errortest2", ServerHealthMessages.Type.ERROR, "error test 2");
        healthMessages.add("warningtest1", ServerHealthMessages.Type.WARNING, "warning test 1");

        // Act
        healthMessages.remove("errortest1");
        healthMessages.remove("errortest1");
        healthMessages.remove("errortest2");

        // Assert
        int expected = 1;
        assertEquals(expected, healthMessages.size());
    }

    @Test
    public void shouldRemoveMessagesWhenCacheExpire() throws InterruptedException {

        // Arrange
        cache = new Cache2kBuilder<String, Map<String, String>>() {
        }
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .entryCapacity(100)
                .build();
        ServerHealthMessages healthMessages = new ServerHealthMessages(cache);

        // Act
        healthMessages.add("errortest1", ServerHealthMessages.Type.ERROR, "error test 1");
        healthMessages.add("errortest2", ServerHealthMessages.Type.ERROR, "error test 2");
        healthMessages.add("warningtest1", ServerHealthMessages.Type.WARNING, "warning test 1");

        // Assert
        int expected = 3;
        assertEquals(expected, healthMessages.size());
        System.out.println("sleep 1100");
        Thread.sleep(1100);
        assertEquals(0, healthMessages.size());
    }

    @Test
    public void shouldCreateTheCorrectDataFormatToSendToServer() {

        // Arrange
        ServerHealthMessages healthMessages = new ServerHealthMessages(cache);

        healthMessages.add("errortest1", ServerHealthMessages.Type.ERROR, "error test 1");
        healthMessages.add("errortest2", ServerHealthMessages.Type.ERROR, "error test 2");
        healthMessages.add("warningtest1", ServerHealthMessages.Type.WARNING, "warning test 1");

        // Act
        final Collection<Map<String, String>> messages = healthMessages.getMessages();

        // Assert
        int expected = 3;
        assertEquals(expected, messages.size());
        messages.forEach(System.out::println);
        messages.forEach(message -> assertTrue(message.get("type").equals("warning") || message.get("type").equals("error"))
        );
    }


    @Test
    public void shouldReturnRequestBodyAsStringJSON() {

        // Arrange
        ServerHealthMessages healthMessages = new ServerHealthMessages(cache);

        healthMessages.add("errortest1", ServerHealthMessages.Type.ERROR, "error test 1");
        healthMessages.add("errortest2", ServerHealthMessages.Type.ERROR, "error test 2");
        healthMessages.add("warningtest1", ServerHealthMessages.Type.WARNING, "warning test 1");

        // Act
        final String requestBody = healthMessages.getJSON();
        System.out.println("[sendServerHealthMessage] requestBody: " + requestBody);
        DefaultGoApiRequest request = new DefaultGoApiRequest(REQUEST_SERVER_SERVER_HEALTH_ADD_MESSAGES, PROCESSOR_API_VERSION, PLUGIN_IDENTIFIER);
        request.setRequestBody(requestBody);
        System.out.println("[sendServerHealthMessage] request.requestBody(): " + request.requestBody());

        // Assert
        assertNotNull(requestBody);
        assertTrue(requestBody.length() > 50);
    }
}