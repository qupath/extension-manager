package qupath.ext.extensionmanager.core.index.model;

import java.util.List;
import java.util.stream.Collectors;

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
        Utils.checkField(name, "name", "Index");
        Utils.checkField(description, "description", "Index");
        Utils.checkField(extensions, "extensions", "Index");

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

