package qupath.ext.extensionmanager.core.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestVersionRangeModel {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Version_Range() {
            Assertions.assertDoesNotThrow(() -> new VersionRangeModel(
                    "v1.1.0",
                    null,
                    null
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRangeModel(
                            null,
                            null,
                            null
                    )
            );
        }

        @Test
        void Check_Valid_With_Max() {
            Assertions.assertDoesNotThrow(() -> new VersionRangeModel(
                    "v1.1.0",
                    "v2.0.0",
                    null
            ));
        }

        @Test
        void Check_Invalid_When_Max_Lower_Than_Min() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRangeModel(
                            "v2.0.0",
                            "v1.1.0",
                            null
                    )
            );
        }

        @Test
        void Check_Valid_With_Excluded() {
            Assertions.assertDoesNotThrow(() -> new VersionRangeModel(
                    "v1.1.0",
                    null,
                    List.of("v1.3.0", "v2.0.0")
            ));
        }

        @Test
        void Check_Invalid_When_Excluded_Lower_Than_Min() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new VersionRangeModel(
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
                    () -> new VersionRangeModel(
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
                    () -> new VersionRangeModel(
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
                    () -> new VersionRangeModel(
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
                    () -> new VersionRangeModel(
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
            VersionRangeModel expectedVersionRange = new VersionRangeModel("v1.1.0", null, null);

            VersionRangeModel versionRange = new Gson().fromJson("""
                    {
                        "min": "v1.1.0"
                    }
                    """,
                    VersionRangeModel.class
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
                            VersionRangeModel.class
                    )
            );
        }

        @Test
        void Check_Max() {
            String expectedMax = "v2.0.0";

            VersionRangeModel versionRange = new Gson().fromJson("""
                    {
                        "min": "v1.1.0",
                        "max": "v2.0.0"
                    }
                    """,
                    VersionRangeModel.class
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
                            VersionRangeModel.class
                    )
            );
        }

        @Test
        void Check_Excluded() {
            List<String> expectedExcluded = List.of("v1.3.0", "v2.0.0");

            VersionRangeModel versionRange = new Gson().fromJson("""
                    {
                        "min": "v1.1.0",
                        "excludes": ["v1.3.0", "v2.0.0"]
                    }
                    """,
                    VersionRangeModel.class
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
                            VersionRangeModel.class
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
                            VersionRangeModel.class
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
                            VersionRangeModel.class
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
                            VersionRangeModel.class
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
                            VersionRangeModel.class
                    )
            );
        }
    }

    @Test
    void Check_Version_Compatibility_When_Version_Null() {
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
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
        VersionRangeModel versionRange = new VersionRangeModel(
                "v0.1.0",
                "v1.0.0",
                List.of("v0.1.1", "v0.2.0", "v1.0.0")
        );
        String version = "v0.2.0";

        boolean isCompatible = versionRange.isCompatible(version);

        Assertions.assertFalse(isCompatible);
    }
}
