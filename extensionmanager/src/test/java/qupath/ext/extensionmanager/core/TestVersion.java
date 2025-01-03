package qupath.ext.extensionmanager.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestVersion {

    @Test
    void Check_Null_Version() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> new Version(null)
        );
    }

    @Test
    void Check_Invalid_Version() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Version("invalid_version")
        );
    }

    @Test
    void Check_Version_Without_V() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Version("1.2.3")
        );
    }

    @Test
    void Check_Valid_Version() {
        Assertions.assertDoesNotThrow(() -> new Version("v1.2.3"));
    }

    @Test
    void Check_Valid_Version_With_Release_Candidate() {
        Assertions.assertDoesNotThrow(() -> new Version("v1.2.3-rc4"));
    }

    @Test
    void Check_Valid_Version_With_Snapshot() {
        Assertions.assertDoesNotThrow(() -> new Version("v1.2.3-SNAPSHOT"));
    }

    @Test
    void Check_Version_Inequality() {
        Version lowerVersion = new Version("v1.2.3");
        Version upperVersion = new Version("v2.3.4");

        int comparison = lowerVersion.compareTo(upperVersion);

        Assertions.assertTrue(comparison < 0);
    }

    @Test
    void Check_Version_Inequality_With_Same_Major() {
        Version lowerVersion = new Version("v1.2.3");
        Version upperVersion = new Version("v1.3.4");

        int comparison = lowerVersion.compareTo(upperVersion);

        Assertions.assertTrue(comparison < 0);
    }

    @Test
    void Check_Version_Inequality_With_Same_Major_And_Minor() {
        Version lowerVersion = new Version("v1.2.3");
        Version upperVersion = new Version("v1.2.4");

        int comparison = lowerVersion.compareTo(upperVersion);

        Assertions.assertTrue(comparison < 0);
    }

    @Test
    void Check_Version_Inequality_With_Same_Major_Minor_And_Patch() {
        Version lowerVersion = new Version("v1.2.4");
        Version upperVersion = new Version("v1.2.4");

        int comparison = lowerVersion.compareTo(upperVersion);

        Assertions.assertEquals(0, comparison);
    }

    @Test
    void Check_Version_Inequality_With_Same_Major_Minor_And_Patch_And_Different_Release_Candidate() {
        Version lowerVersion = new Version("v1.2.4-rc3");
        Version upperVersion = new Version("v1.2.4-rc4");

        int comparison = lowerVersion.compareTo(upperVersion);

        Assertions.assertTrue(comparison < 0);
    }

    @Test
    void Check_Version_Inequality_With_Same_Major_Minor_And_Patch_And_Snapshot() {
        Version lowerVersion = new Version("v1.2.4");
        Version upperVersion = new Version("v1.2.4-SNAPSHOT");

        int comparison = lowerVersion.compareTo(upperVersion);

        Assertions.assertEquals(0, comparison);     // see the description of the Version class to know why they are equal
    }
}
