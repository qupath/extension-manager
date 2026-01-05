package qupath.ext.extensionmanager.core.catalog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.extensionmanager.TestUtils;
import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.model.ReleaseModel;
import qupath.ext.extensionmanager.core.model.VersionRangeModel;

import java.net.URI;
import java.util.List;

public class TestRelease {

    @Test
    void Check_Null_Release_Model() {
        ReleaseModel releaseModel = null;

        Assertions.assertThrows(NullPointerException.class, () -> new Release(releaseModel));
    }

    @Test
    void Check_Version() {
        Release release = createRelease();
        Version expectedVersion = new Version("v1.0.0");

        Version version = release.getVersion();

        Assertions.assertEquals(expectedVersion, version);
    }

    @Test
    void Check_Compatible_Version() {
        Release release = createRelease();
        Version compatibleVersion = new Version("v0.1.0");

        boolean isCompatible = release.isCompatible(compatibleVersion);

        Assertions.assertTrue(isCompatible);
    }

    @Test
    void Check_Incompatible_Version() {
        Release release = createRelease();
        Version compatibleVersion = new Version("v0.30.0");

        boolean isCompatible = release.isCompatible(compatibleVersion);

        Assertions.assertFalse(isCompatible);
    }

    @Test
    void Check_Main_Url() {
        Release release = createRelease();
        URI expectedMainUrl = URI.create("https://github.com/main");

        URI mainUrl = release.getMainUrl();

        Assertions.assertEquals(expectedMainUrl, mainUrl);
    }

    @Test
    void Check_Javadocs_Urls() {
        Release release = createRelease();
        List<URI> expectedJavadocsUrls = List.of(URI.create("https://github.com/javadocs"));

        List<URI> javadocUrls = release.getJavadocUrls();

        TestUtils.assertCollectionsEqualsWithoutOrder(expectedJavadocsUrls, javadocUrls);
    }

    @Test
    void Check_Required_Dependencies_Urls() {
        Release release = createRelease();
        List<URI> expectedRequiredDependenciesUrls = List.of(URI.create("https://github.com/required"));

        List<URI> requiredDependencyUrls = release.getRequiredDependencyUrls();

        TestUtils.assertCollectionsEqualsWithoutOrder(expectedRequiredDependenciesUrls, requiredDependencyUrls);
    }

    @Test
    void Check_Optional_Dependencies_Urls() {
        Release release = createRelease();
        List<URI> expectedOptionalDependenciesUrls = List.of(URI.create("https://github.com/optional"));

        List<URI> optionalDependencyUrls = release.getOptionalDependencyUrls();

        TestUtils.assertCollectionsEqualsWithoutOrder(expectedOptionalDependenciesUrls, optionalDependencyUrls);
    }

    private static Release createRelease() {
        return new Release(new ReleaseModel(
                "v1.0.0",
                URI.create("https://github.com/main"),
                List.of(URI.create("https://github.com/required")),
                List.of(URI.create("https://github.com/optional")),
                List.of(URI.create("https://github.com/javadocs")),
                new VersionRangeModel("v0.1.0", "v0.2.0", null)
        ));
    }
}
