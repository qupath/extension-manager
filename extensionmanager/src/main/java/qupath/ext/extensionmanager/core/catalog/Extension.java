package qupath.ext.extensionmanager.core.catalog;

import qupath.ext.extensionmanager.core.Version;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A description of an extension.
 *
 * @param name the extension's name
 * @param description a short (one sentence or so) description of what the extension is and what it does
 * @param author the author or group responsible for the extension
 * @param homepage a link to the GitHub repository associated with the extension
 * @param starred whether the extension is generally useful or recommended for most users
 * @param releases a list of available releases of the extension. This list is immutable
 */
public record Extension(String name, String description, String author, URI homepage, boolean starred, List<Release> releases) {

    /**
     * Create an Extension.
     * <p>
     * It must respect the following requirements:
     * <ul>
     *     <li>The 'name', 'description', 'author', 'homepage', and 'releases' fields must be defined (but can be empty).</li>
     *     <li>
     *         Each release of the 'version' list must be a valid object
     *         (see {@link Release#Release(String, URI, List, List, List, VersionRange)}).
     *     </li>
     *     <li>The 'homepage' field must be a GitHub URL.</li>
     * </ul>
     *
     * @param name the extension's name
     * @param description a short (one sentence or so) description of what the extension is and what it does
     * @param author the author or group responsible for the extension
     * @param homepage a link to the GitHub repository associated with the extension
     * @param starred whether the extension is generally useful or recommended for most users
     * @param releases a list of available releases of the extension
     * @throws IllegalArgumentException when the created extension is not valid (see the requirements above)
     */
    public Extension(String name, String description, String author, URI homepage, boolean starred, List<Release> releases) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.homepage = homepage;
        this.starred = starred;
        this.releases = releases == null ? null : Collections.unmodifiableList(releases);

        checkValidity();
    }

    /**
     * Provide the most up-to-date release compatible with the provided version.
     *
     * @param version the version that the release should be compatible with. It
     *                must be specified in the form "v[MAJOR].[MINOR].[PATCH]" or
     *                "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     * @return the most up-to-date release compatible with the provided version, or
     * an empty Optional if no release is compatible with the provided version
     * @throws IllegalArgumentException if this extension contains at least one release and
     * the provided version doesn't match the required form
     * @throws NullPointerException if this extension contains at least one release and
     * the provided version is null
     */
    public Optional<Release> getMaxCompatibleRelease(String version) {
        Release maxCompatibleRelease = null;

        for (Release release: releases) {
            if (release.versionRange().isCompatible(version) &&
                    (maxCompatibleRelease == null || new Version(release.name()).compareTo(new Version(maxCompatibleRelease.name())) > 0)
            ) {
                maxCompatibleRelease = release;
            }
        }

        return Optional.ofNullable(maxCompatibleRelease);
    }

    private void checkValidity() {
        Utils.checkField(name, "name", "Extension");
        Utils.checkField(description, "description", "Extension");
        Utils.checkField(author, "author", "Extension");
        Utils.checkField(homepage, "homepage", "Extension");
        Utils.checkField(releases, "releases", "Extension");

        Utils.checkGithubURI(homepage);
    }
}

