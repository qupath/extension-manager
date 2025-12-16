package qupath.ext.extensionmanager;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.tools.TestFilesWatcher;

import java.util.Collection;

/**
 * Adds some utility functions for testing.
 */
public class TestUtils {

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);
    private static final long TIME_BETWEEN_ATTEMPTS_MS = 200;
    private TestUtils() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Assert that two lists are equal without taking the order
     * of elements into account.
     * This function doesn't work if some duplicates are present in one
     * of the list.
     *
     * @param expectedCollection the expected values
     * @param actualCollection the actual values
     * @param <T> the type of the elements of the collection
     */
    public static <T> void assertCollectionsEqualsWithoutOrder(
            Collection<? extends T> expectedCollection,
            Collection<? extends T> actualCollection
    ) {
        Assertions.assertEquals(expectedCollection.size(), actualCollection.size());

        Assertions.assertTrue(expectedCollection.containsAll(actualCollection));
        Assertions.assertTrue(actualCollection.containsAll(expectedCollection));
    }

    public static <T> void assertCollectionsEqualsWithoutOrder(Collection<? extends T> expectedCollection, Collection<? extends T> actualCollection, int waitingTime) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long time = System.currentTimeMillis();

        while (time < startTime + waitingTime) {
            try {
                assertCollectionsEqualsWithoutOrder(expectedCollection, actualCollection);
                return;
            } catch (Throwable e) {
                logger.debug(
                        "Expected: {} and actual: {} are not equal. Waiting {} and attempting to compare again",
                        expectedCollection,
                        actualCollection,
                        TIME_BETWEEN_ATTEMPTS_MS
                );

                Thread.sleep(TIME_BETWEEN_ATTEMPTS_MS);

                time = System.currentTimeMillis();
            }
        }

        assertCollectionsEqualsWithoutOrder(expectedCollection, actualCollection);
    }
}
