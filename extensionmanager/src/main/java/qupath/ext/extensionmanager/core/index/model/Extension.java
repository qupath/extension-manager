package qupath.ext.extensionmanager.core.index.model;

import java.net.URI;
import java.util.List;

/**
 * A description of an extension.
 * <p>
 * Functions of this object may return null only if this object is not valid (see {@link #checkValidity()}).
 *
 * @param name the extension's name
 * @param description a short (one sentence or so) description of what the extension is and what it does
 * @param homepage a link to the GitHub repository associated with the extension
 * @param versions a list of available versions of the extension
 */
public record Extension(String name, String description, URI homepage, List<Release> versions) {

    /**
     * Create an Extension.
     *
     * @param name the extension's name
     * @param description a short (one sentence or so) description of what the extension is and what it does
     * @param homepage a link to the GitHub repository associated with the extension
     * @param versions a list of available versions of the extension
     * @throws IllegalStateException when the created object is not valid (see {@link #checkValidity()})
     */
    public Extension(String name, String description, URI homepage, List<Release> versions) {
        this.name = name;
        this.description = description;
        this.homepage = homepage;
        this.versions = versions;

        checkValidity();
    }

    /**
     * Check that this object is valid:
     * <ul>
     *     <li>The 'name', 'description', 'homepage', and 'versions' fields must be defined.</li>
     *     <li>Each release of the 'version' list must be a valid object (see {@link Release#checkValidity()}).</li>
     *     <li>The 'homepage' field must be a GitHub URL.</li>
     * </ul>
     *
     * @throws IllegalStateException when this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(name, "name", "Extension");
        Utils.checkField(description, "description", "Extension");
        Utils.checkField(homepage, "homepage", "Extension");
        Utils.checkField(versions, "versions", "Extension");

        for (Release version : versions) {
            version.checkValidity();
        }

        Utils.checkGithubURI(homepage);
    }
}

