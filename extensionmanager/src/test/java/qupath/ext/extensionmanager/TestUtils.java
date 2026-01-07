package qupath.ext.extensionmanager;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Assert that two collections are equal without taking the order of elements into account.
     * <p>
     * Warning: this function doesn't work if some duplicates are present in one of the collection.
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

    /**
     * Assert that two collections are equal without taking the order of elements into account. This method will retry the
     * assertion for a specified waiting period if the collections are not immediately equal.
     * <p>
     * Warning: this function doesn't work if some duplicates are present in one of the collection.
     *
     * @param expectedCollection the expected values
     * @param actualCollection the actual values
     * @param waitingTimeMs the maximum time (in milliseconds) to wait for the collections to become equal
     * @param <T> the type of the elements of the collection
     * @throws InterruptedException if the calling thread is interrupted
     */
    public static <T> void assertCollectionsEqualsWithoutOrder(
            Collection<? extends T> expectedCollection,
            Collection<? extends T> actualCollection,
            int waitingTimeMs
    ) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long time = System.currentTimeMillis();

        while (time < startTime + waitingTimeMs) {
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
