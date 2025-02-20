package qupath.ext.extensionmanager.core.catalog;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

public class TestRelease {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Release() {
            Assertions.assertDoesNotThrow(() -> new Release(
                    "v0.1.0",
                    URI.create("https://github.com/qupath/qupath"),
                    null,
                    null,
                    null,
                    new VersionRange("v1.0.0", null, null)
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Release(
                            null,
                            URI.create("https://github.com/qupath/qupath"),
                            null,
                            null,
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Undefined_Main_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Release(
                            "v0.1.0",
                            null,
                            null,
                            null,
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Undefined_Version_Range() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Release(
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
                    () -> new Release(
                            "invalid_version",
                            URI.create("https://github.com/qupath/qupath"),
                            null,
                            null,
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Main_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Release(
                            "v0.1.0",
                            URI.create("https://qupath.readthedocs.io/"),
                            null,
                            null,
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Valid_Required_Dependency_Url() {
            Assertions.assertDoesNotThrow(
                    () -> new Release(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            List.of(URI.create("https://maven.scijava.org/content")),
                            null,
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Required_Dependency_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Release(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            List.of(URI.create("https://qupath.readthedocs.io/")),
                            null,
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Valid_Optional_Dependency_Url() {
            Assertions.assertDoesNotThrow(
                    () -> new Release(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            List.of(URI.create("https://maven.scijava.org/content")),
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Optional_Dependency_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Release(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            List.of(URI.create("https://qupath.readthedocs.io/")),
                            null,
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Valid_Javadoc_Url() {
            Assertions.assertDoesNotThrow(
                    () -> new Release(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            null,
                            List.of(URI.create("https://maven.scijava.org/content")),
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }

        @Test
        void Check_Invalid_Javadoc_Url() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Release(
                            "v0.1.0",
                            URI.create("https://github.com/qupath/qupath/"),
                            null,
                            null,
                            List.of(URI.create("https://qupath.readthedocs.io/")),
                            new VersionRange("v1.0.0", null, null)
                    )
            );
        }
    }

    @Nested
    public class JsonTests {

        @Test
        void Check_Valid_Release() {
            Release expectedRelease = new Release(
                    "v0.1.0",
                    URI.create("https://github.com/qupath/qupath"),
                    null,
                    null,
                    null,
                    new VersionRange("v1.0.0", null, null)
            );

            Release release = new Gson().fromJson("""
                    {
                        "name": "v0.1.0",
                        "mainUrl": "https://github.com/qupath/qupath",
                        "versionRange": {
                            "min": "v1.0.0"
                        }
                    }
                    """
                    ,
                    Release.class
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
                            Release.class
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
                            Release.class
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
                            Release.class
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
                            Release.class
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
                            Release.class
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
                            Release.class
                    )
            );
        }

        @Test
        void Check_Valid_Required_Dependency_Url() {
            List<URI> expectedRequiredDependencyUrls = List.of(URI.create("https://maven.scijava.org/content"));

            Release release = new Gson().fromJson("""
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
                    Release.class
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
                            Release.class
                    )
            );
        }

        @Test
        void Check_Valid_Optional_Dependency_Url() {
            List<URI> expectedOptionalDependencyUrls = List.of(URI.create("https://maven.scijava.org/content"));

            Release release = new Gson().fromJson("""
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
                    Release.class
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
                            Release.class
                    )
            );
        }

        @Test
        void Check_Valid_Javadoc_Url() {
            List<URI> expectedJavadocUrls = List.of(URI.create("https://maven.scijava.org/content"));

            Release release = new Gson().fromJson("""
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
                    Release.class
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
                            Release.class
                    )
            );
        }
    }
}
