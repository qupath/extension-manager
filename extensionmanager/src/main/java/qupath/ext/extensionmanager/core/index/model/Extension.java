package qupath.ext.extensionmanager.core.index.model;

import qupath.ext.extensionmanager.core.Version;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * A description of an extension.
 * <p>
 * Functions of this object may return null or throw undocumented exceptions if this object is not
 * valid (see {@link #checkValidity()}).
 *
 * @param name the extension's name
 * @param description a short (one sentence or so) description of what the extension is and what it does
 * @param author the author or group responsible for the extension
 * @param homepage a link to the GitHub repository associated with the extension
 * @param releases a list of available releases of the extension
 */
public record Extension(String name, String description, String author, URI homepage, List<Release> releases) {

    /**
     * Create an Extension.
     *
     * @param name the extension's name
     * @param description a short (one sentence or so) description of what the extension is and what it does
     * @param author the author or group responsible for the extension
     * @param homepage a link to the GitHub repository associated with the extension
     * @param releases a list of available releases of the extension
     * @throws IllegalStateException when the created object is not valid (see {@link #checkValidity()})
     */
    public Extension(String name, String description, String author, URI homepage, List<Release> releases) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.homepage = homepage;
        this.releases = releases;

        checkValidity();
    }

    /**
     * Check that this object is valid:
     * <ul>
     *     <li>The 'name', 'description', 'author', 'homepage', and 'releases' fields must be defined.</li>
     *     <li>Each release of the 'version' list must be a valid object (see {@link Release#checkValidity()}).</li>
     *     <li>The 'homepage' field must be a GitHub URL.</li>
     * </ul>
     *
     * @throws IllegalStateException when this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(name, "name", "Extension");
        Utils.checkField(description, "description", "Extension");
        Utils.checkField(author, "author", "Extension");
        Utils.checkField(homepage, "homepage", "Extension");
        Utils.checkField(releases, "releases", "Extension");

        for (Release release : releases) {
            release.checkValidity();
        }

        Utils.checkGithubURI(homepage);
    }

    /**
     * Provide the most up-to-date release compatible with the provided version.
     *
     * @param version the version that the release should be compatible with. It
     *                must be specified in the form "v[MAJOR].[MINOR].[PATCH]" or
     *                "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     * @return the most up-to-date release compatible with the provided version, or
     * an empty Optional if no release is compatible with the provided version
     * @throws IllegalArgumentException if the provided version doesn't match the required form
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
}

