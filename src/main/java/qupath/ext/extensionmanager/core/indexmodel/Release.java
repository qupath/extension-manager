package qupath.ext.extensionmanager.core.indexmodel;

import java.net.URL;
import java.util.List;

/**
 * A description of an extension release hosted on GitHub.
 * <p>
 * Functions of this object may return null only if this object is not valid (see {@link #checkValidity()}).
 *
 * @param name the name of this release
 * @param mainUrl the GitHub URL where the main extension jar can be downloaded
 * @param requiredDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where required dependency jars can be downloaded
 * @param optionalDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where optional dependency jars can be downloaded
 * @param javadocsUrls SciJava Maven, Maven Central, or GitHub URLs where javadoc jars for the main extension
 *                     jar and for dependencies can be downloaded
 * @param qupathVersions a specification of minimum and maximum compatible QuPath versions
 */
public record Release(
        String name,
        URL mainUrl,
        List<URL> requiredDependencyUrls,
        List<URL> optionalDependencyUrls,
        List<URL> javadocsUrls,
        QuPathVersionRange qupathVersions
) {
    private static final List<String> VALID_HOSTS = List.of("github.com", "maven.scijava.org", "repo1.maven.org");

    /**
     * Create a Release.
     *
     * @param name the name of this release
     * @param mainUrl the GitHub URL where the main extension jar can be downloaded
     * @param requiredDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where required dependency jars can be downloaded
     * @param optionalDependencyUrls SciJava Maven, Maven Central, or GitHub URLs where optional dependency jars can be downloaded
     * @param javadocsUrls SciJava Maven, Maven Central, or GitHub URLs where javadoc jars for the main extension
     *                     jar and for dependencies can be downloaded
     * @param qupathVersions a specification of minimum and maximum compatible QuPath versions
     * @throws IllegalStateException when the created object is not valid (see {@link #checkValidity()})
     */
    public Release(
            String name,
            URL mainUrl,
            List<URL> requiredDependencyUrls,
            List<URL> optionalDependencyUrls,
            List<URL> javadocsUrls,
            QuPathVersionRange qupathVersions
    ) {
        this.name = name;
        this.mainUrl = mainUrl;
        this.requiredDependencyUrls = requiredDependencyUrls;
        this.optionalDependencyUrls = optionalDependencyUrls;
        this.javadocsUrls = javadocsUrls;
        this.qupathVersions = qupathVersions;

        checkValidity();
    }

    @Override
    public List<URL> requiredDependencyUrls() {
        return requiredDependencyUrls == null ? List.of() : requiredDependencyUrls;
    }

    @Override
    public List<URL> optionalDependencyUrls() {
        return optionalDependencyUrls == null ? List.of() : optionalDependencyUrls;
    }

    @Override
    public List<URL> javadocsUrls() {
        return javadocsUrls == null ? List.of() : javadocsUrls;
    }

    /**
     * Check that this object is valid
     * <ul>
     *     <li>The 'min', 'mainUrl', and 'qupathVersions' fields must be defined.</li>
     *     <li>The 'mainURL' field must be a GitHub URL. All other URLs must be SciJava Maven, Maven Central, or GitHub URLs.</li>
     * </ul>
     *
     * @throws IllegalStateException when this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(name, "name", "Release");
        Utils.checkField(mainUrl, "mainUrl", "Release");
        Utils.checkField(qupathVersions, "qupathVersions", "Release");

        qupathVersions.checkValidity();

        Utils.checkGithubURL(mainUrl);

        checkURLHostValidity(requiredDependencyUrls);
        checkURLHostValidity(optionalDependencyUrls);
        checkURLHostValidity(javadocsUrls);
    }

    private static void checkURLHostValidity(List<URL> urls) {
        if (urls != null) {
            for (URL url: urls) {
                if (!VALID_HOSTS.contains(url.getHost())) {
                    throw new IllegalStateException(String.format(
                            "The host part of %s is not among %s", url, VALID_HOSTS
                    ));
                }
            }
        }
    }
}

