package qupath.ext.extensionmanager.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.SimpleServer;
import qupath.ext.extensionmanager.TestUtils;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.core.model.CatalogModelFetcher;
import qupath.ext.extensionmanager.core.model.ExtensionModel;
import qupath.ext.extensionmanager.core.model.ReleaseModel;
import qupath.ext.extensionmanager.core.model.VersionRangeModel;
import qupath.ext.extensionmanager.core.catalog.UpdateAvailable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Some tests of this class needs a JAR file downloadable on GitHub. Currently,
 * <a href="https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar">this file</a>
 * is used because the repository was archived and unlikely to be changed / removed. If that were to happen, some tests and the
 * catalog in the resources directory would need to be changed.
 */
public class TestExtensionCatalogManager {

    private static final String CATALOG_NAME = "catalog.json";
    private static final int CHANGE_WAITING_TIME_CATALOG_MS = 100;
    private static final int CHANGE_WAITING_TIME_MANUAL_MS = 10000;
    private static SimpleServer server;

    @BeforeAll
    static void setupServer() throws IOException {
        server = new SimpleServer(List.of(new SimpleServer.FileToServe(
                CATALOG_NAME,
                Objects.requireNonNull(TestExtensionCatalogManager.class.getResourceAsStream(CATALOG_NAME))
        )));
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void Check_Creation_When_Extension_Directory_Observable_Null() {
        ObservableValue<Path> extensionDirectory = null;
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ExtensionCatalogManager(extensionDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Creation_When_Extension_Directory_Null() {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertDoesNotThrow(() -> {
                ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                        extensionDirectory,
                        classLoader,
                        version,
                        defaultCatalogs
                );
                extensionCatalogManager.close();
        });
    }

    @Test
    void Check_Creation_When_Parent_Class_Loader_Null() throws IOException {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = null;
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertDoesNotThrow(() -> {
            ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                    extensionDirectory,
                    classLoader,
                    version,
                    defaultCatalogs
            );
            extensionCatalogManager.close();
        });
    }

    @Test
    void Check_Creation_When_Version_Null() throws IOException {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = null;
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ExtensionCatalogManager(extensionDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Creation_When_Version_Not_Valid() throws IOException {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "invalid_version";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ExtensionCatalogManager(extensionDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Creation_When_Default_Catalogs_Null() throws IOException {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = null;

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ExtensionCatalogManager(extensionDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Version() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        Version expectedVersion = new Version("v1.2.3");
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                expectedVersion.toString(),
                defaultCatalogs
        )) {
            Version version = extensionCatalogManager.getVersion();

            Assertions.assertEquals(expectedVersion, version);
        }
    }

    @Test
    void Check_Extension_Directory_Path() throws Exception {
        Path expectedExtensionDirectory = Files.createTempDirectory(null);
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(expectedExtensionDirectory);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Path extensionDirectoryValue = extensionCatalogManager.getExtensionDirectory().getValue();

            Assertions.assertEquals(expectedExtensionDirectory, extensionDirectoryValue);
        }
    }

    @Test
    void Check_Extension_Directory_Path_After_Changed() throws Exception {
        ObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        Path expectedExtensionDirectory = Files.createTempDirectory(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionDirectory.set(expectedExtensionDirectory);
            Path extensionDirectoryValue = extensionCatalogManager.getExtensionDirectory().getValue();

            Assertions.assertEquals(expectedExtensionDirectory, extensionDirectoryValue);
        }
    }

    @Test
    void Check_Catalog_Directory_With_Null_Catalog_Name() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getCatalogDirectory(catalogName)
            );
        }
    }

