package qupath.ext.extensionmanager.core.catalog;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestVersionRange {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Version_Range() {
            Assertions.assertDoesNotThrow(() -> new VersionRange(
                    "v1.1.0",
                    null,
                    null
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRange(
                            null,
                            null,
                            null
                    )
            );
        }

        @Test
        void Check_Valid_With_Max() {
            Assertions.assertDoesNotThrow(() -> new VersionRange(
                    "v1.1.0",
                    "v2.0.0",
                    null
            ));
        }

        @Test
        void Check_Invalid_When_Max_Lower_Than_Min() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRange(
                            "v2.0.0",
                            "v1.1.0",
                            null
                    )
            );
        }

        @Test
        void Check_Valid_With_Excluded() {
            Assertions.assertDoesNotThrow(() -> new VersionRange(
                    "v1.1.0",
                    null,
                    List.of("v1.3.0", "v2.0.0")
            ));
        }

        @Test
        void Check_Invalid_When_Excluded_Lower_Than_Min() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRange(
                            "v2.0.0",
                            null,
                            List.of("v1.3.0", "v2.0.0")
                    )
            );
        }

        @Test
        void Check_Invalid_When_Excluded_Higher_Than_Max() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRange(
                            "v1.0.0",
                            "v1.1.0",
                            List.of("v1.3.0", "v2.0.0")
                    )
            );
        }

        @Test
        void Check_Invalid_Min_Version() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRange(
                            "invalid_version",
                            null,
                            null
                    )
            );
        }

        @Test
        void Check_Invalid_Max_Version() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRange(
                            "v1.0.0",
                            "invalid_version",
                            null
                    )
            );
        }

        @Test
        void Check_Invalid_Excluded_Version() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRange(
                            "v1.0.0",
                            null,
                            List.of("v1.3.0", "invalid_version")
                    )
            );
        }
    }

    @Nested
    public class JsonTests {

        @Test
        void Check_Valid_Version_Range() {
            VersionRange expectedVersionRange = new VersionRange("v1.1.0", null, null);

            VersionRange versionRange = new Gson().fromJson("""
                    {
                        "min": "v1.1.0"
                    }
                    """,
                    VersionRange.class
            );

            Assertions.assertEquals(expectedVersionRange, versionRange);
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {}
                            """,
                            VersionRange.class
                    )
            );
        }

        @Test
        void Check_Max() {
            String expectedMax = "v2.0.0";

            VersionRange versionRange = new Gson().fromJson("""
                    {
                        "min": "v1.1.0",
                        "max": "v2.0.0"
                    }
                    """,
                    VersionRange.class
            );

            Assertions.assertEquals(expectedMax, versionRange.max());
        }

        @Test
        void Check_Invalid_When_Max_Lower_Than_Min() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "min": "v2.0.0",
                                "max": "v1.1.0"
                            }
                            """,
                            VersionRange.class
                    )
            );
        }

        @Test
        void Check_Excluded() {
            List<String> expectedExcluded = List.of("v1.3.0", "v2.0.0");

            VersionRange versionRange = new Gson().fromJson("""
                    {
                        "min": "v1.1.0",
                        "excludes": ["v1.3.0", "v2.0.0"]
                    }
                    """,
                    VersionRange.class
            );

            Assertions.assertEquals(expectedExcluded, versionRange.excludes());
        }

        @Test
        void Check_Invalid_When_Excluded_Lower_Than_Min() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "min": "v2.0.0",
                                "excludes": ["v1.3.0", "v2.0.0"]
                            }
                            """,
                            VersionRange.class
                    )
            );
        }

        @Test
        void Check_Invalid_When_Excluded_Higher_Than_Max() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "min": "v1.0.0",
                                "max": "v1.1.0",
                                "excludes": ["v1.3.0", "v2.0.0"]
                            }
                            """,
                            VersionRange.class
                    )
            );
        }

        @Test
        void Check_Invalid_Min_Version() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "min": "invalid_version"
                            }
                            """,
                            VersionRange.class
                    )
            );
        }

        @Test
        void Check_Invalid_Max_Version() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "min": "v1.0.0",
                                "max": "invalid_version"
                            }
                            """,
                            VersionRange.class
                    )
            );
        }

        @Test
        void Check_Invalid_Excluded_Version() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "min": "v1.0.0",
                                "excludes": ["v1.3.0", "invalid_version"]
                            }
                            """,
                            VersionRange.class
                    )
            );
        }
    }

    @Test
    void Check_Version_Compatibility_When_Version_Null() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );

        Assertions.assertThrows(
                NullPointerException.class,
                () -> versionRange.isCompatible(null)
        );
    }

    @Test
    void Check_Version_Compatibility_When_Invalid_Version() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "invalid_version";

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> versionRange.isCompatible(version)
        );
    }

    @Test
    void Check_Compatible_Version() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v0.1.4";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertTrue(isCompatible);
    }

    @Test
    void Check_Compatible_Version_When_Minor_Not_Specified_For_Min() {
        VersionRange versionRange = new VersionRange(
                "v0.1",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v0.1.4";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertTrue(isCompatible);
    }

    @Test
    void Check_Compatible_Version_When_Minor_And_Patch_Not_Specified_For_Min() {
        VersionRange versionRange = new VersionRange(
                "v0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v0.1.4";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertTrue(isCompatible);
    }

    @Test
    void Check_Compatible_Version_When_Minor_Not_Specified_For_Max() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v1.0.4";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertTrue(isCompatible);
    }

    @Test
    void Check_Compatible_Version_When_Minor_And_Patch_Not_Specified_For_Max() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v1.1.4";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertTrue(isCompatible);
    }

    @Test
    void Check_Incompatible_Version_Because_Of_Min() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v0.0.4";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertFalse(isCompatible);
    }

    @Test
    void Check_Incompatible_Version_Because_Of_Max() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v1.0.4";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertFalse(isCompatible);
    }

    @Test
    void Check_Incompatible_Version_Because_Of_Excluded() {
        VersionRange versionRange = new VersionRange(
                "v0.1.0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v0.2.0";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertFalse(isCompatible);
    }
}
