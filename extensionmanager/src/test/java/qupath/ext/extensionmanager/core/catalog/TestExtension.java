package qupath.ext.extensionmanager.core.catalog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.TestUtils;
import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.model.ExtensionModel;
import qupath.ext.extensionmanager.core.model.ReleaseModel;
import qupath.ext.extensionmanager.core.model.VersionRangeModel;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class TestExtension {

    @Test
    void Check_Null_Extension_Model() {
        ExtensionModel extensionModel = null;
        Release installedRelease = new Release(new ReleaseModel(
                "v1.0.0",
                URI.create("http://github.com/1.0.0"),
                List.of(),
                List.of(),
                List.of(),
                new VersionRangeModel("v1.0.0", null, null)
        ));
        boolean optionalDependenciesInstalled = true;

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new Extension(extensionModel, installedRelease, optionalDependenciesInstalled)
        );
    }

    @Test
    void Check_Name() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        String expectedName = "name";

        String name = extension.getName();

        Assertions.assertEquals(expectedName, name);
    }

    @Test
    void Check_Description() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        String expectedDescription = "description";

        String description = extension.getDescription();

        Assertions.assertEquals(expectedDescription, description);
    }

    @Test
    void Check_Releases() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        List<Release> expectedReleases = List.of(
                new Release(new ReleaseModel(
                        "v0.1.0",
                        URI.create("http://github.com/0.1.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                )),
                new Release(new ReleaseModel(
                        "v0.2.0",
                        URI.create("http://github.com/0.2.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                ))
        );

        List<Release> releases = extension.getReleases();

        TestUtils.assertCollectionsEqualsWithoutOrder(expectedReleases, releases);
    }

    @Test
    void Check_Homepage() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        URI expectedHomepage = URI.create("http://github.com/extension");

        URI homepage = extension.getHomepage();

        Assertions.assertEquals(expectedHomepage, homepage);
    }

    @Test
    void Check_Starred() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        boolean expectedStarred = true;

        boolean starred = extension.isStarred();

        Assertions.assertEquals(expectedStarred, starred);
    }

    @Test
    void Check_Installed_Release_When_Not_Installed() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );

        Optional<Release> installedRelease = extension.getInstalledRelease().getValue();

        Assertions.assertTrue(installedRelease.isEmpty());
    }

    @Test
    void Check_Installed_Release_When_Installed() {
        Release expectedInstalledRelease = new Release(new ReleaseModel(
                "v0.2.0",
                URI.create("http://github.com/0.2.0"),
                List.of(),
                List.of(),
                List.of(),
                new VersionRangeModel("v0.1.0", "v0.2.0", null)
        ));
        Extension extension = new Extension(
                createExtensionModel(),
                expectedInstalledRelease,
                false
        );

        Release installedRelease = extension.getInstalledRelease().getValue().orElse(null);

        Assertions.assertEquals(expectedInstalledRelease, installedRelease);
    }

    @Test
    void Check_Installed_Release_When_Installed_After_Initialization() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        Release expectedInstalledRelease = new Release(new ReleaseModel(
                "v0.2.0",
                URI.create("http://github.com/0.2.0"),
                List.of(),
                List.of(),
                List.of(),
                new VersionRangeModel("v0.1.0", "v0.2.0", null)
        ));
        extension.installRelease(expectedInstalledRelease, false);

        Release installedRelease = extension.getInstalledRelease().getValue().orElse(null);

        Assertions.assertEquals(expectedInstalledRelease, installedRelease);
    }

    @Test
    void Check_Installed_Release_When_Uninstalled() {
        Extension extension = new Extension(
                createExtensionModel(),
                new Release(new ReleaseModel(
                        "v0.2.0",
                        URI.create("http://github.com/0.2.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                )),
                false
        );
        extension.uninstallRelease();

        Optional<Release> installedRelease = extension.getInstalledRelease().getValue();

        Assertions.assertTrue(installedRelease.isEmpty());
    }

    @Test
    void Check_Optional_Dependencies_Installed_When_Not_Installed() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );

        boolean optionalDependenciesInstalled = extension.areOptionalDependenciesInstalled().getValue();

        Assertions.assertFalse(optionalDependenciesInstalled);
    }

    @Test
    void Check_Optional_Dependencies_Installed_When_Installed() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                true
        );

        boolean optionalDependenciesInstalled = extension.areOptionalDependenciesInstalled().getValue();

        Assertions.assertTrue(optionalDependenciesInstalled);
    }

    @Test
    void Check_Optional_Dependencies_Installed_When_Installed_After_Initialization() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        extension.installRelease(
                new Release(new ReleaseModel(
                        "v0.2.0",
                        URI.create("http://github.com/0.2.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                )),
                true
        );

        boolean optionalDependenciesInstalled = extension.areOptionalDependenciesInstalled().getValue();

        Assertions.assertTrue(optionalDependenciesInstalled);
    }

    @Test
    void Check_Optional_Dependencies_Installed_When_Not_Installed_After_Initialization() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        extension.installRelease(
                new Release(new ReleaseModel(
                        "v0.2.0",
                        URI.create("http://github.com/0.2.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                )),
                false
        );

        boolean optionalDependenciesInstalled = extension.areOptionalDependenciesInstalled().getValue();

        Assertions.assertFalse(optionalDependenciesInstalled);
    }

    @Test
    void Check_Optional_Dependencies_Installed_When_Uninstalled() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                true
        );
        extension.uninstallRelease();

        boolean optionalDependenciesInstalled = extension.areOptionalDependenciesInstalled().getValue();

        Assertions.assertFalse(optionalDependenciesInstalled);
    }

    @Test
    void Check_No_Available_Updates_When_Extension_Not_Installed() {
        Extension extension = new Extension(
                createExtensionModel(),
                null,
                false
        );
        Version version = new Version("v0.1.0");

        Optional<UpdateAvailable> updateAvailable = extension.getUpdateAvailable(version);

        Assertions.assertTrue(updateAvailable.isEmpty());
    }

    @Test
    void Check_No_Available_Updates_When_No_Compatible_Release() {
        Extension extension = new Extension(
                createExtensionModel(),
                new Release(new ReleaseModel(
                        "v0.1.0",
                        URI.create("http://github.com/0.1.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                )),
                false
        );
        Version version = new Version("v0.30.0");

        Optional<UpdateAvailable> updateAvailable = extension.getUpdateAvailable(version);

        Assertions.assertTrue(updateAvailable.isEmpty());
    }

    @Test
    void Check_No_Available_Updates_When_Already_Latest_Release() {
        Extension extension = new Extension(
                createExtensionModel(),
                new Release(new ReleaseModel(
                        "v0.2.0",
                        URI.create("http://github.com/0.2.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                )),
                false
        );
        Version version = new Version("v0.1.0");

        Optional<UpdateAvailable> updateAvailable = extension.getUpdateAvailable(version);

        Assertions.assertTrue(updateAvailable.isEmpty());
    }

    @Test
    void Check_Available_Update() {
        Extension extension = new Extension(
                createExtensionModel(),
                new Release(new ReleaseModel(
                        "v0.1.0",
                        URI.create("http://github.com/0.1.0"),
                        List.of(),
                        List.of(),
                        List.of(),
                        new VersionRangeModel("v0.1.0", "v0.2.0", null)
                )),
                false
        );
        Version version = new Version("v0.1.0");
        UpdateAvailable expectedUpdateAvailable = new UpdateAvailable(
                "name",
                new Version("v0.1.0"),
                new Version("v0.2.0")
        );

        UpdateAvailable updateAvailable = extension.getUpdateAvailable(version).orElse(null);

        Assertions.assertEquals(expectedUpdateAvailable, updateAvailable);
    }

    private static ExtensionModel createExtensionModel() {
        return new ExtensionModel(
                "name",
                "description",
                "author",
                URI.create("http://github.com/extension"),
                true,
                List.of(
                        new ReleaseModel(
                                "v0.1.0",
                                URI.create("http://github.com/0.1.0"),
                                List.of(),
                                List.of(),
                                List.of(),
                                new VersionRangeModel("v0.1.0", "v0.2.0", null)
                        ),
                        new ReleaseModel(
                                "v0.2.0",
                                URI.create("http://github.com/0.2.0"),
                                List.of(),
                                List.of(),
                                List.of(),
                                new VersionRangeModel("v0.1.0", "v0.2.0", null)
                        )
                )
        );
    }
}
