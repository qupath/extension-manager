package qupath.ext.extensionmanager.core.model;

import java.net.URI;
import java.util.Collections;
import java.util.List;

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
public record ExtensionModel(String name, String description, String author, URI homepage, boolean starred, List<ReleaseModel> releases) {

    /**
     * Create an Extension.
     * <p>
     * It must respect the following requirements:
     * <ul>
     *     <li>The 'name', 'description', 'author', 'homepage', and 'releases' fields must be defined (but can be empty).</li>
     *     <li>
     *         Each release of the 'version' list must be a valid object
     *         (see {@link ReleaseModel#ReleaseModel(String, URI, List, List, List, VersionRangeModel)}).
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
    public ExtensionModel(String name, String description, String author, URI homepage, boolean starred, List<ReleaseModel> releases) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.homepage = homepage;
        this.starred = starred;
        this.releases = releases == null ? null : Collections.unmodifiableList(releases);

        checkValidity();
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

