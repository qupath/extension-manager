package qupath.ext.extensionmanager.core.catalog;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.SimpleServer;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TestCatalogFetcher {

    private static final Catalog VALID_CATALOG = new Catalog(
            "Some catalog",
            "Some description",
            List.of(new Extension(
                    "Some extension",
                    "Some extension description",
                    "Some author",
                    URI.create("https://github.com/qupath/qupath"),
                    false,
                    List.of(
                            new Release(
                                    "v0.1.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    null,
                                    null,
                                    null,
                                    new VersionRange("v1.0.0", null, null)
                            ),
                            new Release(
                                    "v1.0.0",
                                    URI.create("https://github.com/qupath/qupath"),
                                    null,
                                    null,
                                    null,
                                    new VersionRange("v2.0.0", null, null)
                            )
                    )
            ))
    );
    private static SimpleServer server;
    private enum JsonCatalog {
        VALID_CATALOG("valid_catalog"),
        INVALID_CATALOG("invalid_catalog");

        private final String name;

        JsonCatalog(String name) {
            this.name = name;
        }

        public String getFileName() {
            return String.format("%s.json", name);
        }
    }

    @BeforeAll
    static void setupServer() throws IOException {
        server = new SimpleServer(Arrays.stream(JsonCatalog.values())
                .map(jsonCatalog -> new SimpleServer.FileToServe(
                        jsonCatalog.getFileName(),
                        Objects.requireNonNull(TestCatalogFetcher.class.getResourceAsStream(jsonCatalog.getFileName()))
                ))
                .toList()
        );
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void Check_Null_URI() {
        Assertions.assertThrows(
                ExecutionException.class,
                () -> CatalogFetcher.getCatalog(null).get()
        );
    }

    @Test
    void Check_Invalid_URI() {
        Assertions.assertThrows(
                ExecutionException.class,
                () -> CatalogFetcher.getCatalog(URI.create("")).get()
        );
    }

    @Test
    void Check_Valid_Catalog_Retrievable() {
        Assertions.assertDoesNotThrow(() -> CatalogFetcher.getCatalog(
                server.getURI(JsonCatalog.VALID_CATALOG.getFileName())).get()
        );
    }

    @Test
    void Check_Valid_Catalog_Values() throws ExecutionException, InterruptedException {
        Catalog catalog = CatalogFetcher.getCatalog(server.getURI(JsonCatalog.VALID_CATALOG.getFileName())).get();

        Assertions.assertEquals(VALID_CATALOG, catalog);
    }

    @Test
    void Check_Invalid_Catalog() {
        Assertions.assertThrows(
                ExecutionException.class,
                () -> CatalogFetcher.getCatalog(server.getURI(JsonCatalog.INVALID_CATALOG.getFileName())).get()
        );
    }
}
