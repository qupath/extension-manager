package qupath.ext.extensionmanager.core.catalog;

import qupath.ext.extensionmanager.core.model.CatalogModel;
import qupath.ext.extensionmanager.core.model.CatalogModelFetcher;
import qupath.ext.extensionmanager.core.registry.RegistryCatalog;
import qupath.ext.extensionmanager.core.registry.RegistryExtension;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A catalog containing a collection of extensions.
 */
public class Catalog {

    private final String name;
    private final String description;
    private final URI uri;
    private final URI rawUri;
    private final boolean deletable;
    private final List<RegistryExtension> registryExtensions;
    private CompletableFuture<List<Extension>> extensions;

    /**
     * Create a non-deletable catalog from a list of attributes.
     *
     * @param name the name of the catalog
     * @param description a short (one sentence or so) description of what the catalog contains and what its purpose is
     * @param uri a URI pointing to the raw content of the catalog, or to a GitHub repository where the catalog can be found
     * @param rawUri the URI pointing to the raw content of the catalog (can be the same as the provided uri)
     */
    public Catalog(String name, String description, URI uri, URI rawUri) {
        this.name = name;
        this.description = description;
        this.uri = uri;
        this.rawUri = rawUri;
        this.deletable = false;
        this.registryExtensions = List.of();
    }

    /**
     * Create a catalog from a {@link CatalogModel}. This will directly populate the extensions.
     *
     * @param catalogModel information on the catalog
     * @param uri a URI pointing to the raw content of the catalog, or to a GitHub repository where the catalog can be found
     * @param rawUri the URI pointing to the raw content of the catalog (can be same as {@link #uri})
     * @param deletable whether this catalog can be deleted
     */
    public Catalog(CatalogModel catalogModel, URI uri, URI rawUri, boolean deletable) {
        this.name = catalogModel.name();
        this.description = catalogModel.description();
        this.uri = Objects.requireNonNull(uri);
        this.rawUri = Objects.requireNonNull(rawUri);
        this.deletable = deletable;
        this.registryExtensions = List.of();
        this.extensions = CompletableFuture.completedFuture(createExtensionsFromCatalog(catalogModel));
    }

    /**
     * Create a catalog from a {@link RegistryCatalog}. This will not populate the extensions.
     *
     * @param registryCatalog information on the catalog
     */
    public Catalog(RegistryCatalog registryCatalog) {
        this.name = registryCatalog.name();
        this.description = registryCatalog.description();
        this.uri = registryCatalog.uri();
        this.rawUri = registryCatalog.rawUri();
        this.deletable = registryCatalog.deletable();
        this.registryExtensions = registryCatalog.extensions();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Catalog catalog = (Catalog) o;
        return deletable == catalog.deletable && name.equals(catalog.name) && description.equals(catalog.description) &&
                uri.equals(catalog.uri) && rawUri.equals(catalog.rawUri);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + uri.hashCode();
        result = 31 * result + rawUri.hashCode();
        result = 31 * result + Boolean.hashCode(deletable);
        return result;
    }

    /**
     * @return the name of the catalog
     */
    public String getName() {
        return name;
    }

    /**
     * @return a short (one sentence or so) description of what the catalog contains and what its purpose is
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return a URI pointing to the raw content of the catalog, or to a GitHub repository where the catalog can be found
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @return the URI pointing to the raw content of the catalog (can be same as {@link #uri})
     */
    public URI getRawUri() {
        return rawUri;
    }

    /**
     * @return whether this metadata can be deleted
     */
    public boolean isDeletable() {
        return deletable;
    }

    /**
     * Compute and return the extensions this catalog owns.
     * <p>
     * Depending on which constructor was used to create this catalog, this function may act differently:
     * <ul>
     *     <li>If {@link #Catalog(CatalogModel, URI, URI, boolean)} was used, the extensions are already determined.</li>
     *     <li>
     *         If {@link #Catalog(RegistryCatalog)} was used, this function will call {@link CatalogModelFetcher#getCatalog(URI)}
     *         to determine the extensions, which might take some time.
     *     </li>
     * </ul>
     * At most one call to {@link CatalogModelFetcher#getCatalog(URI)} is made, because the results are cached.
     * <p>
     * Note that exception handling is left to the caller (the returned CompletableFuture may complete exceptionally
     * if the request made in {@link CatalogModelFetcher#getCatalog(URI)} failed).
     *
     * @return a CompletableFuture with the list of extensions this catalog owns (that completes exceptionally if the
     * operation failed)
     */
    public synchronized CompletableFuture<List<Extension>> getExtensions() {
        if (extensions == null) {
            extensions = CatalogModelFetcher.getCatalog(rawUri).thenApply(this::createExtensionsFromCatalog);
        }
        return extensions;
    }

    private List<Extension> createExtensionsFromCatalog(CatalogModel catalog) {
        return catalog.extensions().stream()
                .map(extension -> {
                    Optional<RegistryExtension> registryExtension = registryExtensions.stream()
                            .filter(e -> e.name().equals(extension.name()))
                            .findAny();

                    return new Extension(
                            extension,
                            registryExtension.flatMap(e -> extension.releases().stream()
                                    .filter(release -> release.name().equals(e.installedVersion()))
                                    .map(Release::new)
                                    .findAny()
                            ).orElse(null),
                            registryExtension.isPresent() && registryExtension.get().optionalDependenciesInstalled()
                    );
                })
                .toList();
    }
}
