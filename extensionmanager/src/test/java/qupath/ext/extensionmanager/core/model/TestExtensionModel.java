package qupath.ext.extensionmanager.core.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

public class TestExtensionModel {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Extension() {
            Assertions.assertDoesNotThrow(() -> new ExtensionModel(
                    "",
                    "",
                    "",
                    URI.create("https://github.com/qupath/qupath"),
                    false,
                    List.of(new ReleaseModel(
                            "v1.0.0",
                            URI.create("https://github.com/qupath/qupath"),
                            List.of(URI.create("https://github.com/qupath/qupath")),
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    ))
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ExtensionModel(
                            null,
                            "",
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            false,
                            List.of(new ReleaseModel(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRangeModel("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Description() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ExtensionModel(
                            "",
                            null,
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            false,
                            List.of(new ReleaseModel(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRangeModel("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Author() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ExtensionModel(
                            "",
                            "",
                            null,
                            URI.create("https://github.com/qupath/qupath"),
                            false,
                            List.of(new ReleaseModel(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRangeModel("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Homepage() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ExtensionModel(
                            "",
                            "",
                            "",
                            null,
                            false,
                            List.of(new ReleaseModel(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    List.of(URI.create("https://github.com/qupath/qupath")),
                                    null,
                                    null,
                                    new VersionRangeModel("v1.0.0", null, null)
                            ))
                    )
            );
        }

        @Test
        void Check_Undefined_Releases() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ExtensionModel(
                            "",
                            "",
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            false,
                            null
                    )
            );
        }

        @Test
        void Check_Homepage_Not_GitHub() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new ExtensionModel(
                        "",
                        "",
                        "",
                        URI.create("https://qupath.readthedocs.io/"),
                        false,
                        List.of(new ReleaseModel(
                                "v1.0.0",
                                URI.create("https://github.com/qupath/qupath"),
                                List.of(URI.create("https://github.com/qupath/qupath")),
                                null,
                                null,
                                new VersionRangeModel("v1.0.0", null, null)
                        ))
                    )
            );
        }
    }

    @Nested
    public class JsonTests {

        @Test
        void Check_Valid_Extension() {
            ExtensionModel expectedExtension = new ExtensionModel(
                    "",
                    "",
                    "",
                    URI.create("https://github.com/qupath/qupath"),
                    true,
                    List.of(new ReleaseModel(
                            "v1.0.0",
                            URI.create("https://github.com/qupath/qupath"),
                            null,
                            null,
                            null,
                            new VersionRangeModel("v1.0.0", null, null)
                    ))
            );

            ExtensionModel extension = new Gson().fromJson("""
                    {
                        "name": "",
                        "description": "",
                        "author": "",
                        "homepage": "https://github.com/qupath/qupath",
                        "starred": true,
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
                    ExtensionModel.class
            );

            Assertions.assertEquals(expectedExtension, extension);
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
                            ExtensionModel.class
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
                            ExtensionModel.class
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
                            ExtensionModel.class
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
                            ExtensionModel.class
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
                            ExtensionModel.class
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
                            ExtensionModel.class
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
                            ExtensionModel.class
                    )
            );
        }
    }
}
