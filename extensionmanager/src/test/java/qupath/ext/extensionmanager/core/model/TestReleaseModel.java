package qupath.ext.extensionmanager.core.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

public class TestReleaseModel {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Release() {
            Assertions.assertDoesNotThrow(() -> new ReleaseModel(
                    "v0.1.0",
                    URI.create("https://github.com/qupath/qupath"),
                    null,
                    null,
                    null,
                    new VersionRangeModel("v1.0.0", null, null)
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            null,
                            URI.create("https://github.com/qupath/qupath"),
                            null,
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Undefined_Main_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            "v0.1.0",
                            null,
                            null,
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Undefined_Version_Range() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath"),
                            null,
                            null,
                            null,
                            null
                    )
            );
        }

        @Test
        void Check_Invalid_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            "invalid_version",
                            URI.create("https://github.com/qupath/qupath"),
                            null,
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Main_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://qupath.readthedocs.io/"),
                            null,
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Valid_Required_Dependency_Url() {
            Assertions.assertDoesNotThrow(
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            List.of(URI.create("https://maven.scijava.org/content")),
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Required_Dependency_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            List.of(URI.create("https://qupath.readthedocs.io/")),
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Valid_Optional_Dependency_Url() {
            Assertions.assertDoesNotThrow(
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            List.of(URI.create("https://maven.scijava.org/content")),
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Optional_Dependency_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            List.of(URI.create("https://qupath.readthedocs.io/")),
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Valid_Javadoc_Url() {
            Assertions.assertDoesNotThrow(
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            null,
                            List.of(URI.create("https://maven.scijava.org/content")),
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Javadoc_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ReleaseModel(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            null,
                            List.of(URI.create("https://qupath.readthedocs.io/")),
                            new VersionRangeModel("v1.0.0", null, null)
                    )
            );
        }
    }

    @Nested
    public class JsonTests {

        @Test
        void Check_Valid_Release() {
            ReleaseModel expectedRelease = new ReleaseModel(
                    "v0.1.0",
                    URI.create("https://github.com/qupath/qupath"),
                    null,
                    null,
                    null,
                    new VersionRangeModel("v1.0.0", null, null)
            );

            ReleaseModel release = new Gson().fromJson("""
                    {
                        "name": "v0.1.0",
                        "mainUrl": "https://github.com/qupath/qupath",
                        "versionRange": {
                            "min": "v1.0.0"
                        }
                    }
                    """
                    ,
                    ReleaseModel.class
            );

            Assertions.assertEquals(expectedRelease, release);
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "mainUrl": "https://github.com/qupath/qupath",
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Undefined_Main_Url() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "v0.1.0",
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Undefined_Version_Range() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "v0.1.0",
                                "mainUrl": "https://github.com/qupath/qupath"
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Invalid_Name() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "invalid_version",
                                "mainUrl": "https://github.com/qupath/qupath",
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Invalid_Version_Range() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "v0.1.0",
                                "mainUrl": "https://github.com/qupath/qupath",
                                "versionRange": {}
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Invalid_Main_Url() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "v0.1.0",
                                "mainUrl": "https://qupath.readthedocs.io/",
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Valid_Required_Dependency_Url() {
            List<URI> expectedRequiredDependencyUrls = List.of(URI.create("https://maven.scijava.org/content"));

            ReleaseModel release = new Gson().fromJson("""
                    {
                        "name": "v0.1.0",
                        "mainUrl": "https://github.com/qupath/qupath",
                        "requiredDependencyUrls": ["https://maven.scijava.org/content"],
                        "versionRange": {
                            "min": "v1.0.0"
                        }
                    }
                    """
                    ,
                    ReleaseModel.class
            );

            Assertions.assertEquals(expectedRequiredDependencyUrls, release.requiredDependencyUrls());
        }

        @Test
        void Check_Invalid_Required_Dependency_Url() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "v0.1.0",
                                "mainUrl": "https://github.com/qupath/qupath/",
                                "requiredDependencyUrls": ["https://qupath.readthedocs.io/"],
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Valid_Optional_Dependency_Url() {
            List<URI> expectedOptionalDependencyUrls = List.of(URI.create("https://maven.scijava.org/content"));

            ReleaseModel release = new Gson().fromJson("""
                    {
                        "name": "v0.1.0",
                        "mainUrl": "https://github.com/qupath/qupath",
                        "optionalDependencyUrls": ["https://maven.scijava.org/content"],
                        "versionRange": {
                            "min": "v1.0.0"
                        }
                    }
                    """
                    ,
                    ReleaseModel.class
            );

            Assertions.assertEquals(expectedOptionalDependencyUrls, release.optionalDependencyUrls());
        }

        @Test
        void Check_Invalid_Optional_Dependency_Url() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "v0.1.0",
                                "mainUrl": "https://github.com/qupath/qupath/",
                                "optionalDependencyUrls": ["https://qupath.readthedocs.io/"],
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }

        @Test
        void Check_Valid_Javadoc_Url() {
            List<URI> expectedJavadocUrls = List.of(URI.create("https://maven.scijava.org/content"));

            ReleaseModel release = new Gson().fromJson("""
                    {
                        "name": "v0.1.0",
                        "mainUrl": "https://github.com/qupath/qupath",
                        "javadocUrls": ["https://maven.scijava.org/content"],
                        "versionRange": {
                            "min": "v1.0.0"
                        }
                    }
                    """
                    ,
                    ReleaseModel.class
            );

            Assertions.assertEquals(expectedJavadocUrls, release.javadocUrls());
        }

        @Test
        void Check_Invalid_Javadoc_Url() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "v0.1.0",
                                "mainUrl": "https://github.com/qupath/qupath/",
                                "javadocUrls": ["https://qupath.readthedocs.io/"],
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                            """,
                            ReleaseModel.class
                    )
            );
        }
    }
}
