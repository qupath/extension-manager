package qupath.ext.extensionmanager.core.catalog;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class TestExtension {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Extension() {
            Assertions.assertDoesNotThrow(() -> new Extension(
                    "",
                    "",
                    "",
                    URI.create("https://github.com/qupath/qupath"),
                    List.of(new Release(
                            "v1.0.0",
                            URI.create("https://github.com/qupath/qupath"),
                            List.of(URI.create("https://github.com/qupath/qupath")),
                            null,
                            null,
                            new VersionRange("v1.0.0", null, null)
                    ))
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Extension(
                            null,
                            "",
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            List.of(new Release(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRange("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Description() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Extension(
                            "",
                            null,
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            List.of(new Release(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRange("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Author() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Extension(
                            "",
                            "",
                            null,
                            URI.create("https://github.com/qupath/qupath"),
                            List.of(new Release(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRange("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Homepage() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Extension(
                            "",
                            "",
                            "",
                            null,
                            List.of(new Release(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRange("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Releases() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Extension(
                            "",
                            "",
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            null
                    )
            );
        }

        @Test
        void Check_Homepage_Not_GitHub() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Extension(
                        "",
                        "",
                        "",
                        URI.create("https://qupath.readthedocs.io/"),
                        List.of(new Release(
                                "v1.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.0.0", null, null)
                        ))
                    )
            );
        }
    }

    @Nested
    public class JsonTests {

        @Test
        void Check_Valid_Extension() {
            Assertions.assertDoesNotThrow(() -> new Gson().fromJson("""
                    {
                        "name": "",
                        "description": "",
                        "author": "",
                        "homepage": "https://github.com/qupath/qupath",
                        "releases": [
                            {
                                "name": "v1.0.0",
                                "mainUrl": "https://github.com/qupath/qupath",
                                "versionRange": {
                                    "min": "v1.0.0"
                                }
                            }
                        ]
                    }
                    """,
                    Extension.class
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "description": "",
                                "author": "",
                                "homepage": "https://github.com/qupath/qupath",
                                "releases": [
                                    {
                                        "name": "v1.0.0",
                                        "mainUrl": "https://github.com/qupath/qupath",
                                        "versionRange": {
                                            "min": "v1.0.0"
                                        }
                                    }
                                ]
                            }
                            """,
                            Extension.class
                    )
            );
        }

        @Test
        void Check_Undefined_Description() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "author": "",
                                "homepage": "https://github.com/qupath/qupath",
                                "releases": [
                                    {
                                        "name": "v1.0.0",
                                        "mainUrl": "https://github.com/qupath/qupath",
                                        "versionRange": {
                                            "min": "v1.0.0"
                                        }
                                    }
                                ]
                            }
                            """,
                            Extension.class
                    )
            );
        }

        @Test
        void Check_Undefined_Author() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": "",
                                "homepage": "https://github.com/qupath/qupath",
                                "releases": [
                                    {
                                        "name": "v1.0.0",
                                        "mainUrl": "https://github.com/qupath/qupath",
                                        "versionRange": {
                                            "min": "v1.0.0"
                                        }
                                    }
                                ]
                            }
                            """,
                            Extension.class
                    )
            );
        }

        @Test
        void Check_Undefined_Homepage() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": "",
                                "author": "",
                                "releases": [
                                    {
                                        "name": "v1.0.0",
                                        "mainUrl": "https://github.com/qupath/qupath",
                                        "versionRange": {
                                            "min": "v1.0.0"
                                        }
                                    }
                                ]
                            }
                            """,
                            Extension.class
                    )
            );
        }

        @Test
        void Check_Undefined_Releases() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": "",
                                "author": "",
                                "homepage": "https://github.com/qupath/qupath"
                            }
                            """,
                            Extension.class
                    )
            );
        }

        @Test
        void Check_Invalid_Release() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": "",
                                "author": "",
                                "homepage": "https://github.com/qupath/qupath",
                                "releases": [{}]
                            }
                            """,
                            Extension.class
                    )
            );
        }

        @Test
        void Check_Homepage_Not_GitHub() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": "",
                                "author": "",
                                "homepage": "https://qupath.readthedocs.io/",
                                "releases": [
                                    {
                                        "name": "v1.0.0",
                                        "mainUrl": "https://github.com/qupath/qupath",
                                        "versionRange": {
                                            "min": "v1.0.0"
                                        }
                                    }
                                ]
                            }
                            """,
                            Extension.class
                    )
            );
        }
    }
    @Test
    void Check_Max_Compatible_Release_When_Two_Compatibles() {
        Release expectedRelease = new Release(
                "v2.0.0",
                URI.create("https://github.com/qupath/qupath"),
                List.of(URI.create("https://github.com/qupath/qupath")),
                null,
                null,
                new VersionRange("v1.1.0", null, null)
        );
        Extension extension = new Extension(
                "",
                "",
                "",
                URI.create("https://github.com/qupath/qupath"),
                List.of(
                        new Release(
                                "v1.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.0.0", null, null)
                        ),
                        expectedRelease,
                        new Release(
                                "v3.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.2.0", null, null)
                        )
                )
        );
        String version = "v1.1.0";

        Release release = extension.getMaxCompatibleRelease(version).orElse(null);

        Assertions.assertEquals(expectedRelease, release);
    }

    @Test
    void Check_Max_Compatible_Release_When_Three_Compatible() {
        Release expectedRelease = new Release(
                "v3.0.0",
                URI.create("https://github.com/qupath/qupath"),
                List.of(URI.create("https://github.com/qupath/qupath")),
                null,
                null,
                new VersionRange("v1.2.0", null, null)
        );
        Extension extension = new Extension(
                "",
                "",
                "",
                URI.create("https://github.com/qupath/qupath"),
                List.of(
                        new Release(
                                "v1.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.0.0", null, null)
                        ),
                        new Release(
                                "v2.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.1.0", null, null)
                        ),
                        expectedRelease
                )
        );
        String version = "v2.0.0";

        Release release = extension.getMaxCompatibleRelease(version).orElse(null);

        Assertions.assertEquals(expectedRelease, release);
    }

    @Test
    void Check_Max_Compatible_Release_When_Zero_Compatible() {
        Extension extension = new Extension(
                "",
                "",
                "",
                URI.create("https://github.com/qupath/qupath"),
                List.of(
                        new Release(
                                "v1.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.0.0", null, null)
                        ),
                        new Release(
                                "v2.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.1.0", null, null)
                        ),
                        new Release(
                                "v3.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.2.0", null, null)
                        )
                )
        );
        String version = "v0.0.1";

        Optional<Release> release = extension.getMaxCompatibleRelease(version);

        Assertions.assertTrue(release.isEmpty());
    }

    @Test
    void Check_Max_Compatible_Release_When_Invalid_Version() {
        Extension extension = new Extension(
                "",
                "",
                "",
                URI.create("https://github.com/qupath/qupath"),
                List.of(
                        new Release(
                                "v1.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.0.0", null, null)
                        ),
                        new Release(
                                "v2.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.1.0", null, null)
                        ),
                        new Release(
                                "v3.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.2.0", null, null)
                        )
                )
        );
        String version = "invalid_version";

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> extension.getMaxCompatibleRelease(version)
        );
    }

    @Test
    void Check_Max_Compatible_Release_When_Null_Version() {
        Extension extension = new Extension(
                "",
                "",
                "",
                URI.create("https://github.com/qupath/qupath"),
                List.of(
                        new Release(
                                "v1.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.0.0", null, null)
                        ),
                        new Release(
                                "v2.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.1.0", null, null)
                        ),
                        new Release(
                                "v3.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRange("v1.2.0", null, null)
                        )
                )
        );

        Assertions.assertThrows(
                NullPointerException.class,
                () -> extension.getMaxCompatibleRelease(null)
        );
    }
}
