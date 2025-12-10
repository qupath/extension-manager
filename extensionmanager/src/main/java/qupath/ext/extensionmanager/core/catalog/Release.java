package qupath.ext.extensionmanager.core.catalog;

import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.model.VersionRangeModel;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public class Release {

    private final Version version;
    private final URI mainUrl;
    private final List<URI> javadocUrls;
    private final List<URI> requiredDependencyUrls;
    private final List<URI> optionalDependencyUrls;
    private final VersionRangeModel versionRange;

    public Release(
            Version version,
            URI mainUrl,
            List<URI> javadocUrls,
            List<URI> requiredDependencyUrls,
            List<URI> optionalDependencyUrls,
            VersionRangeModel versionRange
    ) {
        this.version = Objects.requireNonNull(version);
        this.mainUrl = Objects.requireNonNull(mainUrl);
        this.javadocUrls = Objects.requireNonNull(javadocUrls);
        this.requiredDependencyUrls = Objects.requireNonNull(requiredDependencyUrls);
        this.optionalDependencyUrls = Objects.requireNonNull(optionalDependencyUrls);
        this.versionRange = Objects.requireNonNull(versionRange);
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

    public Version getVersion() {
        return version;
    }

    public boolean isCompatible(Version version) {
        return versionRange.isCompatible(version);
    }

    public URI getMainUrl() {
        return mainUrl;
    }

    public List<URI> getJavadocUrls() {
        return javadocUrls;
    }

    public List<URI> getRequiredDependencyUrls() {
        return requiredDependencyUrls;
    }

    public List<URI> getOptionalDependencyUrls() {
        return optionalDependencyUrls;
    }
}
