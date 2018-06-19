package cd.go.contrib.elasticagents.openstack.utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UtilTest {

    @Test
    public void testRandom() throws Exception {
        // Arrange
        int max = 5;
        int min = -5;

        // Act

        // Assert
        boolean minusValueFound = false;
        for (int i = 0; i < 100; i++) {
            int value = Util.randomPlusMinus(max);
            System.out.println(value);
            assertTrue("Should be max " + max, max >= value);
            assertTrue("Should be min " + min, min <= value);
            if (value < 0) {
                minusValueFound = true;
            }
        }
        assertTrue("Should find at least one minus value", minusValueFound);

    }

    @Test
    public void testRandomZero() throws Exception {
        // Arrange
        int max = 0;
        int min = 0;

        // Act

        // Assert
        for (int i = 0; i < 100; i++) {
            int value = Util.randomPlusMinus(max);
            System.out.println(value);
            assertTrue("Should be max " + max, max >= value);
            assertTrue("Should be min " + min, min <= value);
        }
    }
}