package qupath.ext.extensionmanager.core.catalog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.SimpleServer;
import qupath.ext.extensionmanager.TestUtils;
import qupath.ext.extensionmanager.core.model.CatalogModel;
import qupath.ext.extensionmanager.core.model.ExtensionModel;
import qupath.ext.extensionmanager.core.model.ReleaseModel;
import qupath.ext.extensionmanager.core.model.VersionRangeModel;
import qupath.ext.extensionmanager.core.registry.RegistryCatalog;
import qupath.ext.extensionmanager.core.registry.RegistryExtension;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TestCatalog {

    abstract static class GenericCreation {

        @Test
        void Check_Name() {
            String expectedName = "name";

            Catalog catalog = createCatalog();

            Assertions.assertEquals(expectedName, catalog.getName());
        }

        @Test
        void Check_Description() {
            String expectedDescription = "description";

            Catalog catalog = createCatalog();

            Assertions.assertEquals(expectedDescription, catalog.getDescription());
        }

        @Test
        void Check_Uri() {
            URI expectedUri = URI.create("http://uri.com");

            Catalog catalog = createCatalog();

            Assertions.assertEquals(expectedUri, catalog.getUri());
        }

        @Test
        void Check_Raw_Uri() {
            URI expectedRawUri = URI.create("http://raw.com");

            Catalog catalog = createCatalog();

            Assertions.assertEquals(expectedRawUri, catalog.getRawUri());
        }

        @Test
        void Check_Deletable() {
            boolean expectedDeletable = false;

            Catalog catalog = createCatalog();

            Assertions.assertEquals(expectedDeletable, catalog.isDeletable());
        }

        @Test
        abstract void Check_Extensions() throws IOException, ExecutionException, InterruptedException;

        protected abstract Catalog createCatalog();
    }

    @Nested
    class AttributeCreation extends GenericCreation {

        @Test
        void Check_Null_Name() {
            String name = null;
            String description = "description";
            URI uri = URI.create("http://uri.com");
            URI rawUri = URI.create("http://raw.com");

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(name, description, uri, rawUri));
        }

        @Test
        void Check_Null_Description() {
            String name = null;
            String description = "description";
            URI uri = URI.create("http://uri.com");
            URI rawUri = URI.create("http://raw.com");

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(name, description, uri, rawUri));
        }

        @Test
        void Check_Null_Uri() {
            String name = "name";
            String description = "description";
            URI uri = null;
            URI rawUri = URI.create("http://raw.com");

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(name, description, uri, rawUri));
        }

        @Test
        void Check_Null_Raw_Uri() {
            String name = "name";
            String description = "description";
            URI uri = URI.create("http://uri.com");
            URI rawUri = null;

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(name, description, uri, rawUri));
        }

        @Test
        @Override
        void Check_Extensions() throws IOException, ExecutionException, InterruptedException {
            SimpleServer server = new SimpleServer(List.of(new SimpleServer.FileToServe(
                    "catalog.json",
                    Objects.requireNonNull(TestCatalog.class.getResourceAsStream("catalog.json"))
            )));
            Catalog catalog = new Catalog(
                    "name",
                    "description",
                    server.getURI("catalog.json"),
                    server.getURI("catalog.json")
            );
            List<Extension> expectedExtensions = List.of(new Extension(
                    new ExtensionModel(
                            "Some extension",
                            "Some extension description",
                            "Some author",
                            URI.create("http://github.com/qupath/qupath"),
                            false,
                            List.of(
                                    new ReleaseModel(
                                            "v0.1.0",
                                            URI.create("https://github.com/qupath/qupath"),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            new VersionRangeModel("v1.0.0", null, null)
                                    ),
                                    new ReleaseModel(
                                            "v1.0.0",
                                            URI.create("https://github.com/qupath/qupath"),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            new VersionRangeModel("v2.0.0", null, null)
                                    )
                            )
                    ),
                    null,
                    false
            ));

            List<Extension> extensions = catalog.getExtensions().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedExtensions, extensions);

            server.close();
        }

        @Override
        protected Catalog createCatalog() {
            return new Catalog(
                    "name",
                    "description",
                    URI.create("http://uri.com"),
                    URI.create("http://raw.com")
            );
        }
    }

    @Nested
    class CatalogModelCreation extends GenericCreation {

        @Test
        void Check_Null_Catalog_Model() {
            CatalogModel catalogModel = null;
            URI uri = URI.create("http://uri.com");
            URI rawUri = URI.create("http://raw.com");
            boolean deletable = true;

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(catalogModel, uri, rawUri, deletable));
        }

        @Test
        void Check_Null_Uri() {
            CatalogModel catalogModel = new CatalogModel("name", "description", List.of());
            URI uri = null;
            URI rawUri = URI.create("http://raw.com");
            boolean deletable = true;

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(catalogModel, uri, rawUri, deletable));
        }

        @Test
        void Check_Null_Raw_Uri() {
            CatalogModel catalogModel = new CatalogModel("name", "description", List.of());
            URI uri = URI.create("http://uri.com");
            URI rawUri = null;
            boolean deletable = true;

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(catalogModel, uri, rawUri, deletable));
        }

        @Test
        @Override
        void Check_Extensions() throws ExecutionException, InterruptedException {
            Catalog catalog = new Catalog(
                    new CatalogModel(
                            "name",
                            "description",
                            List.of(new ExtensionModel(
                                    "name",
                                    "description",
                                    "author",
                                    URI.create("https://github.com/qupath/qupath"),
                                    true,
                                    List.of(
                                            new ReleaseModel(
                                                    "v0.1.0",
                                                    URI.create("https://github.com/qupath/qupath"),
                                                    List.of(),
                                                    List.of(),
                                                    List.of(),
                                                    new VersionRangeModel("v1.0.0", null, null)
                                            ),
                                            new ReleaseModel(
                                                    "v1.0.0",
                                                    URI.create("https://github.com/qupath/qupath"),
                                                    List.of(),
                                                    List.of(),
                                                    List.of(),
                                                    new VersionRangeModel("v2.0.0", null, null)
                                            )
                                    )
                            ))
                    ),
                    URI.create("http://uri.com"),
                    URI.create("http://raw.com"),
                    false
            );
            List<Extension> expectedExtensions = List.of(new Extension(
                    new ExtensionModel(
                            "name",
                            "description",
                            "author",
                            URI.create("https://github.com/qupath/qupath"),
                            true,
                            List.of(
                                    new ReleaseModel(
                                            "v0.1.0",
                                            URI.create("https://github.com/qupath/qupath"),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            new VersionRangeModel("v1.0.0", null, null)
                                    ),
                                    new ReleaseModel(
                                            "v1.0.0",
                                            URI.create("https://github.com/qupath/qupath"),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            new VersionRangeModel("v2.0.0", null, null)
                                    )
                            )
                    ),
                    null,
                    false
            ));

            List<Extension> extensions = catalog.getExtensions().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedExtensions, extensions);
        }

        @Override
        protected Catalog createCatalog() {
            return new Catalog(
                    new CatalogModel("name", "description", List.of()),
                    URI.create("http://uri.com"),
                    URI.create("http://raw.com"),
                    false
            );
        }
    }

    @Nested
    class RegistryCatalogCreation extends GenericCreation {

        @Test
        void Check_Null_Registry_Catalog() {
            RegistryCatalog registryCatalog = null;

            Assertions.assertThrows(NullPointerException.class, () -> new Catalog(registryCatalog));
        }

        @Test
        @Override
        void Check_Extensions() throws IOException, ExecutionException, InterruptedException {
            SimpleServer server = new SimpleServer(List.of(new SimpleServer.FileToServe(
                    "catalog.json",
                    Objects.requireNonNull(TestCatalog.class.getResourceAsStream("catalog.json"))
            )));
            Catalog catalog = new Catalog(new RegistryCatalog(
                    "name",
                    "description",
                    server.getURI("catalog.json"),
                    server.getURI("catalog.json"),
                    false,
                    List.of(new RegistryExtension("Some extension", "v1.0.0", true))
            ));
            List<Extension> expectedExtensions = List.of(new Extension(
                    new ExtensionModel(
                            "Some extension",
                            "Some extension description",
                            "Some author",
                            URI.create("http://github.com/qupath/qupath"),
                            false,
                            List.of(
                                    new ReleaseModel(
                                            "v0.1.0",
                                            URI.create("https://github.com/qupath/qupath"),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            new VersionRangeModel("v1.0.0", null, null)
                                    ),
                                    new ReleaseModel(
                                            "v1.0.0",
                                            URI.create("https://github.com/qupath/qupath"),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            new VersionRangeModel("v2.0.0", null, null)
                                    )
                            )
                    ),
                    new Release(new ReleaseModel(
                            "v1.0.0",
                            URI.create("https://github.com/qupath/qupath"),
                            List.of(),
                            List.of(),
                            List.of(),
                            new VersionRangeModel("v2.0.0", null, null)
                    )),
                    true
            ));

            List<Extension> extensions = catalog.getExtensions().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedExtensions, extensions);

            server.close();
        }

        @Override
        protected Catalog createCatalog() {
            return new Catalog(new RegistryCatalog(
                    "name",
                    "description",
                    URI.create("http://uri.com"),
                    URI.create("http://raw.com"),
                    false,
                    List.of()
            ));
        }
    }
}
