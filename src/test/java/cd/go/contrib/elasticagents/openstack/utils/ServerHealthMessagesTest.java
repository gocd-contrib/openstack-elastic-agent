package cd.go.contrib.elasticagents.openstack.utils;

import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cd.go.contrib.elasticagents.openstack.Constants.*;
import static org.junit.Assert.*;

public class ServerHealthMessagesTest {
    //    private PluginRequest pluginRequest;
    private Cache<String, Map<String, String>> cache;

    @Before
    public void setUp() {
//        pluginRequest = mock(PluginRequest.class);
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
        assertEquals("Should have " + expected, expected, healthMessages.size());
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
        assertEquals("Should have " + expected, expected, healthMessages.size());
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
        assertEquals("Should have " + expected, expected, healthMessages.size());
        System.out.println("sleep 1100");
        Thread.sleep(1100);
        assertEquals("Should have " + 0, 0, healthMessages.size());
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
        assertEquals("Should have " + expected, expected, messages.size());
        messages.stream().forEach(message -> System.out.println(message));
        messages.stream().forEach(message ->
                assertTrue(message.get("type").equals("warning") || message.get("type").equals("error"))
        );
    }


    @Test
    public void shouldReturnRequestBodyAsStringJSON() {

        // Arrange
        ServerHealthMessages healthMessages = new ServerHealthMessages(cache);

        healthMessages.add("errortest1", ServerHealthMessages.Type.ERROR, "error test 1");
        healthMessages.add("errortest2", ServerHealthMessages.Type.ERROR, "error test 2");
        healthMessages.add("warningtest1", ServerHealthMessages.Type.WARNING, "warning test 1");


//        System.out.println("[sendServerHealthMessage]: " +  healthMessages.getMessages().stream()
//                .map(n -> n.toString())
//                .collect(Collectors.joining(","))
//        );

        // Act
        final String requestBody = healthMessages.getJSON();
        System.out.println("[sendServerHealthMessage] requestBody: " + requestBody);
        DefaultGoApiRequest request = new DefaultGoApiRequest(REQUEST_SERVER_SERVER_HEALTH_ADD_MESSAGES, PROCESSOR_API_VERSION, PLUGIN_IDENTIFIER);
        request.setRequestBody(requestBody);
        System.out.println("[sendServerHealthMessage] request.requestBody(): " + request.requestBody());

//
//        messageToBeAdded.put("type", "error");
//        messageToBeAdded.put("message", "error test 1");
//        final String id1 = "errortest1";
//        healthMessages.add(id1, messageToBeAdded);
//        messageToBeAdded.put("type", "error");
//        messageToBeAdded.put("message", "error test 2");
//        healthMessages.add("errortest2", messageToBeAdded);
//
        // Assert
        assertNotNull(requestBody);
        assertTrue(requestBody.length() > 50);
//        int expected = 2;
//        assertEquals("Should have " + expected, expected, healthMessages.size());
//        verify(pluginRequest, never()).sendServerHealthMessage(anyCollection());
//        healthMessages.send();
//        verify(pluginRequest, times(1)).sendServerHealthMessage(anyCollection());
    }
}