package qupath.ext.extensionmanager.core.catalog;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A catalog describing a collection of extensions.
 *
 * @param name the name of the catalog
 * @param description a short (one sentence or so) description of what the catalog contains and what its purpose is
 * @param extensions the collection of extensions that the catalog describes. This list is immutable
 */
public record Catalog(String name, String description, List<Extension> extensions) {

    /**
     * Create a catalog.
     * <p>
     * It must respect the following requirements:
     * <ul>
     *     <li>The 'name', 'description', and 'extensions' fields must be defined (but can be empty).</li>
     *     <li>
     *         Each extension of the 'extensions' list must be a valid object
     *         (see {@link Extension#Extension(String, String, String, URI, List)}).
     *     </li>
     *     <li>Two extensions of the 'extensions' list cannot have the same name.</li>
     * </ul>
     *
     * @param name the name of the catalog
     * @param description a short (one sentence or so) description of what the catalog contains and what its purpose is
     * @param extensions the collection of extensions that the catalog describes
     * @throws IllegalArgumentException when the created catalog is not valid (see the requirements above)
     */
    public Catalog(String name, String description, List<Extension> extensions) {
        this.name = name;
        this.description = description;
        this.extensions = extensions == null ? null : Collections.unmodifiableList(extensions);

        checkValidity();
    }

    private void checkValidity() {
        Utils.checkField(name, "name", "Catalog");
        Utils.checkField(description, "description", "Catalog");
        Utils.checkField(extensions, "extensions", "Catalog");

        if (extensions.stream().map(Extension::name).collect(Collectors.toSet()).size() < extensions.size()) {
            throw new IllegalArgumentException(String.format(
                    "At least two extensions of %s have the same name",
                    extensions
            ));
        }
    }
}

