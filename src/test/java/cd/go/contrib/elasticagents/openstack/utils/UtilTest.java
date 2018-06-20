package cd.go.contrib.elasticagents.openstack.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtilTest {

    @Test
    public void testShouldReturnTimeToLiveBetween10And20Minutes() {
        // Arrange
        int min = 10;
        int max = 20;

        // Assert
        for (int i = 0; i < 100; i++) {
            int value = Util.calculateTTL(min, max);
            System.out.println(value);
            assertTrue("Should be max " + max, max >= value);
            assertTrue("Should be min " + min, min <= value);
        }
    }

    @Test
    public void testShouldReturnTimeToLiveNoLessThanMinimum() {

        // Assert
        assertEquals("Should be no less than minimum", 10, Util.calculateTTL(10, 0));
        assertEquals("Should be no less than minimum", 10, Util.calculateTTL(10, 5));
        assertEquals("Should be no less than minimum", 10, Util.calculateTTL(10, 10));
        assertEquals("Should be no less than minimum", 10, Util.calculateTTL(10, -11));
    }
}