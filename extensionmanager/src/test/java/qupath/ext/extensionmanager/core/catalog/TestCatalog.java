package qupath.ext.extensionmanager.core.catalog;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

public class TestCatalog {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Catalog() {
            Assertions.assertDoesNotThrow(() -> new Catalog(
                    "",
                    "",
                    List.of(new Extension("", "", "", URI.create("https://github.com/qupath/qupath"), List.of()))
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Catalog(null, "", List.of())
            );
        }

        @Test
        void Check_Undefined_Description() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Catalog("", null, List.of())
            );
        }

        @Test
        void Check_Undefined_Extensions() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Catalog("", "", null)
            );
        }

        @Test
        void Check_Extensions_With_Same_Name() {
            List<Extension> extensions = List.of(
                    new Extension("name", "", "", URI.create("https://github.com/qupath/qupath"), List.of()),
                    new Extension("name", "", "", URI.create("https://github.com/qupath/qupath"), List.of()),
                    new Extension("other_name", "", "", URI.create("https://github.com/qupath/qupath"), List.of())
            );

            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new Catalog("", "", extensions)
            );
        }
    }

    @Nested
    public class JsonTests {

        @Test
        void Check_Valid_Catalog() {
            Catalog expectedCatalog = new Catalog(
                    "",
                    "",
                    List.of(new Extension(
                            "",
                            "",
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            List.of()
                    ))
            );

            Catalog catalog = new Gson().fromJson("""
                    {
                        "name": "",
                        "description": "",
                        "extensions": [
                            {
                                "name": "",
                                "description": "",
                                "author": "",
                                "homepage": "https://github.com/qupath/qupath",
                                "releases": []
                            }
                        ]
                    }
                    """,
                    Catalog.class
            );

            Assertions.assertEquals(expectedCatalog, catalog);
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "description": "",
                                "extensions": []
                            }
                            """,
                            Catalog.class
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
                                "extensions": []
                            }
                            """,
                            Catalog.class
                    )
            );
        }

        @Test
        void Check_Undefined_Extensions() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": ""
                            }
                            """,
                            Catalog.class
                    )
            );
        }

        @Test
        void Check_Invalid_Extensions() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": "",
                                "extensions": [{}]
                            }
                            """,
                            Catalog.class
                    )
            );
        }

        @Test
        void Check_Extensions_With_Same_Name() {
            Assertions.assertThrows(
                    RuntimeException.class,
                    () -> new Gson().fromJson("""
                            {
                                "name": "",
                                "description": "",
                                "extensions": [
                                    {
                                        "name": "name",
                                        "description": "",
                                        "author": "",
                                        "homepage": "https://github.com/qupath/qupath",
                                        "releases": []
                                    },
                                    {
                                        "name": "name",
                                        "description": "",
                                        "author": "",
                                        "homepage": "https://github.com/qupath/qupath",
                                        "releases": []
                                    },
                                    {
                                        "name": "other_name",
                                        "description": "",
                                        "author": "",
                                        "homepage": "https://github.com/qupath/qupath",
                                        "releases": []
                                    }
                                ]
                            }
                            """,
                            Catalog.class
                    )
            );
        }
    }
}
