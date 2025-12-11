package qupath.ext.extensionmanager.core.registry;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * A catalog containing a collection of extensions.
 * <p>
 * A {@link RuntimeException} is thrown if one parameter is null.
 *
 * @param name the name of the catalog
 * @param description a short (one sentence or so) description of what the catalog contains and what its purpose is
 * @param uri a URI pointing to the raw content of the catalog, or to a GitHub repository where the catalog can be found
 * @param rawUri the URI pointing to the raw content of the catalog (can be same as {@link #uri})
 * @param deletable whether this metadata can be deleted
 * @param extensions a list of installed extensions this catalogs owns
 */
public record RegistryCatalog(
        String name,
        String description,
        URI uri,
        URI rawUri,
        boolean deletable,
        List<RegistryExtension> extensions
) {
    public RegistryCatalog {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(uri);
        Objects.requireNonNull(rawUri);
        Objects.requireNonNull(extensions);
    }
}
