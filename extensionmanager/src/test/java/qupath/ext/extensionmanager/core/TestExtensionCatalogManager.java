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
import qupath.ext.extensionmanager.core.model.CatalogModel;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Some tests of this class needs a JAR file downloadable on GitHub. Currently,
 * <a href="https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar">this file</a>
 * is used because the repository was archived and unlikely to be changed / removed. If that were to happen, some tests and the
 * catalog in the resources directory would need to be changed.
 */
public class TestExtensionCatalogManager {

    private static final String CATALOG_NAME = "catalog.json";
    private static final int JAR_LOADED_TIMEOUT_MS = 10000;
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
    void Check_Creation_When_Extensions_Directory_Observable_Null() {
        ObservableValue<Path> extensionsDirectory = null;
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ExtensionCatalogManager(extensionsDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Creation_When_Extensions_Directory_Null() {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertDoesNotThrow(() -> {
                ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                        extensionsDirectory,
                        classLoader,
                        version,
                        defaultCatalogs
                );
                extensionCatalogManager.close();
        });
    }

    @Test
    void Check_Creation_When_Parent_Class_Loader_Null() throws IOException {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = null;
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertDoesNotThrow(() -> {
            ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                    extensionsDirectory,
                    classLoader,
                    version,
                    defaultCatalogs
            );
            extensionCatalogManager.close();
        });
    }

    @Test
    void Check_Creation_When_Version_Null() throws IOException {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = null;
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ExtensionCatalogManager(extensionsDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Creation_When_Version_Not_Valid() throws IOException {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "invalid_version";
        List<Catalog> defaultCatalogs = createSampleCatalogs();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ExtensionCatalogManager(extensionsDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Creation_When_Default_Catalogs_Null() throws IOException {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = null;

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new ExtensionCatalogManager(extensionsDirectory, classLoader, version, defaultCatalogs)
        );
    }

    @Test
    void Check_Version() throws Exception {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        Version expectedVersion = new Version("v1.2.3");
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
                classLoader,
                expectedVersion.toString(),
                defaultCatalogs
        )) {
            Version version = extensionCatalogManager.getVersion();

            Assertions.assertEquals(expectedVersion, version);
        }
    }

    @Test
    void Check_Extensions_Directory_Path() throws Exception {
        Path expectedExtensionsDirectory = Files.createTempDirectory(null);
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(expectedExtensionsDirectory);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Path extensionsDirectoryValue = extensionCatalogManager.getExtensionsDirectory().getValue();

            Assertions.assertEquals(expectedExtensionsDirectory, extensionsDirectoryValue);
        }
    }

    @Test
    void Check_Extensions_Directory_Path_After_Changed() throws Exception {
        ObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        Path expectedExtensionsDirectory = Files.createTempDirectory(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionsDirectory.set(expectedExtensionsDirectory);
            Path extensionsDirectoryValue = extensionCatalogManager.getExtensionsDirectory().getValue();

            Assertions.assertEquals(expectedExtensionsDirectory, extensionsDirectoryValue);
        }
    }

