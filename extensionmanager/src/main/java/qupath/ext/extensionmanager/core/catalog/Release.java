package qupath.ext.extensionmanager.core.catalog;

import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.model.ReleaseModel;
import qupath.ext.extensionmanager.core.model.VersionRangeModel;

import java.net.URI;
import java.util.List;

/**
 * An extension release hosted on GitHub.
 */
public class Release {

    private final Version version;
    private final URI mainUrl;
    private final List<URI> javadocUrls;
    private final List<URI> requiredDependencyUrls;
    private final List<URI> optionalDependencyUrls;
    private final VersionRangeModel versionRange;

    /**
     * Create a release from a {@link ReleaseModel}.
     *
     * @param releaseModel information on the release
     * @throws NullPointerException if the provided parameter is null
     */
    public Release(ReleaseModel releaseModel) {
        this.version = new Version(releaseModel.name());
        this.mainUrl = releaseModel.mainUrl();
        this.javadocUrls = releaseModel.javadocUrls();
        this.requiredDependencyUrls = releaseModel.requiredDependencyUrls();
        this.optionalDependencyUrls = releaseModel.optionalDependencyUrls();
        this.versionRange = releaseModel.versionRange();
    }

    @Override
    public String toString() {
        return version.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Release release = (Release) o;
        return version.equals(release.version) && mainUrl.equals(release.mainUrl) && javadocUrls.equals(release.javadocUrls) &&
                requiredDependencyUrls.equals(release.requiredDependencyUrls) && optionalDependencyUrls.equals(release.optionalDependencyUrls) &&
                versionRange.equals(release.versionRange);
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + mainUrl.hashCode();
        result = 31 * result + javadocUrls.hashCode();
        result = 31 * result + requiredDependencyUrls.hashCode();
        result = 31 * result + optionalDependencyUrls.hashCode();
        result = 31 * result + versionRange.hashCode();
        return result;
    }

    /**
     * @return the version of this release
     */
    public Version getVersion() {
        return version;
    }

    /**
     * Indicate whether the provided version is compatible with this release.
     *
     * @param version the version that may be compatible with this release
     * @return whether the provided version is compatible with this release
     * @throws NullPointerException if the provided version is null
     */
    public boolean isCompatible(Version version) {
        return versionRange.isCompatible(version);
    }

    /**
     * @return the GitHub URL where the main extension jar can be downloaded
     */
    public URI getMainUrl() {
        return mainUrl;
    }

    /**
     * @return SciJava Maven, Maven Central, or GitHub URLs where javadoc jars for the main extension jar and for dependencies
     * can be downloaded
     */
    public List<URI> getJavadocUrls() {
        return javadocUrls;
    }

    /**
     * @return SciJava Maven, Maven Central, or GitHub URLs where required dependency jars can be downloaded
     */
    public List<URI> getRequiredDependencyUrls() {
        return requiredDependencyUrls;
    }

    /**
     * @return SciJava Maven, Maven Central, or GitHub URLs where optional dependency jars can be downloaded
     */
    public List<URI> getOptionalDependencyUrls() {
        return optionalDependencyUrls;
    }
}
