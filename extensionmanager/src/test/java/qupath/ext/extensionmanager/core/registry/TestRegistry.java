package qupath.ext.extensionmanager.core.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.SimpleServer;
import qupath.ext.extensionmanager.core.catalog.Catalog;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class TestRegistry {

    @Test
    void Check_Null_Catalogs() {
        List<RegistryCatalog> catalogs = null;

        Assertions.assertThrows(
                RuntimeException.class,
                () -> new Registry(catalogs)
        );
    }

    @Test
    void Check_Creation_From_Catalogs() throws ExecutionException, InterruptedException, IOException {
        List<String> catalogNames = List.of("catalog1.json", "catalog2.json");
        SimpleServer server = new SimpleServer(catalogNames.stream()
                .map(catalogName -> new SimpleServer.FileToServe(
                        catalogName,
                        Objects.requireNonNull(TestRegistry.class.getResourceAsStream(catalogName))
                ))
                .toList()
        );
        List<Catalog> catalogs = List.of(
                new Catalog(
                        "Some catalog 1",
                        "Some description 1",
                        server.getURI("catalog1.json"),
                        server.getURI("catalog1.json")
                ),
                new Catalog(
                        "Some catalog 2",
                        "Some description 2",
                        server.getURI("catalog2.json"),
                        server.getURI("catalog2.json")
                )
        );
        Registry expectedRegistry = new Registry(List.of(
                new RegistryCatalog(
                        "Some catalog 1",
                        "Some description 1",
                        server.getURI("catalog1.json"),
                        server.getURI("catalog1.json"),
                        false,
                        List.of()
                ),
                new RegistryCatalog(
                        "Some catalog 2",
                        "Some description 2",
                        server.getURI("catalog2.json"),
                        server.getURI("catalog2.json"),
                        false,
                        List.of()
                )
        ));

        Registry registry = Registry.createFromCatalogs(catalogs).get();

        Assertions.assertEquals(expectedRegistry, registry);

        server.close();
    }
}