    @Test
    void Check_Catalog_Directory_With_Null_Catalog_Name() throws Exception {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
    void Check_Catalog_Directory_With_Null_Extensions_Directory() throws Exception {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog firstCatalog = new Catalog(
                "same name",
                "some description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        Catalog secondCatalog = new Catalog(
                "same name",
                "some other description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
    void Check_Catalog_Addition_When_Extensions_Directory_Null() throws Exception {
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "name",
                "description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
        ObservableValue<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "name",
                "description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        List<Catalog> expectedCatalogs = Stream.concat(defaultCatalogs.stream(), Stream.of(catalog)).toList();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(catalog);

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Only_Catalogs_From_Default_Registry_When_Extensions_Directory_Changed() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "some name",
                "some description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            extensionCatalogManager.addCatalog(catalog);
            extensionsDirectory.set(Files.createTempDirectory(null));

            List<Catalog> catalogs = extensionCatalogManager.getCatalogs();

            TestUtils.assertCollectionsEqualsWithoutOrder(defaultCatalogs, catalogs);
        }
    }

    @Test
    void Check_Undeletable_Catalog_Not_Removed() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "some name",
                "some description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
    void Check_Catalog_Removal_When_Catalog_Null() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                new CatalogModel("some name", "some description", List.of()),
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME),
                true
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
    void Check_Extensions_Directory_When_Catalog_Name_Is_Null() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = null;
        String extensionName = "extension";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
    void Check_Extensions_Directory_When_Extension_Name_Is_Null() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        String extensionName = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
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
    void Check_Extensions_Directory_Not_Null() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        String catalogName = "catalog";
        String extensionName = "extension";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertNotNull(extensionCatalogManager.getExtensionDirectory(catalogName, extensionName));
        }
    }

    @Test
    void Check_Download_Links_With_Null_Release() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
        Release release = null;
        boolean installOptionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionsDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(release, installOptionalDependencies)
            );
        }
    }

    @Test
    void Check_Download_Links_With_Null_Extensions_Directory() throws Exception {
        SimpleObjectProperty<Path> extensionsDirectory = new SimpleObjectProperty<>(null);
        ClassLoader classLoader = TestExtensionCatalogManager.class.getClassLoader();
        String version = "v1.2.3";
        List<Catalog> defaultCatalogs = createSampleCatalogs();
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
                extensionsDirectory,
                classLoader,
                version,
                defaultCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(release, installOptionalDependencies)
            );
        }
    }

    @Test
    void Check_Download_Links_With_Optional_Dependencies() throws Exception {
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/main"),
                List.of(URI.create("https://github.com/required1"), URI.create("https://github.com/required2")),
                List.of(URI.create("https://github.com/optional1"), URI.create("https://github.com/optional2")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        ));
        List<URI> expectedDownloadLinks = List.of(
                URI.create("https://github.com/main"),
                URI.create("https://github.com/required1"),
                URI.create("https://github.com/required2"),
                URI.create("https://github.com/optional1"),
                URI.create("https://github.com/optional2")
        );
        boolean optionalDependencies = true;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            List<URI> downloadLinks = extensionCatalogManager.getDownloadLinks(release, optionalDependencies);

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedDownloadLinks,
                    downloadLinks
            );
        }
    }

    @Test
    void Check_Download_Links_Without_Optional_Dependencies() throws Exception {
        Release release = new Release(new ReleaseModel(
                "v0.1.2",
                URI.create("https://github.com/main"),
                List.of(URI.create("https://github.com/required1"), URI.create("https://github.com/required2")),
                List.of(URI.create("https://github.com/optional1"), URI.create("https://github.com/optional2")),
                null,
                new VersionRangeModel("v0.0.0", null, null)
        ));
        List<URI> expectedDownloadLinks = List.of(
                URI.create("https://github.com/main"),
                URI.create("https://github.com/required1"),
                URI.create("https://github.com/required2")
        );
        boolean optionalDependencies = false;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleCatalogs()
        )) {
            List<URI> downloadLinks = extensionCatalogManager.getDownloadLinks(release, optionalDependencies);

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedDownloadLinks,
                    downloadLinks
            );
        }
    }

    @Test
    void Check_Extension_Installation_When_Unknown_Catalog() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "Unknown catalog",
                "Description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_When_Extension_Does_Not_Belong_To_Catalog() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = new Extension(
                new ExtensionModel(
                        "Unknown extension",
                        "Description",
                        "Some author",
                        URI.create("https://github.com/qupath/qupath-macOS-extension"),
                        false,
                        List.of(new ReleaseModel(
                                "v0.1.0",
                                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                                null,
                                null,
                                null,
                                new VersionRangeModel("v1.0.0", null, null)
                        ))
                ),
                null,
                false
        );
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_When_Release_Does_Not_Belong_To_Extension() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = new Release(new ReleaseModel(
                "v0.4.6",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                null,
                null,
                new VersionRangeModel("v5.0.0", null, null)
        ));
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_With_Null_Catalog() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = null;
        Extension extension = new Extension(
                getResourceCatalog().extensions().getFirst(),
                null,
                false
        );
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_With_Null_Extension() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = null;
        Release release = catalog.getExtensions().get().getFirst().getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_With_Null_Release() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = null;
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_With_Null_Extensions_Directory() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(null),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_With_Null_On_Progress() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = null;
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_With_Null_On_Status_Changed() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            release,
                            optionalDependencies,
                            onProgress,
                            onStatusChanged
                    )
            );
        }
    }

    @Test
    void Check_Extension_Release_Installed() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
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
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            Assertions.assertEquals(
                    release,
                    extension.getInstalledRelease().getValue().orElse(null)
            );
        }
    }

    @Test
    void Check_Extension_Optional_Dependencies_Installed() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = true;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
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
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            Assertions.assertEquals(
                    optionalDependencies,
                    extension.areOptionalDependenciesInstalled().getValue()
            );
        }
    }

    @Test
    void Check_Extension_Reinstalled_When_Already_Installed() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release firstRelease = extension.getReleases().get(0);
        Release secondRelease = extension.getReleases().get(1);
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    firstRelease,
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    secondRelease,
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            Assertions.assertEquals(
                    secondRelease,
                    extension.getInstalledRelease().getValue().orElse(null)
            );
        }
    }

    @Test
    void Check_Installed_Jars_After_Extension_Installation() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = true;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        List<String> expectedJarNames = List.of(
                "qupath-extension-macos.jar",   // for main URL
                "qupath-extension-macos.jar",   // for required dependency URL
                "qupath-extension-macos.jar"    // for optional dependency URL
        );
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
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedJarNames,
                    extensionCatalogManager.getCatalogManagedInstalledJars().stream()
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toList()
            );
        }
    }

    @Test
    void Check_Jar_Loaded_Runnable_Run_After_Extension_Installation() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = true;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        int expectedNumberOfCalls = 3;      // one jar for main URL, one required dependency, and one optional dependency
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            CountDownLatch countDownLatch = new CountDownLatch(expectedNumberOfCalls);

            extensionCatalogManager.addOnJarLoadedRunnable(countDownLatch::countDown);
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            Assertions.assertTrue(countDownLatch.await(JAR_LOADED_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        String version = "v1.2.3";
        List<UpdateAvailable> expectedUpdates = List.of();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                version,
                sampleCatalogs
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_Update_Available() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        String version = "v2.2.3";
        List<UpdateAvailable> expectedUpdates = List.of(new UpdateAvailable(
                "Some extension",
                new Version("v0.1.0"),
                new Version("v1.0.0")
        ));     // see catalog.json in resources
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                version,
                sampleCatalogs
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_Extension_Removal_When_Unknown_Catalog() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = new Catalog(
                "Unknown catalog",
                "Description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        );
        Extension extension = catalog.getExtensions().get().getFirst();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.removeExtension(catalog, extension)
            );
        }
    }

    @Test
    void Check_Extension_Removal_When_Extension_Does_Not_Belong_To_Catalog() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = new Extension(
                new ExtensionModel(
                        "Unknown extension",
                        "Description",
                        "Some author",
                        URI.create("https://github.com/qupath/qupath-macOS-extension"),
                        false,
                        List.of(new ReleaseModel(
                                "v0.1.0",
                                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                                null,
                                null,
                                null,
                                new VersionRangeModel("v1.0.0", null, null)
                        ))
                ),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.removeExtension(catalog, extension)
            );
        }
    }

    @Test
    void Check_Extension_Removal_With_Null_Catalog() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = null;
        Extension extension = new Extension(
                getResourceCatalog().extensions().getFirst(),
                null,
                false
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.removeExtension(catalog, extension)
            );
        }
    }

    @Test
    void Check_Extension_Removal_With_Null_Extension() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = null;
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.removeExtension(catalog, extension)
            );
        }
    }

    @Test
    void Check_Extension_Removed() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    extension.getReleases().getFirst(),
                    false,
                    progress -> {},
                    (step, res) -> {}
            );

            extensionCatalogManager.removeExtension(catalog, extension);

            Assertions.assertTrue(extension.getInstalledRelease().getValue().isEmpty());
        }
    }

    @Test
    void Check_Extension_Removal_When_Not_Installed() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            extensionCatalogManager.removeExtension(catalog, extension);

            Assertions.assertTrue(extension.getInstalledRelease().getValue().isEmpty());
        }
    }

    @Test
    void Check_Installed_Jars_After_Extension_Removal() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        boolean optionalDependencies = false;
        Consumer<Float> onProgress = progress -> {};
        BiConsumer<ExtensionCatalogManager.InstallationStep, String> onStatusChanged = (step, res) -> {};
        List<String> expectedJarNames = List.of();
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
                    optionalDependencies,
                    onProgress,
                    onStatusChanged
            );

            extensionCatalogManager.removeExtension(catalog, extension);

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedJarNames,
                    extensionCatalogManager.getCatalogManagedInstalledJars().stream()
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toList()
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
                    JAR_LOADED_TIMEOUT_MS       // wait for list to update
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
    void Check_Manually_Installed_Jars_When_Extension_Installed_With_Catalog() throws Exception {
        List<Catalog> sampleCatalogs = createSampleCatalogs();
        Catalog catalog = sampleCatalogs.getFirst();
        Extension extension = catalog.getExtensions().get().getFirst();
        Release release = extension.getReleases().getFirst();
        Path extensionDirectory = Files.createTempDirectory(null);
        List<Path> expectedJars = List.of(
                Files.createFile(extensionDirectory.resolve("lib1.jar")),
                Files.createFile(extensionDirectory.resolve("lib2.jar"))
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(extensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                sampleCatalogs
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    release,
                    false,
                    progress -> {},
                    (step, res) -> {}
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
                getResourceCatalog().name(),
                getResourceCatalog().description(),
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME)
        ));
    }

    private static CatalogModel getResourceCatalog() {
        return new CatalogModel(
                "Some catalog",
                "Some description",
                List.of(new ExtensionModel(
                        "Some extension",
                        "Some extension description",
                        "Some author",
                        URI.create("https://github.com/qupath/qupath-macOS-extension"),
                        false,
                        List.of(
                                new ReleaseModel(
                                        "v0.1.0",
                                        URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                                        List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                                        List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                                        null,
                                        new VersionRangeModel("v1.0.0", null, null)
                                ),
                                new ReleaseModel(
                                        "v1.0.0",
                                        URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                                        null,
                                        null,
                                        null,
                                        new VersionRangeModel("v2.0.0", null, null)
                                )
                        )
                ))
        );
    }
}
