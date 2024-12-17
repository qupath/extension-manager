package qupath.ext.extensionmanager.core.catalog;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TestCatalogFetcher {

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8080;
    private static final Catalog VALID_CATALOG = new Catalog(
            "Some catalog",
            "Some description",
            List.of(new Extension(
                    "Some extension",
                    "Some extension description",
                    "Some author",
                    URI.create("https://github.com/qupath/qupath"),
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
    private static HttpServer server;
    private enum JsonCatalog {
        VALID_CATALOG("valid_catalog"),
        INVALID_CATALOG("invalid_catalog");

        private final String name;

        JsonCatalog(String name) {
            this.name = name;
        }

        public URI getURI() {
            return URI.create(String.format("http://%s:%d/%s", HOSTNAME, PORT, getFileName()));
        }

        public String getFileName() {
            return String.format("%s.json", name);
        }
    }

    @BeforeAll
    static void setupServer() throws IOException {
        Path serverFilesPath = Files.createTempDirectory("").toFile().toPath();
        for (JsonCatalog jsonCatalog: JsonCatalog.values()) {
            Files.copy(
                    Objects.requireNonNull(TestCatalogFetcher.class.getResourceAsStream(jsonCatalog.getFileName())),
                    serverFilesPath.resolve(jsonCatalog.getFileName())
            );
        }

        server = SimpleFileServer.createFileServer(
                new InetSocketAddress(HOSTNAME, PORT),
                serverFilesPath,
                SimpleFileServer.OutputLevel.INFO
        );
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
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
        Assertions.assertDoesNotThrow(() -> CatalogFetcher.getCatalog(JsonCatalog.VALID_CATALOG.getURI()).get());
    }

    @Test
    void Check_Valid_Catalog_Values() throws ExecutionException, InterruptedException {
        Catalog catalog = CatalogFetcher.getCatalog(JsonCatalog.VALID_CATALOG.getURI()).get();

        Assertions.assertEquals(VALID_CATALOG, catalog);
    }

    @Test
    void Check_Invalid_Catalog() {
        Assertions.assertThrows(
                ExecutionException.class,
                () -> CatalogFetcher.getCatalog(JsonCatalog.INVALID_CATALOG.getURI()).get()
        );
    }
}
