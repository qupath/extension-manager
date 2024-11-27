package qupath.ext.extensionmanager.core.index.model;

import java.util.List;

/**
 * An index describing a collection of extensions.
 * <p>
 * Functions of this object may return null only if this object is not valid (see {@link #checkValidity()}).
 *
 * @param name the name of the index
 * @param description a short (one sentence or so) description of what the index contains and what its purpose is
 * @param extensions the collection of extensions that the index describes
 */
public record Index(String name, String description, List<Extension> extensions) {

    /**
     * Create an index.
     *
     * @param name the name of the index
     * @param description a short (one sentence or so) description of what the index contains and what its purpose is
     * @param extensions the collection of extensions that the index describes
     * @throws IllegalStateException when the created object is not valid (see {@link #checkValidity()})
     */
    public Index(String name, String description, List<Extension> extensions) {
        this.name = name;
        this.description = description;
        this.extensions = extensions;

        checkValidity();
    }

    /**
     * Check that this object is valid.
     *
     * @throws IllegalStateException when this object is not valid
     */
    public void checkValidity() {
        Utils.checkField(name, "name", "Index");
        Utils.checkField(description, "description", "Index");
        Utils.checkField(extensions, "extensions", "Index");

        for (Extension extension: extensions) {
            extension.checkValidity();
        }
    }
}

