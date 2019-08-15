package cd.go.contrib.elasticagents.openstack.utils;

import com.google.gson.Gson;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ServerHealthMessages {
    private final Cache<String, Map<String, String>> massageCache;

    public ServerHealthMessages() {
        massageCache = new Cache2kBuilder<String, Map<String, String>>() {
        }.expireAfterWrite(2, TimeUnit.MINUTES).entryCapacity(100).build();
    }

    public ServerHealthMessages(Cache<String, Map<String, String>> massageCache) {
        this.massageCache = massageCache;
    }

    public void add(String id, Type type, String message) {
        Map<String, String> messageToBeAdded = new HashMap<>();
        messageToBeAdded.put("type", type.toString().toLowerCase());
        messageToBeAdded.put("message", message);
        massageCache.put(id, messageToBeAdded);
    }

    public void remove(String id) {
        massageCache.remove(id);
    }

    public int size() {
        return massageCache.asMap().size();
    }
//
//    public void send() {
//        pluginRequest.sendServerHealthMessage(serverMessages.asMap().values());
//    }

    Collection<Map<String, String>> getMessages() {
        return massageCache.asMap().values();
    }

    public String getJSON() {
        Gson gson = new Gson();
        return gson.toJson(getMessages(), List.class);
    }

    public enum Type {
        ERROR, WARNING
    }
}
