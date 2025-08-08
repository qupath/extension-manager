package qupath.ext.extensionmanager.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.SimpleServer;
import qupath.ext.extensionmanager.TestUtils;
import qupath.ext.extensionmanager.core.catalog.CatalogFetcher;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.core.catalog.VersionRange;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.Registry;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.core.savedentities.UpdateAvailable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private static final int CHANGE_WAITING_TIME_MS = 10000;
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
    void Check_Creation_When_Extension_Directory_Null() {
        Assertions.assertDoesNotThrow(() -> {
                ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                        new SimpleObjectProperty<>(null),
                        TestExtensionCatalogManager.class.getClassLoader(),
                        "v1.2.3",
                        createSampleRegistry()
                );
                extensionCatalogManager.close();
        });
    }

    @Test
    void Check_Creation_When_Extension_Directory_Property_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                    ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                            null,
                            TestExtensionCatalogManager.class.getClassLoader(),
                            "v1.2.3",
                            createSampleRegistry()
                    );
                    extensionCatalogManager.close();
                }
        );
    }

    @Test
    void Check_Creation_When_Parent_Class_Loader_Null() {
        Assertions.assertDoesNotThrow(() -> {
            ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                    new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                    null,
                    "v1.2.3",
                    createSampleRegistry()
            );
            extensionCatalogManager.close();
        });
    }

    @Test
    void Check_Creation_When_Version_Null() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> {
                    ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                            new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                            TestExtensionCatalogManager.class.getClassLoader(),
                            null,
                            createSampleRegistry()
                    );
                    extensionCatalogManager.close();
                }
        );
    }

    @Test
    void Check_Creation_When_Version_Not_Valid() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                            new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                            TestExtensionCatalogManager.class.getClassLoader(),
                            "invalid_version",
                            createSampleRegistry()
                    );
                    extensionCatalogManager.close();
                }
        );
    }

    @Test
    void Check_Creation_When_Default_Registry_Null() {
        Assertions.assertDoesNotThrow(() -> {
            ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                    new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                    TestExtensionCatalogManager.class.getClassLoader(),
                    "v1.2.3",
                    null
            );
            extensionCatalogManager.close();
        });
    }

    @Test
    void Check_Extension_Directory_Path() throws Exception {
        Path expectedExtensionDirectory = Files.createTempDirectory(null);
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(expectedExtensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Path extensionDirectory = extensionCatalogManager.getExtensionDirectoryPath().get();

            Assertions.assertEquals(expectedExtensionDirectory, extensionDirectory);
        }
    }

    @Test
    void Check_Extension_Directory_Path_After_Changed() throws Exception {
        Path firstExtensionDirectory = Files.createTempDirectory(null);
        Path expectedExtensionDirectory = Files.createTempDirectory(null);
        ObjectProperty<Path> extensionDirectoryProperty = new SimpleObjectProperty<>(firstExtensionDirectory);
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectoryProperty,
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionDirectoryProperty.set(expectedExtensionDirectory);
            Path extensionDirectory = extensionCatalogManager.getExtensionDirectoryPath().get();

            Assertions.assertEquals(expectedExtensionDirectory, extensionDirectory);
        }
    }

    @Test
    void Check_Version() throws Exception {
        String expectedVersion = "v1.2.4";
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                expectedVersion,
                createSampleRegistry()
        )) {
            String version = extensionCatalogManager.getVersion();

            Assertions.assertEquals(expectedVersion, version);
        }
    }

    @Test
    void Check_Catalog_Directory_When_Catalog_Is_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getCatalogDirectory(null)
            );
        }
    }

    @Test
    void Check_Catalog_Directory_Not_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Path catalogDirectory = extensionCatalogManager.getCatalogDirectory(new SavedCatalog(
                    "some name",
                    "some description",
                    URI.create("http://test"),
                    URI.create("http://test"),
                    true
            ));

            Assertions.assertNotNull(catalogDirectory);
        }
    }

    @Test
    void Check_Catalog_Addition_When_Extension_Directory_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(null),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.addCatalog(List.of())
            );
        }
    }

    @Test
    void Check_Catalog_Addition_When_List_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.addCatalog(null)
            );
        }
    }

    @Test
    void Check_Catalog_Addition_When_One_Null() throws Exception {
        List<SavedCatalog> catalogsToAdd = new ArrayList<>();
        catalogsToAdd.add(new SavedCatalog(
                "some other name",
                "some other description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        ));
        catalogsToAdd.add(null);
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.addCatalog(catalogsToAdd)
            );
        }
    }

    @Test
    void Check_Catalog_Added() throws Exception {
        Registry defaultRegistry = createSampleRegistry();
        SavedCatalog catalogToAdd = new SavedCatalog(
                "some other name",
                "some other description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        List<SavedCatalog> expectedCatalogs = Stream.concat(
                defaultRegistry.catalogs().stream(),
                Stream.of(catalogToAdd)
        ).toList();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                defaultRegistry
        )) {
            extensionCatalogManager.addCatalog(List.of(catalogToAdd));

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Catalog_Addition_When_Same_Name() throws Exception {
        SavedCatalog firstCatalog = new SavedCatalog(
                "same name",
                "some description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        SavedCatalog secondCatalog = new SavedCatalog(
                "same name",
                "some other description",
                URI.create("http://test/other"),
                URI.create("http://test/other"),
                true
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                null
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.addCatalog(List.of(firstCatalog, secondCatalog))
            );
        }
    }

    @Test
    void Check_Catalog_With_Existing_Name_Not_Added() throws Exception {
        SavedCatalog firstCatalog = new SavedCatalog(
                "same name",
                "some description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        SavedCatalog secondCatalog = new SavedCatalog(
                "same name",
                "some other description",
                URI.create("http://test/other"),
                URI.create("http://test/other"),
                true
        );
        List<SavedCatalog> expectedCatalogs = List.of(firstCatalog);
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                null
        )) {
            extensionCatalogManager.addCatalog(List.of(firstCatalog));
            extensionCatalogManager.addCatalog(List.of(secondCatalog));

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Only_Catalogs_From_Default_Registry_When_Extension_Directory_Changed() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        SavedCatalog catalogToAdd = new SavedCatalog(
                "some other name",
                "some other description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Registry defaultRegistry = createSampleRegistry();
        List<SavedCatalog> expectedCatalogs = defaultRegistry.catalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                defaultRegistry
        )) {
            extensionCatalogManager.addCatalog(List.of(catalogToAdd));

            extensionDirectory.set(Files.createTempDirectory(null));

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Catalog_Deletion_When_Extension_Directory_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(null),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.removeCatalogs(List.of(), true)
            );
        }
    }

    @Test
    void Check_Catalog_Deletion_When_List_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.removeCatalogs(null, true)
            );
        }
    }

    @Test
    void Check_Catalog_Deletion_When_One_Null() throws Exception {
        List<SavedCatalog> catalogsToRemove = new ArrayList<>();
        catalogsToRemove.add(new SavedCatalog(
                "some other name",
                "some other description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        ));
        catalogsToRemove.add(null);
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.removeCatalogs(catalogsToRemove, true)
            );
        }
    }

    @Test
    void Check_Catalog_Removed() throws Exception {
        Registry defaultRegistry = createSampleRegistry();
        SavedCatalog catalogToAdd = new SavedCatalog(
                "some other name",
                "some other description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        List<SavedCatalog> expectedCatalogs = defaultRegistry.catalogs();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                defaultRegistry
        )) {
            extensionCatalogManager.addCatalog(List.of(catalogToAdd));
            extensionCatalogManager.removeCatalogs(List.of(catalogToAdd), true);

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Undeletable_Catalog_Not_Removed() throws Exception {
        Registry defaultRegistry = createSampleRegistry();
        SavedCatalog catalogToAdd = new SavedCatalog(
                "some other name",
                "some other description",
                URI.create("http://test"),
                URI.create("http://test"),
                false
        );
        List<SavedCatalog> expectedCatalogs = Stream.concat(
                defaultRegistry.catalogs().stream(),
                Stream.of(catalogToAdd)
        ).toList();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                defaultRegistry
        )) {
            extensionCatalogManager.addCatalog(List.of(catalogToAdd));
            extensionCatalogManager.removeCatalogs(List.of(catalogToAdd), true);

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedCatalogs, extensionCatalogManager.getCatalogs());
        }
    }

    @Test
    void Check_Extension_Directory_When_Catalog_Is_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getExtensionDirectory(
                            null,
                            new Extension("", "", "", URI.create("http://github.com"), false, List.of())
                    )
            );
        }
    }

    @Test
    void Check_Extension_Directory_When_Extension_Is_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getExtensionDirectory(
                            new SavedCatalog(
                                    "some name",
                                    "some description",
                                    URI.create("http://test"),
                                    URI.create("http://test"),
                                    true
                            ),
                            null
                    )
            );
        }
    }

    @Test
    void Check_Extension_Directory_Not_Null() throws Exception {
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Path extensionDirectory = extensionCatalogManager.getExtensionDirectory(
                    new SavedCatalog(
                            "some name",
                            "some description",
                            URI.create("http://test"),
                            URI.create("http://test"),
                            true
                    ),
                    new Extension("", "", "", URI.create("http://github.com"), false, List.of())
            );

            Assertions.assertNotNull(extensionDirectory);
        }
    }

    @Test
    void Check_Download_Links() throws Exception {
        List<URI> expectedDownloadLinks = List.of(
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")
        );
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            List<URI> downloadLinks = extensionCatalogManager.getDownloadLinks(catalog, extension, installationInformation);

            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedDownloadLinks,
                    downloadLinks
            );
        }
    }

    @Test
    void Check_Download_Links_Cannot_Be_Retrieved_When_Desired_Release_Does_Not_Exist() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        InstalledExtension installationInformation = new InstalledExtension("invalid_release", false);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of()
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalog, extension, installationInformation)
            );
        }
    }

    @Test
    void Check_Download_Links_Cannot_Be_Retrieved_When_Extension_Directory_Null() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(null),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.getDownloadLinks(catalog, extension, installationInformation)
            );
        }
    }

    @Test
    void Check_Extension_Installed() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    installationInformation,
                    progress -> {},
                    (step, resource) -> {}
            );

            Assertions.assertEquals(
                    installationInformation,
                    extensionCatalogManager.getInstalledExtension(catalog, extension).get().orElse(null)
            );
        }
    }

    @Test
    void Check_Extension_Reinstalled_When_Already_Installed() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        InstalledExtension firstInstallationInformation = new InstalledExtension(release.name(), true);
        InstalledExtension secondInstallationInformation = new InstalledExtension(release.name(), false);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    firstInstallationInformation,
                    progress -> {},
                    (step, resource) -> {}
            );
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    secondInstallationInformation,
                    progress -> {},
                    (step, resource) -> {}
            );

            Assertions.assertEquals(
                    secondInstallationInformation,
                    extensionCatalogManager.getInstalledExtension(catalog, extension).get().orElse(null)
            );
        }
    }

    @Test
    void Check_Extension_Installation_Fails_When_Desired_Release_Does_Not_Exist() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        InstalledExtension installationInformation = new InstalledExtension("invalid_release", false);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of()
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            installationInformation,
                            progress -> {},
                            (step, resource) -> {}
                    )
            );
        }
    }

    @Test
    void Check_Extension_Installation_Fails_When_Extension_Directory_Null() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(null),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            Assertions.assertThrows(
                    NullPointerException.class,
                    () -> extensionCatalogManager.installOrUpdateExtension(
                            catalog,
                            extension,
                            installationInformation,
                            progress -> {},
                            (step, resource) -> {}
                    )
            );
        }
    }

    @Test
    void Check_Extension_Removed_After_Changing_Extension_Directory() throws Exception {
        SimpleObjectProperty<Path> extensionDirectory = new SimpleObjectProperty<>(Files.createTempDirectory(null));
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                extensionDirectory,
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    new InstalledExtension(release.name(), true),
                    progress -> {},
                    (step, resource) -> {}
            );

            extensionDirectory.set(Files.createTempDirectory(null));

            Assertions.assertNull(extensionCatalogManager.getInstalledExtension(catalog, extension).get().orElse(null));
        }
    }

    @Test
    void Check_Installed_Jars_After_Extension_Installation() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        List<String> expectedJarNames = List.of("qupath-extension-macos.jar", "qupath-extension-macos.jar");
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    installationInformation,
                    progress -> {},
                    (step, resource) -> {}
            );

            Thread.sleep(CHANGE_WAITING_TIME_MS);     // wait for list to update
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
    void Check_Installed_Jars_After_Extension_Manually_Installed() throws Exception {
        Path extensionDirectory = Files.createTempDirectory(null);
        Files.createFile(extensionDirectory.resolve("lib.jar"));
        List<Path> expectedJars = List.of();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(extensionDirectory),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            TestUtils.assertCollectionsEqualsWithoutOrder(
                    expectedJars,
                    extensionCatalogManager.getCatalogManagedInstalledJars()
            );
        }
    }

    @Test
    void Check_Jar_Loaded_Runnable_Run_After_Extension_Installation() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        int expectedNumberOfCalls = 2;
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            AtomicInteger numberOfJarLoaded = new AtomicInteger();
            extensionCatalogManager.addOnJarLoadedRunnable(numberOfJarLoaded::getAndIncrement);
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    installationInformation,
                    progress -> {},
                    (step, resource) -> {}
            );

            Thread.sleep(CHANGE_WAITING_TIME_MS);     // wait for list to update
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
                createSampleRegistry()
        )) {
            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_No_Available_Updates_When_Incompatible_Version() throws Exception {
        Registry defaultRegistry = createSampleRegistry();
        SavedCatalog catalog = defaultRegistry.catalogs().getFirst();
        Extension extension = CatalogFetcher.getCatalog(catalog.rawUri()).get().extensions().getFirst();
        Release release = extension.releases().getFirst();
        List<UpdateAvailable> expectedUpdates = List.of();
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                defaultRegistry
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    new InstalledExtension(release.name(), true),
                    progress -> {},
                    (step, resource) -> {}
            );

            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_Update_Available() throws Exception {
        Registry defaultRegistry = createSampleRegistry();
        SavedCatalog catalog = defaultRegistry.catalogs().getFirst();
        Extension extension = CatalogFetcher.getCatalog(catalog.rawUri()).get().extensions().getFirst();
        Release release = extension.releases().getFirst();
        List<UpdateAvailable> expectedUpdates = List.of(new UpdateAvailable(
                "Some extension",
                "v0.1.0",
                "v1.0.0"
        ));     // see catalog.json in resources
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v2.2.3",
                defaultRegistry
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    new InstalledExtension(release.name(), true),
                    progress -> {},
                    (step, resource) -> {}
            );

            List<UpdateAvailable> updates = extensionCatalogManager.getAvailableUpdates().get();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedUpdates, updates);
        }
    }

    @Test
    void Check_Extension_Removed() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    installationInformation,
                    progress -> {},
                    (step, resource) -> {}
            );

            extensionCatalogManager.removeExtension(catalog, extension);

            Assertions.assertNull(extensionCatalogManager.getInstalledExtension(catalog, extension).get().orElse(null));
        }
    }

    @Test
    void Check_Extension_Removal_When_Not_Installed() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionCatalogManager.removeExtension(catalog, extension);

            Assertions.assertNull(extensionCatalogManager.getInstalledExtension(catalog, extension).get().orElse(null));
        }
    }

    @Test
    void Check_Installed_Jars_After_Extension_Removal() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        List<String> expectedJarNames = List.of();
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
        );
        try (ExtensionCatalogManager extensionCatalogManager = new ExtensionCatalogManager(
                new SimpleObjectProperty<>(Files.createTempDirectory(null)),
                TestExtensionCatalogManager.class.getClassLoader(),
                "v1.2.3",
                createSampleRegistry()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    installationInformation,
                    progress -> {},
                    (step, resource) -> {}
            );

            extensionCatalogManager.removeExtension(catalog, extension);

            Thread.sleep(CHANGE_WAITING_TIME_MS);     // wait for list to update
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
                createSampleRegistry()
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
                createSampleRegistry()
        )) {
            List<Path> expectedJars = List.of(
                    Files.createFile(extensionDirectory.resolve("lib1.jar")),
                    Files.createFile(extensionDirectory.resolve("lib2.jar"))
            );

            List<Path> jars = extensionCatalogManager.getManuallyInstalledJars();

            Thread.sleep(CHANGE_WAITING_TIME_MS);     // wait for list to update
            TestUtils.assertCollectionsEqualsWithoutOrder(expectedJars, jars);
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
                createSampleRegistry()
        )) {
            List<Path> jars = extensionCatalogManager.getManuallyInstalledJars();

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedJars, jars);
        }
    }

    @Test
    void Check_Manually_Installed_Jars_When_Extension_Installed_With_Index() throws Exception {
        SavedCatalog catalog = new SavedCatalog(
                "name",
                "description",
                URI.create("http://test"),
                URI.create("http://test"),
                true
        );
        Release release = new Release(
                "v0.1.2",
                URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar"),
                null,
                List.of(URI.create("https://github.com/qupath/qupath-macOS-extension/releases/download/v0.0.1/qupath-extension-macos.jar")),
                null,
                new VersionRange("v0.0.0", null, null)
        );
        InstalledExtension installationInformation = new InstalledExtension(release.name(), true);
        Extension extension = new Extension(
                "name",
                "description",
                "author",
                URI.create("https://github.com"),
                false,
                List.of(release)
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
                createSampleRegistry()
        )) {
            extensionCatalogManager.installOrUpdateExtension(
                    catalog,
                    extension,
                    installationInformation,
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
                createSampleRegistry()
        )) {
            extensionDirectory.set(Files.createTempDirectory(null));

            TestUtils.assertCollectionsEqualsWithoutOrder(expectedJars, extensionCatalogManager.getManuallyInstalledJars());
        }
    }

    private static Registry createSampleRegistry() {
        return new Registry(List.of(new SavedCatalog(
                "Some catalog",
                "Some description",
                server.getURI(CATALOG_NAME),
                server.getURI(CATALOG_NAME),
                true
        )));
    }
}
