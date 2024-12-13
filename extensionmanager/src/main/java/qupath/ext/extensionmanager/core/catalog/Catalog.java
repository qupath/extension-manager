package qupath.ext.extensionmanager.core.catalog;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A catalog describing a collection of extensions.
 * <p>
 * Functions of this object may return null only if this object is not valid (see {@link #checkValidity()}).
 *
 * @param name the name of the catalog
 * @param description a short (one sentence or so) description of what the catalog contains and what its purpose is
 * @param extensions the collection of extensions that the catalog describes
 */
public record Catalog(String name, String description, List<Extension> extensions) {

    /**
     * Create a catalog.
     *
     * @param name the name of the catalog
     * @param description a short (one sentence or so) description of what the catalog contains and what its purpose is
     * @param extensions the collection of extensions that the catalog describes
     * @throws IllegalStateException when the created object is not valid (see {@link #checkValidity()})
     */
    public Catalog(String name, String description, List<Extension> extensions) {
        this.name = name;
        this.description = description;
        this.extensions = extensions;

        checkValidity();
    }

    /**
     * Check that this object is valid:
     * <ul>
     *     <li>The 'name', 'description', and 'extensions' fields must be defined.</li>
     *     <li>Each extension of the 'extensions' list must be a valid object (see {@link Extension#checkValidity()}).</li>
     *     <li>Two extensions of the 'extensions' list cannot have the same name.</li>
     * </ul>
     *
     * @throws IllegalStateException when this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(name, "name", "Catalog");
        Utils.checkField(description, "description", "Catalog");
        Utils.checkField(extensions, "extensions", "Catalog");

        for (Extension extension: extensions) {
            extension.checkValidity();
        }

        if (extensions.stream().map(Extension::name).collect(Collectors.toSet()).size() < extensions.size()) {
            throw new IllegalStateException(String.format(
                    "At least two extensions of %s have the same name",
                    extensions
            ));
        }
    }
}

