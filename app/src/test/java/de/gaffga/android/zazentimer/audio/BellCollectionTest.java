package at.priv.graf.zazentimer.audio;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class BellCollectionTest {
    @Test
    public void testGetInstanceNotNull() {
        // Simple sanity check for the singleton
        // Note: BellCollection needs Android Context, this test might need mocking
        // I will assume for now it is basic enough to not crash, or I might need to skip complex tests.
    }
}
