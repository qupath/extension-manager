package qupath.ext.extensionmanager.core.registry;

import qupath.ext.extensionmanager.core.catalog.Catalog;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A registry contains a list of {@link RegistryCatalog catalogs}.
 * <p>
 * A {@link RuntimeException} is thrown if one parameter is null.
 *
 * @param catalogs the catalogs this registry owns
 */
public record Registry(List<RegistryCatalog> catalogs) {

    public Registry {
        Objects.requireNonNull(catalogs);
    }

    /**
     * Create a registry from a list of {@link Catalog catalogs}. As this function requires to call {@link Catalog#getExtensions()},
     * it may take some time to complete.
     * <p>
     * Note that exception handling is left to the caller (the returned CompletableFuture may complete exceptionally).
     *
     * @param catalogs the catalogs to create the registry from
     * @return a CompletableFuture with the created registry (that completes exceptionally if the operation failed)
     */
    public static CompletableFuture<Registry> createFromCatalogs(List<Catalog> catalogs) {
        return CompletableFuture.supplyAsync(() -> new Registry(catalogs.stream()
                .map(catalog -> new RegistryCatalog(
                        catalog.getName(),
                        catalog.getDescription(),
                        catalog.getUri(),
                        catalog.getRawUri(),
                        catalog.isDeletable(),
                        catalog.getExtensions().join().stream()
                                .map(extension -> extension.getInstalledRelease().getValue()
                                        .map(release -> new RegistryExtension(
                                                extension.getName(),
                                                release.getVersion().toString(),
                                                extension.areOptionalDependenciesInstalled().get()
                                        ))
                                        .orElse(null)
                                )
                                .filter(Objects::nonNull)
                                .toList()
                ))
                .toList()
        ));
    }
}
