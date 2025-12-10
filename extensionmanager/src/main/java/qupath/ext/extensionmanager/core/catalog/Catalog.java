package qupath.ext.extensionmanager.core.catalog;

import qupath.ext.extensionmanager.core.Version;
import qupath.ext.extensionmanager.core.model.CatalogModel;
import qupath.ext.extensionmanager.core.model.CatalogModelFetcher;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Catalog {

    private final String name;
    private final String description;
    private final URI uri;
    private final URI rawUri;
    private final boolean deletable;
    private CompletableFuture<List<Extension>> extensions;

    public Catalog(String name, String description, URI uri, URI rawUri, boolean deletable, CatalogModel catalogModel) {
        this.name = Objects.requireNonNull(name);
        this.uri = Objects.requireNonNull(uri);
        this.rawUri = Objects.requireNonNull(rawUri);
        this.description = Objects.requireNonNull(description);
        this.deletable = deletable;

        if (catalogModel != null) {
            extensions = CompletableFuture.completedFuture(createExtensionsFromCatalog(catalogModel));
        }
    }

    public Catalog(String name, String description, URI uri, URI rawUri, boolean deletable) {
        this(name, description, uri, rawUri, deletable, null);
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public URI getUri() {
        return uri;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public synchronized CompletableFuture<List<Extension>> getExtensions() {
        if (extensions == null) {
            extensions = CatalogModelFetcher.getCatalog(rawUri).thenApply(this::createExtensionsFromCatalog);
        }
        return extensions;
    }

    private List<Extension> createExtensionsFromCatalog(CatalogModel catalog) {
        return catalog.extensions().stream()
                .map(extension -> new Extension(
                        extension.name(),
                        extension.description(),
                        extension.releases().stream().map(release -> new Release(
                                new Version(release.name()),
                                release.mainUrl(),
                                release.javadocUrls(),
                                release.requiredDependencyUrls(),
                                release.optionalDependencyUrls(),
                                release.versionRange()
                        )).toList(),
                        extension.homepage(),
                        extension.starred()
                ))
                .toList();
    }
}
