package qupath.ext.extensionmanager.core.catalog;

import qupath.ext.extensionmanager.core.Version;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * A description of an extension release hosted on GitHub.
 *
 * @param name the name of this release in the form "v[MAJOR].[MINOR].[PATCH]" or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
 * @param mainUrl the GitHub URL where the main extension jar can be downloaded
 * @param requiredDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where required dependency jars can be downloaded.
 *                               This list is immutable and won't be null
 * @param optionalDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where optional dependency jars can be downloaded.
 *                               This list is immutable and won't be null
 * @param javadocUrls SciJava Maven, Maven Central, or GitHub URLs where javadoc jars for the main extension
 *                     jar and for dependencies can be downloaded. This list is immutable and won't be null
 * @param versionRange a specification of minimum and maximum compatible versions
 */
public record Release(
        String name,
        URI mainUrl,
        List<URI> requiredDependencyUrls,
        List<URI> optionalDependencyUrls,
        List<URI> javadocUrls,
        VersionRange versionRange
) {
    private static final List<String> VALID_HOSTS = List.of("github.com", "maven.scijava.org", "repo1.maven.org");
    private static final String VALID_SCHEME = "https";

    /**
     * Create a Release.
     * <p>
     * It must respect the following requirements:
     * <ul>
     *     <li>The 'name', 'mainUrl', and 'versionRange' fields must be defined.</li>
     *     <li>
     *         'name' must be specified in the form "v[MAJOR].[MINOR].[PATCH]" corresponding to semantic versions,
     *         although trailing release candidate qualifiers (eg, "-rc1") are also allowed.
     *     </li>
     *     <li>The 'versions' object must be valid (see {@link VersionRange#VersionRange(String, String, List)}).</li>
     *     <li>The 'mainURL' field must be a GitHub URL. All other URLs must be SciJava Maven, Maven Central, or GitHub URLs.</li>
     * </ul>
     *
     * @param name the name of this release in the form "v[MAJOR].[MINOR].[PATCH]" or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     * @param mainUrl the GitHub URL where the main extension jar can be downloaded
     * @param requiredDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where required dependency jars can be downloaded.
     *                               Can be null
     * @param optionalDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where optional dependency jars can be downloaded.
     *                               Can be null
     * @param javadocUrls SciJava Maven, Maven Central, or GitHub URLs where javadoc jars for the main extension
     *                    jar and for dependencies can be downloaded. Can be null
     * @param versionRange a specification of minimum and maximum compatible versions
     * @throws IllegalArgumentException when the created release is not valid (see the requirements above)
     */
    public Release(
            String name,
            URI mainUrl,
            List<URI> requiredDependencyUrls,
            List<URI> optionalDependencyUrls,
            List<URI> javadocUrls,
            VersionRange versionRange
    ) {
        this.name = name;
        this.mainUrl = mainUrl;
        this.requiredDependencyUrls = requiredDependencyUrls == null ? List.of() : Collections.unmodifiableList(requiredDependencyUrls);
        this.optionalDependencyUrls = optionalDependencyUrls == null ? List.of() : Collections.unmodifiableList(optionalDependencyUrls);
        this.javadocUrls = javadocUrls == null ? List.of() : Collections.unmodifiableList(javadocUrls);
        this.versionRange = versionRange;

        checkValidity();
    }

    private void checkValidity() {
        Utils.checkField(name, "name", "Release");
        Utils.checkField(mainUrl, "mainUrl", "Release");
        Utils.checkField(versionRange, "versionRange", "Release");

        Version.isValid(name, true);

        Utils.checkGithubURI(mainUrl);

        checkURIHostValidity(requiredDependencyUrls);
        checkURIHostValidity(optionalDependencyUrls);
        checkURIHostValidity(javadocUrls);
    }

    private static void checkURIHostValidity(List<URI> uris) {
        if (uris != null) {
            for (URI uri: uris) {
                if (!VALID_SCHEME.equalsIgnoreCase(uri.getScheme())) {
                    throw new IllegalArgumentException(String.format(
                            "The URL %s must use %s",
                            uri,
                            VALID_SCHEME
                    ));
                }

                if (!VALID_HOSTS.contains(uri.getHost())) {
                    throw new IllegalArgumentException(String.format(
                            "The host part of %s is not among %s", uri, VALID_HOSTS
                    ));
                }
            }
        }
    }
}

