package qupath.ext.extensionmanager.core.index.model;

import qupath.ext.extensionmanager.core.Version;

import java.net.URI;
import java.util.List;

/**
 * A description of an extension release hosted on GitHub.
 * <p>
 * Functions of this object may return null only if this object is not valid (see {@link #checkValidity()}).
 *
 * @param name the name of this release in the form "v[MAJOR].[MINOR].[PATCH]" or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
 * @param mainUrl the GitHub URL where the main extension jar can be downloaded
 * @param requiredDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where required dependency jars can be downloaded
 * @param optionalDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where optional dependency jars can be downloaded
 * @param javadocsUrls SciJava Maven, Maven Central, or GitHub URLs where javadoc jars for the main extension
 *                     jar and for dependencies can be downloaded
 * @param versionRange a specification of minimum and maximum compatible versions
 */
public record Release(
        String name,
        URI mainUrl,
        List<URI> requiredDependencyUrls,
        List<URI> optionalDependencyUrls,
        List<URI> javadocsUrls,
        VersionRange versionRange
) {
    private static final List<String> VALID_HOSTS = List.of("github.com", "maven.scijava.org", "repo1.maven.org");
    private static final String VALID_SCHEME = "https";

    /**
     * Create a Release.
     *
     * @param name the name of this release in the form "v[MAJOR].[MINOR].[PATCH]" or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     * @param mainUrl the GitHub URL where the main extension jar can be downloaded
     * @param requiredDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where required dependency jars can be downloaded
     * @param optionalDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where optional dependency jars can be downloaded
     * @param javadocsUrls SciJava Maven, Maven Central, or GitHub URLs where javadoc jars for the main extension
     *                     jar and for dependencies can be downloaded
     * @param versionRange a specification of minimum and maximum compatible versions
     * @throws IllegalStateException when the created object is not valid (see {@link #checkValidity()})
     */
    public Release(
            String name,
            URI mainUrl,
            List<URI> requiredDependencyUrls,
            List<URI> optionalDependencyUrls,
            List<URI> javadocsUrls,
            VersionRange versionRange
    ) {
        this.name = name;
        this.mainUrl = mainUrl;
        this.requiredDependencyUrls = requiredDependencyUrls;
        this.optionalDependencyUrls = optionalDependencyUrls;
        this.javadocsUrls = javadocsUrls;
        this.versionRange = versionRange;

        checkValidity();
    }

    @Override
    public List<URI> requiredDependencyUrls() {
        return requiredDependencyUrls == null ? List.of() : requiredDependencyUrls;
    }

    @Override
    public List<URI> optionalDependencyUrls() {
        return optionalDependencyUrls == null ? List.of() : optionalDependencyUrls;
    }

    @Override
    public List<URI> javadocsUrls() {
        return javadocsUrls == null ? List.of() : javadocsUrls;
    }

    /**
     * Check that this object is valid:
     * <ul>
     *     <li>The 'name', 'mainUrl', and 'versionRange' fields must be defined.</li>
     *     <li>
     *         'name' must be specified in the form "v[MAJOR].[MINOR].[PATCH]" corresponding to semantic versions,
     *         although trailing release candidate qualifiers (eg, "-rc1") are also allowed.
     *     </li>
     *     <li>The 'versions' object must be valid (see {@link VersionRange#checkValidity()}).</li>
     *     <li>The 'mainURL' field must be a GitHub URL. All other URLs must be SciJava Maven, Maven Central, or GitHub URLs.</li>
     * </ul>
     *
     * @throws IllegalStateException when this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(name, "name", "Release");
        Utils.checkField(mainUrl, "mainUrl", "Release");
        Utils.checkField(versionRange, "versionRange", "Release");

        try {
            Version.isValid(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }

        versionRange.checkValidity();

        Utils.checkGithubURI(mainUrl);

        checkURIHostValidity(requiredDependencyUrls);
        checkURIHostValidity(optionalDependencyUrls);
        checkURIHostValidity(javadocsUrls);
    }

    private static void checkURIHostValidity(List<URI> uris) {
        if (uris != null) {
            for (URI uri: uris) {
                if (!VALID_SCHEME.equalsIgnoreCase(uri.getScheme())) {
                    throw new IllegalStateException(String.format(
                            "The URL %s must use %s",
                            uri,
                            VALID_SCHEME
                    ));
                }

                if (!VALID_HOSTS.contains(uri.getHost())) {
                    throw new IllegalStateException(String.format(
                            "The host part of %s is not among %s", uri, VALID_HOSTS
                    ));
                }
            }
        }
    }
}