    @Test
    void Check_Catalog_Directory_With_Null_Extension_Directory() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getCatalogDirectory(catalogName)
            );
        }
    }

    @Test
    void Check_Catalog_Directory_Not_Null() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Path catalogDirectory = extensionCatalogManager.getCatalogDirectory(catalogName);

            Assertions.assertNotNull(catalogDirectory);
        }
    }

    @Test
    void Check_Catalog_With_Existing_Name_Not_Added() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog firstCatalog = new Catalog(
                "same name",
                "some description",
                URI.create("http://uri1.com"),
                URI.create("http://raw1.com")
        );
        Catalog secondCatalog = new Catalog(
                "same name",
                "some other description",
                URI.create("http://uri2.com"),
                URI.create("http://raw2.com")
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(firstCatalog);

            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.addCatalog(secondCatalog)
            );
        }
    }

    @Test
    void Check_Catalog_Addition_When_Extension_Directory_Null() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://uri.com"),
                URI.create("http://raw.com")
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.addCatalog(catalog)
            );
        }
    }

    @Test
    void Check_Catalog_Addition_When_Null() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.addCatalog(catalog)
            );
        }
    }

    @Test
    void Check_Catalog_Added() throws Exception {
        ObservableValue<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://uri.com"),
                URI.create("http://raw.com")
        );
        List<Catalog> expectedCatalogs = Stream.concat(defaultCatalogs.stream(), Stream.of(catalog)).toList();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(catalog);

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Only_Catalogs_From_Default_Registry_When_Extension_Directory_Changed() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "some name",
                "some description",
                URI.create("http://uri.com"),
                URI.create("http://raw.com")
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(catalog);
            extensionDirectory.set(Files.createTempDirectory(null));

            List<Catalog> catalogs = extensionCatalogManager.getCatalogs();

            TestUtils.assertCollectionsEqualsWithoutOrder(defaultCatalogs, catalogs);
        }
    }

    @Test
    void Check_Undeletable_Catalog_Not_Removed() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "some name",
                "some description",
                URI.create("http://uri.com"),
                URI.create("http://raw.com")
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(catalog);

            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.removeCatalog(catalog)
            );
        }
    }

    @Test
    void Check_Catalog_Removal_When_Extension_Directory_Null() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "some name",
                "some description",
                URI.create("http://uri.com"),
                URI.create("http://raw.com")
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(catalog);

            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.removeCatalog(catalog)
            );
        }
    }

    @Test
    void Check_Catalog_Removal_When_Catalog_Null() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.removeCatalog(catalog)
            );
        }
    }

    @Test
    void Check_Catalog_Removed() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "some name",
                "some description",
                URI.create("http://uri.com"),
                URI.create("http://raw.com")
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(catalog);

            extensionCatalogManager.removeCatalog(catalog);

            TestUtils.assertCollectionsEqualsWithoutOrder(defaultCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Extension_Directory_When_Catalog_Name_Is_Null() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = null;
        String extensionName = "extension";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getExtensionDirectory(catalogName, extensionName)
            );
        }
    }

    @Test
    void Check_Extension_Directory_When_Extension_Name_Is_Null() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        String extensionName = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getExtensionDirectory(catalogName, extensionName)
            );
        }
    }

    @Test
    void Check_Extension_Directory_Not_Null() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        String extensionName = "extension";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertNotNull(extensionCatalogManager.getExtensionDirectory(catalogName, extensionName));
        }
    }

    @Test
    void Check_Download_Links_With_Null_Catalog_Name() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = null;
        String extensionName = "extension";
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/main"),
                List.of(),
                List.of(),
                List.of(),
                new VersionRangeModel("v0.0.0", null, null)
        ));
        boolean installOptionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalogName, extensionName, release, installOptionalDependencies)
            );
        }
    }

    @Test
    void Check_Download_Links_With_Null_Extension_Name() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        String extensionName = null;
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/main"),
                List.of(),
                List.of(),
                List.of(),
                new VersionRangeModel("v0.0.0", null, null)
        ));
        boolean installOptionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalogName, extensionName, release, installOptionalDependencies)
            );
        }
    }

    @Test
    void Check_Download_Links_With_Null_Release() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        String extensionName = "extension";
        Release release = null;
        boolean installOptionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalogName, extensionName, release, installOptionalDependencies)
            );
        }
    }

    @Test
    void Check_Download_Links_With_Null_Extension_Directory() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        String extensionName = "extension";
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/main"),
                List.of(),
                List.of(),
                List.of(),
                new VersionRangeModel("v0.0.0", null, null)
        ));
        boolean installOptionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalogName, extensionName, release, installOptionalDependencies)
            );
        }
    }

    //TODO: refactor from here

    @Test
    void Check_Download_Links() throws Exception {
        List<URI> expectedDownloadLinks = List.of(
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")
        );
        String catalogName = "name";
        String extensionName = "name";
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        ));
        boolean optionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            List<URI> downloadLinks = extensionCatalogManager.getDownloadLinks(catalogName, extensionName, release, optionalDependencies);

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedDownloadLinks,
                    downloadLinks
            );
        }
    }

    @Test
    void Check_Download_Links_Cannot_Be_Retrieved_When_Desired_Release_Does_Not_Exist() throws Exception {
        String catalogName = "name";
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        ));
        String extensionName = "name";
        boolean optionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalogName, extensionName, release, optionalDependencies)
            );
        }
    }

    @Test
    void Check_Download_Links_Cannot_Be_Retrieved_When_Extension_Directory_Null() throws Exception {
        String catalogName = "name";
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        ));
        String extensionName = "name";
        boolean optionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(null),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalogName, extensionName, release, optionalDependencies)
            );
        }
    }

    @Test
    void Check_Extension_Installed() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            Assertions.assertEquals(
                    release,
                    extension.getInstalledRelease().getValue().orElse(null)
            );
        }
    }

    @Test
    void Check_Extension_Reinstalled_When_Already_Installed() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel firstReleaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release firstRelease = new Release(firstReleaseModel);
        ReleaseModel secondReleaseModel = new ReleaseModel(
                "v0.1.3",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release secondRelease = new Release(secondReleaseModel);
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(firstReleaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    firstRelease,
                    false,
                    progress -> {},
                    (step, resource) -> {}
            );
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    secondRelease,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            Assertions.assertEquals(
                    secondRelease,
                    extension.getInstalledRelease().getValue().orElse(null)
            );
        }
    }

    @Test
    void Check_Extension_Installation_Fails_When_Desired_Release_Does_Not_Exist() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        Release release = new Release(new ReleaseModel(
                "v0.1.3",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        ));
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of()
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            false,
                            progress -> {},
                            (step, resource) -> {}
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_Fails_When_Extension_Directory_Null() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(null),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            false,
                            progress -> {},
                            (step, resource) -> {}
                    )
            );
        }
    }

    @Test
    void Check_Extension_Removed_After_Changing_Extension_Directory() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            extensionDirectory.set(Files.createTempDirectory(null));

            Assertions.assertTrue(extension.getInstalledRelease().getValue().isEmpty());
        }
    }

    @Test
    void Check_Installed_Jars_After_Extension_Installation() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        List<String> expectedJarNames = List.of("qupath-extension-macos.jar", "qupath-extension-macos.jar");
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    false,
                    progress -> {},
                    (step, resource) -> {}
            );

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedJarNames,
                    extensionCatalogManager.getCatalogManagedInstalledJars().stream()
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toList(),
                    CHANGE_WAITING_TIME_CATALOG_MS      // wait for list to update
            );
        }
    }

    @Test
    void Check_Installed_Jars_After_Extension_Manually_Installed() throws Exception {
        Path extensionDirectory = Files.createTempDirectory(null);
        Files.createFile(extensionDirectory.resolve("lib.jar"));
        List<Path> expectedJars = List.of();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(extensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedJars,
                    extensionCatalogManager.getCatalogManagedInstalledJars()
            );
        }
    }

    @Test
    void Check_Jar_Loaded_Runnable_Run_After_Extension_Installation() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        int expectedNumberOfCalls = 2;
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            AtomicInteger numberOfJarLoaded = new AtomicInteger();
            extensionCatalogManager.addOnJarLoadedRunnable(numberOfJarLoaded::getAndIncrement);
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    false,
                    progress -> {},
                    (step, resource) -> {}
            );

            Thread.sleep(CHANGE_WAITING_TIME_CATALOG_MS);     // wait for list to update
            Assertions.assertEquals(expectedNumberOfCalls, numberOfJarLoaded.get());
        }
    }

    @Test
    void Check_No_Available_Updates_When_No_Extension_Installed() throws Exception {
        List<UpdateAvailable> expectedUpdates = List.of();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_No_Available_Updates_When_Incompatible_Version() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = new Extension(
                CatalogModelFetcher.getCatalog(catalog.getRawUri()).get().extensions().getFirst(),
                null,
                false
        );
        Release release = extension.getReleases().getFirst();
        List<UpdateAvailable> expectedUpdates = List.of();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_Update_Available() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = new Extension(
                CatalogModelFetcher.getCatalog(catalog.getRawUri()).get().extensions().getFirst(),
                null,
                false
        );
        Release release = extension.getReleases().getFirst();
        List<UpdateAvailable> expectedUpdates = List.of(new UpdateAvailable(
                "Some extension",
                new Version("v0.1.0"),
                new Version("v1.0.0")
        ));     // see catalog.json in resources
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v2.2.3",
                sampleCatalogs
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_Extension_Removed() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            extensionCatalogManager.removeExtension(catalog, extension);

            Assertions.assertTrue(extension.getInstalledRelease().getValue().isEmpty());
        }
    }

    @Test
    void Check_Extension_Removal_When_Not_Installed() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel release = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(release)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.removeExtension(catalog, extension);

            Assertions.assertTrue(extension.getInstalledRelease().getValue().isEmpty());
        }
    }

    @Test
    void Check_Installed_Jars_After_Extension_Removal() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        List<String> expectedJarNames = List.of();
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            extensionCatalogManager.removeExtension(catalog, extension);

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedJarNames,
                    extensionCatalogManager.getCatalogManagedInstalledJars().stream()
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toList(),
                    CHANGE_WAITING_TIME_CATALOG_MS      // wait for list to update
            );
        }
    }

    @Test
    void Check_Manually_Installed_Jars_When_Two_Jars_Added_Before_Manager_Creation() throws Exception {
        Path extensionDirectory = Files.createTempDirectory(null);
        List<Path> expectedJars = List.of(
                Files.createFile(extensionDirectory.resolve("lib1.jar")),
                Files.createFile(extensionDirectory.resolve("lib2.jar"))
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(extensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            List<Path> jars = extensionCatalogManager.getManuallyInstalledJars();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedJars, jars);
        }
    }

    @Test
    void Check_Manually_Installed_Jars_When_Two_Jars_Added_After_Manager_Creation() throws Exception {
        Path extensionDirectory = Files.createTempDirectory(null);
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(extensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            List<Path> expectedJars = List.of(
                    Files.createFile(extensionDirectory.resolve("lib1.jar")),
                    Files.createFile(extensionDirectory.resolve("lib2.jar"))
            );

            List<Path> jars = extensionCatalogManager.getManuallyInstalledJars();

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedJars,
                    jars,
                    CHANGE_WAITING_TIME_MANUAL_MS       // wait for list to update
            );
        }
    }

    @Test
    void Check_Manually_Installed_Jars_When_Non_Jar_File_Added() throws Exception {
        Path extensionDirectory = Files.createTempDirectory(null);
        Files.createFile(extensionDirectory.resolve("file.notJar"));
        List<Path> expectedJars = List.of(
                Files.createFile(extensionDirectory.resolve("lib1.jar")),
                Files.createFile(extensionDirectory.resolve("lib2.jar"))
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(extensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            List<Path> jars = extensionCatalogManager.getManuallyInstalledJars();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedJars, jars);
        }
    }

    @Test
    void Check_Manually_Installed_Jars_When_Extension_Installed_With_Index() throws Exception {
        Catalog catalog = new Catalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test")
        );
        ReleaseModel releaseModel = new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        );
        Release release = new Release(releaseModel);
        Extension extension = new Extension(
                new ExtensionModel(
                        "name",
                        "description",
                        "author",
                        URI.create("https://github.com"),
                        false,
                        List.of(releaseModel)
                ),
                null,
                false
        );
        Path extensionDirectory = Files.createTempDirectory(null);
        List<Path> expectedJars = List.of(
                Files.createFile(extensionDirectory.resolve("lib1.jar")),
                Files.createFile(extensionDirectory.resolve("lib2.jar"))
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(extensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    true,
                    progress -> {},
                    (step, resource) -> {}
            );

            List<Path> jars = extensionCatalogManager.getManuallyInstalledJars();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedJars, jars);
        }
    }

    @Test
    void Check_Manually_Installed_Jars_When_Extension_Directory_Changed() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        Files.createFile(extensionDirectory.get().resolve("lib1.jar"));
        Files.createFile(extensionDirectory.get().resolve("lib2.jar"));
        List<Path> expectedJars = List.of();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            extensionDirectory.set(Files.createTempDirectory(null));

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedJars, extensionCatalogManager.getManuallyInstalledJars());
        }
    }

    private static List<Catalog> createSampleCatalogs() {
        return List.of(new Catalog(
                "Some catalog",
                "Some description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        ));
    }
}
