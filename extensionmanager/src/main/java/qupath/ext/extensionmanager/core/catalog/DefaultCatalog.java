package qupath.ext.extensionmanager.core.catalog;

import java.net.URI;
import java.util.Objects;

/**
 * Represent a non-deletable catalog.
 * <p>
 * A {@link RuntimeException} is thrown if one parameter is null.
 *
 * @param name the name of the catalog
 * @param description a short (one sentence or so) description of what the catalog contains and what its purpose is
 * @param uri a URI pointing to the raw content of the catalog, or to a GitHub repository where the catalog can be found
 * @param rawUri the URI pointing to the raw content of the catalog (can be same as {@link #uri})
 */
public record DefaultCatalog(
        String name,
        String description,
        URI uri,
        URI rawUri
) {
    public DefaultCatalog {
        Objects.requireNonNull(name);
        Objects.requireNonNull(description);
        Objects.requireNonNull(uri);
        Objects.requireNonNull(rawUri);
    }
}
