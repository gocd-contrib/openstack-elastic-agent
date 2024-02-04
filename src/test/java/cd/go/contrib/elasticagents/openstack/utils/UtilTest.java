package cd.go.contrib.elasticagents.openstack.utils;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilTest {

    @Test
    public void shouldGetPluginId() {
        assertEquals("cd.go.contrib.elastic-agent.openstack", Util.pluginId());
    }

    @Test
    public void testShouldReturnTimeToLiveBetween10And20Minutes() {
        // Arrange
        int min = 10;
        int max = 20;

        // Assert
        for (int i = 0; i < 100; i++) {
            int value = Util.calculateTTL(min, max);
            System.out.println(value);
            assertTrue(max >= value, "Should be max " + max);
            assertTrue(min <= value, "Should be min " + min);
        }
    }

    @Test
    public void testShouldReturnTimeToLiveNoLessThanMinimum() {

        assertEquals(10, Util.calculateTTL(10, 0));
        assertEquals(10, Util.calculateTTL(10, 5));
        assertEquals(10, Util.calculateTTL(10, 10));
        assertEquals(10, Util.calculateTTL(10, -11));
    }
}