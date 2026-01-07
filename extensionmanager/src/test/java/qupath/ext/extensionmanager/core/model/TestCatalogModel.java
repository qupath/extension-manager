package qupath.ext.extensionmanager.core.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

public class TestCatalogModel {

    @Nested
    public class ConstructorTests {

        @Test
        void Check_Valid_Catalog() {
            Assertions.assertDoesNotThrow(() -> new CatalogModel(
                    "",
                    "",
                    List.of(new ExtensionModel("", "", "", URI.create("https://github.com/qupath/qupath"), false, List.of()))
            ));
        }

        @Test
        void Check_Undefined_Name() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new CatalogModel(null, "", List.of())
            );
        }

        @Test
        void Check_Undefined_Description() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new CatalogModel("", null, List.of())
            );
        }

        @Test
        void Check_Undefined_Extensions() {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new CatalogModel("", "", null)
            );
        }

        @Test
        void Check_Extensions_With_Same_Name() {
            List<ExtensionModel> extensions = List.of(
                    new ExtensionModel("name", "", "", URI.create("https://github.com/qupath/qupath"), false, List.of()),
                    new ExtensionModel("name", "", "", URI.create("https://github.com/qupath/qupath"), false, List.of()),
                    new ExtensionModel("other_name", "", "", URI.create("https://github.com/qupath/qupath"), false, List.of())
            );

            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> new CatalogModel("", "", extensions)
            );
        }
    }

    @Nested
    public class JsonTests {

        @Test
        void Check_Valid_Catalog() {
            CatalogModel expectedCatalog = new CatalogModel(
                    "",
                    "",
                    List.of(new ExtensionModel(
                            "",
                            "",
                            "",
                            URI.create("https://github.com/qupath/qupath"),
                            false,
                            List.of()
                    ))
            );

            CatalogModel catalog = new Gson().fromJson("""
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
                    CatalogModel.class
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
                            CatalogModel.class
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
                            CatalogModel.class
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
                            CatalogModel.class
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
                            CatalogModel.class
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
                            CatalogModel.class
                    )
            );
        }
    }
}
