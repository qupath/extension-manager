package qupath.ext.extensionmanager.core;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.core.registry.Registry;
import qupath.ext.extensionmanager.core.catalog.UpdateAvailable;
import qupath.ext.extensionmanager.core.registry.RegistryCatalog;
import qupath.ext.extensionmanager.core.tools.FileDownloader;
import qupath.ext.extensionmanager.core.tools.FileTools;
import qupath.ext.extensionmanager.core.tools.ZipExtractor;

import java.io.IOError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A manager for catalogs and extensions. It can be used to get access to all saved catalogs, add or remove a catalog, get
 * access to all installed extensions, and install or delete an extension. Manually installed extensions are automatically
 * detected.
 * <p>
 * It also automatically loads extension classes with a custom ClassLoader (see {@link #getExtensionClassLoader()}). Note
 * that removed extensions are not unloaded from the class loader.
 * <p>
 * The list of active catalogs and installed extensions is determined by this class. It is internally saved in a registry
 * JSON file located in the extension directory.
 * <p>
 * This class is thread-safe.
 * <p>
 * This manager must be {@link #close() closed} once no longer used.
 */
public class ExtensionCatalogManager implements AutoCloseable{

    private static final Logger logger = LoggerFactory.getLogger(ExtensionCatalogManager.class);
    private final ObservableList<Catalog> catalogs = FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<Catalog> catalogsImmutable = FXCollections.unmodifiableObservableList(catalogs);
    private final ObservableList<Path> catalogManagedInstalledJars = FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<Path> catalogManagedInstalledJarsImmutable = FXCollections.unmodifiableObservableList(catalogManagedInstalledJars);
    private final ExtensionFolderManager extensionFolderManager;
    private final ExtensionClassLoader extensionClassLoader;
    private final Version version;
    private record UriFileName(URI uri, Path filePath) {}
    /**
     * Indicate an extension installation step
     */
    public enum InstallationStep {
        /**
         * Some files are being downloaded
         */
        DOWNLOADING,
        /**
         * Some files are being extracted
         */
        EXTRACTING_ZIP
    }
    private enum Operation {
        ADD,
        REMOVE
    }

    /**
     * Create the extension catalog manager.
     *
     * @param extensionsDirectoryPath an observable value pointing to the path the extensions directory should have. The
     *                                path can be null or invalid (but not the observable). If this observable is changed,
     *                                catalogs and extensions will be set to the content of the new value of the observable
     *                                (so will be reset if the new path is empty)
     * @param parentClassLoader the class loader that should be the parent of the extension class loader. Can be null to use
     *                          the bootstrap class loader
     * @param version a text describing the release of the current software with the form "v[MAJOR].[MINOR].[PATCH]" or
     *                "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]". It will determine which extensions are compatible
     * @param defaultCatalogs a list of catalogs this manager should use by default, i.e. when no catalog or extension is
     *                        installed
     * @throws IllegalArgumentException if the provided version doesn't meet the specified requirements
     * @throws NullPointerException if one of the parameters (except the class loader) is null
     */
    public ExtensionCatalogManager(
            ObservableValue<Path> extensionsDirectoryPath,
            ClassLoader parentClassLoader,
            String version,
            List<Catalog> defaultCatalogs
    ) {
        this.extensionFolderManager = new ExtensionFolderManager(extensionsDirectoryPath);
        this.extensionClassLoader = new ExtensionClassLoader(parentClassLoader);
        this.version = new Version(version);

        resetCatalogsAndJars(defaultCatalogs);
        extensionFolderManager.getCatalogsDirectoryPath().addListener((p, o, n) ->
                resetCatalogsAndJars(defaultCatalogs)
        );

        loadJars();
    }

    @Override
    public void close() throws Exception {
        this.extensionClassLoader.close();
        this.extensionFolderManager.close();
    }

    /**
     * @return the version of the current software, as given in {@link #ExtensionCatalogManager(ObservableValue, ClassLoader, String, List)}
     */
    public Version getVersion() {
        return version;
    }

    /**
     * @return an observable value containing the path to the extensions folder. It may be updated from any thread and the
     * path (but not the observable) can be null or invalid
     */
    public ObservableValue<Path> getExtensionsDirectory() {
        return extensionFolderManager.getExtensionsDirectoryPath();
    }

    /**
     * Get the path to the directory containing the provided catalog. It may not exist.
     *
     * @param catalogName the name of the catalog to retrieve
     * @return the path of the directory containing the provided catalog
     * @throws InvalidPathException if the path cannot be created
     * @throws NullPointerException if the provided catalog is null or if the path contained in {@link #getExtensionsDirectory()}
     * is null
     */
    public Path getCatalogDirectory(String catalogName) {
        return extensionFolderManager.getCatalogDirectoryPath(catalogName);
    }

    /**
     * Add and save a catalog.
     * <p>
     * This operation may take some time, but can be interrupted.
     *
     * @param catalog the catalog to add. It must have a different name from the ones returned by {@link #getCatalogs()}
     * @throws IllegalArgumentException if a catalog with the same name already exists
     * @throws IOException if an I/O error occurs while saving the catalogs to disk
     * @throws NullPointerException if the path contained in {@link #getExtensionsDirectory()} is null or if the provided
     * catalog is null
     * @throws InvalidPathException if the path to the registry containing the list of catalogs cannot be created
     * @throws ExecutionException if an error occurred while saving the registry
     * @throws InterruptedException if the calling thread is interrupted
     */
    public synchronized void addCatalog(Catalog catalog) throws IOException, ExecutionException, InterruptedException {
        Objects.requireNonNull(catalog);

        if (catalogs.stream().map(Catalog::getName).anyMatch(catalogName -> catalogName.equals(catalog.getName()))) {
            throw new IllegalArgumentException(String.format(
                    "Cannot add %s: a catalog with the same name already exists",
                    catalog
            ));
        }

        catalogs.add(catalog);
        try {
            extensionFolderManager.saveRegistry(Registry.createFromCatalogs(catalogs).get());
        } catch (Exception e) {
            catalogs.remove(catalog);
            throw e;
        }

        logger.info("Catalog {} added", catalog);
    }

    /**
     * Get the catalogs added or removed with {@link #addCatalog(Catalog)} and {@link #removeCatalog(Catalog)}. This list
     * may be updated from any thread and won't contain null elements.
     *
     * @return a read-only observable list of all saved catalogs
     */
    public ObservableList<Catalog> getCatalogs() {
        return catalogsImmutable;
    }

    /**
     * Remove the provided catalog from the list of saved catalogs.
     * <p>
     * Warning: this will attempt to move the directory returned by {@link #getCatalogDirectory(String)} to trash if supported
     * by this platform or recursively delete it if extension are asked to be removed. If this operation fails, no exception
     * is thrown.
     * <p>
     * If the provided catalog does not belong to {@link #getCatalogs()}, nothing happens.
     * <p>
     * This operation may take some time, but can be interrupted.
     *
     * @param catalog the catalog to remove
     * @throws IllegalArgumentException if the provided catalog is not {@link RegistryCatalog#deletable() deletable}
     * @throws IOException if an I/O error occurs while removing the catalog from disk
     * @throws NullPointerException if the path contained in {@link #getExtensionsDirectory()} is null or if the provided
     * catalog is null
     * @throws InvalidPathException if the path to the registry containing the list of catalogs cannot be created
     * @throws ExecutionException if an error occurred while saving the registry
     * @throws InterruptedException if the calling thread is interrupted
     */
    public synchronized void removeCatalog(Catalog catalog) throws IOException, ExecutionException, InterruptedException {
        if (catalogs.stream().noneMatch(catalog::equals)) {
            logger.debug("{} was asked to be removed, but does not belong to {}. Doing nothing", catalog, catalogs);
            return;
        }

        if (!catalog.isDeletable()) {
            throw new IllegalArgumentException(String.format("Cannot delete %s: this catalog is not deletable", catalog));
        }

        catalogs.remove(catalog);
        try {
            extensionFolderManager.saveRegistry(Registry.createFromCatalogs(catalogs).get());
        } catch (Exception e) {
            catalogs.add(catalog);
            throw e;
        }

        updateCatalogManagedInstalledJarsOfDirectory(
                extensionFolderManager.getCatalogDirectoryPath(catalog.getName()),
                Operation.REMOVE
        );

        try {
            extensionFolderManager.deleteExtensionsFromCatalog(catalog.getName());
        } catch (IOException | InvalidPathException | NullPointerException e) {
            logger.debug("Could not delete {}", catalog, e);
        }

        logger.info("Catalog {} removed", catalog);
    }

    /**
     * Get the path to the directory containing the provided extension of the provided catalog.
     *
     * @param catalogName the name of the catalog owning the extension
     * @param extensionName the name of the extension to retrieve
     * @return the path to the folder containing the provided extension
     * @throws InvalidPathException if the path cannot be created
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionsDirectory()}
     * is null
     */
    public Path getExtensionDirectory(String catalogName, String extensionName) {
        return extensionFolderManager.getExtensionDirectoryPath(catalogName, extensionName);
    }

    /**
     * Get the list of links the {@link #installOrUpdateExtension(Catalog, Extension, Release, boolean, Consumer, BiConsumer)}
     * function will download to install the provided release.
     *
     * @param release the release of the extension to install
     * @param installOptionalDependencies whether to install optional dependencies
     * @return the list URIs that will be downloaded to install the extension with the provided parameters
     * @throws InvalidPathException if a path cannot be created, for example because the extensions folder path contain
     * invalid characters
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionsDirectory()}
     * is null
     */
    public List<URI> getDownloadLinks(Release release, boolean installOptionalDependencies) {
        try {
            return getDownloadUrlsToFilePaths(
                    "catalog",  // The catalog and extension names parameters are only used to create the file
                    "extension",            // paths, which are not considered here
                    release,
                    installOptionalDependencies,
                    false
            ).stream().map(UriFileName::uri).toList();
        } catch (IOException e) {
            // IOException only occurs if directories are created, which is not the case here, so this should never be called
            throw new RuntimeException(e);
        }
    }

    /**
     * Install (or update if it already exists) an extension. This may take a lot of time depending on the internet connection
     * and the size of the extension, but this operation can be interrupted.
     * <p>
     * If the extension already exists, it will be deleted before downloading the provided version of the extension.
     * <p>
     * Warning: If the extension already exists, this function will attempt to move to trash the directory returned by
     * {@link #getExtensionDirectory(String, String)} or recursively delete it if moving files to trash is not supported.
     * If this operation fails, no exception is thrown.
     * <p>
     * Note that this function attempts to install the provided release even if it is not compatible with {@link #getVersion()}.
     *
     * @param catalog the catalog owning the extension to install/update. It must be one of {@link #getCatalogs()}
     * @param extension the extension to install/update. It must belong to the provided catalog
     * @param release the release to install. It must belong to the provided extension
     * @param installOptionalDependencies whether to install optional dependencies
     * @param onProgress a function that will be called at different steps during the installation. Its parameter will be
     *                   a float between 0 and 1 indicating the progress of the installation (0: beginning, 1: finished).
     *                   This function will be called from the calling thread
     * @param onStatusChanged a function that will be called at different steps during the installation. Its first parameter
     *                        will be the step currently happening, and its second parameter a text describing the resource
     *                        on which the step is happening (for example, a link if the step is a download). This function
     *                        will be called from the calling thread
     * @throws IllegalArgumentException if the provided catalog does not belong to {@link #getCatalogs()}, if the provided
     * extension does not belong to the provided catalog, or if the provided release does not belong to the provided extension
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionsDirectory()}
     * is null
     * @throws IOException if an I/O error occurred while deleting, downloading or installing the extension
     * @throws InvalidPathException if a path cannot be created, for example because the extensions folder path contain
     * invalid characters
     * @throws InterruptedException if the calling thread is interrupted
     * @throws ExecutionException if an error occurs while retrieving the extensions of the provided catalog
     */
    public synchronized void installOrUpdateExtension(
            Catalog catalog,
            Extension extension,
            Release release,
            boolean installOptionalDependencies,
            Consumer<Float> onProgress,
            BiConsumer<InstallationStep, String> onStatusChanged
    ) throws IOException, InterruptedException, ExecutionException {
        if (extension.getReleases().stream().noneMatch(release::equals)) {
            throw new IllegalArgumentException(String.format(
                    "The provided release %s does not belong to the provided extension %s",
                    release,
                    extension
            ));
        }

        removeExtension(catalog, extension);

        downloadAndExtractLinks(
                getDownloadUrlsToFilePaths(
                        catalog.getName(),
                        extension.getName(),
                        release,
                        installOptionalDependencies,
                        true
                ),
                onProgress,
                onStatusChanged
        );

        extension.installRelease(release, installOptionalDependencies);

        try {
            extensionFolderManager.saveRegistry(Registry.createFromCatalogs(catalogs).get());
        } catch (Exception e) {
            extension.uninstallRelease();
            throw e;
        }

        updateCatalogManagedInstalledJarsOfDirectory(
                extensionFolderManager.getExtensionDirectoryPath(catalog.getName(), extension.getName(), release.getVersion().toString()),
                Operation.ADD
        );

        logger.info("{} of {} installed", extension, catalog);
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were added with function of this class. This
     * list can be updated from any thread
     */
    public ObservableList<Path> getCatalogManagedInstalledJars() {
        return catalogManagedInstalledJarsImmutable;
    }

    /**
     * Extension classes are automatically loaded with a custom class loader. This function returns it.
     *
     * @return the class loader user to load extensions
     */
    public ClassLoader getExtensionClassLoader() {
        return extensionClassLoader;
    }

    /**
     * Set a runnable to be called each time a JAR file is loaded by {@link #getExtensionClassLoader()}. The call may
     * happen from any thread. Note that the runnable may be called a few seconds after a JAR is added.
     *
     * @param runnable the runnable to run when a JAR file is loaded
     * @throws NullPointerException if the provided path is null
     */
    public void addOnJarLoadedRunnable(Runnable runnable) {
        extensionClassLoader.addOnJarLoadedRunnable(runnable);
    }

    /**
     * Get a list of updates available on the currently installed extensions (with catalogs, extensions manually installed
     * are not considered).
     *
     * @return a CompletableFuture with a list of available updates, or a failed CompletableFuture if the update query
     * failed
     */
    public synchronized CompletableFuture<List<UpdateAvailable>> getAvailableUpdates() {
        return CompletableFuture.supplyAsync(() -> catalogs.stream()
                .map(catalog -> catalog.getExtensions().join().stream()
                        .map(extension -> extension.getUpdateAvailable(version))
                        .flatMap(Optional::stream)
                        .toList()
                )
                .flatMap(List::stream)
                .toList()
        );
    }

    /**
     * Uninstall an extension and attempt to remove its files. This can take some time but the operation can be interrupted.
     * <p>
     * Warning: this will attempt to move the directory returned by {@link #getExtensionDirectory(String, String)} to
     * trash or recursively delete it if moving files to trash is not supported by this platform. If this operation fails,
     * the function doesn't throw any exceptions.
     *
     * @param catalog the catalog owning the extension to uninstall. It must be one of {@link #getCatalogs()}
     * @param extension the extension to uninstall. It must belong to the provided catalog
     * @throws IllegalArgumentException if the provided catalog does not belong to {@link #getCatalogs()}, or if the provided
     * extension does not belong to the provided catalog
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionsDirectory()}
     * is null
     * @throws IOException if an I/O error occurred while deleting the extension
     * @throws InvalidPathException if a path cannot be created, for example because the extensions folder path contain
     * invalid characters
     * @throws InterruptedException if the calling thread is interrupted
     * @throws ExecutionException if an error occurs while retrieving the extensions of the provided catalog
     */
    public synchronized void removeExtension(Catalog catalog, Extension extension) throws IOException, ExecutionException, InterruptedException {
        if (catalogs.stream().noneMatch(catalog::equals)) {
            throw new IllegalArgumentException(String.format(
                    "The provided catalog %s is not among the internal list %s",
                    catalog,
                    catalogs
            ));
        }
        if (catalog.getExtensions().get().stream().noneMatch(extension::equals)) {
            throw new IllegalArgumentException(String.format(
                    "The provided extension %s does not belong to the provided catalog %s",
                    extension,
                    catalog
            ));
        }

        if (extension.getInstalledRelease().getValue().isEmpty()) {
            logger.debug("{} is not installed. Skipping deletion of it", extension);
            return;
        }

        extension.uninstallRelease();
        try {
            extensionFolderManager.saveRegistry(Registry.createFromCatalogs(catalogs).get());
        } catch (Exception e) {
            extension.installRelease(
                    extension.getInstalledRelease().getValue().get(),
                    extension.areOptionalDependenciesInstalled().get()
            );
            throw e;
        }

        updateCatalogManagedInstalledJarsOfDirectory(
                extensionFolderManager.getExtensionDirectoryPath(catalog.getName(), extension.getName()),
                Operation.REMOVE
        );

        try {
            extensionFolderManager.deleteExtension(catalog.getName(), extension.getName());
        } catch (Exception e) {
            logger.debug("Error while removing files of {}. They won't be deleted", extension, e);
        }

        logger.info("{} of {} removed", extension, catalog);
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were manually added (i.e. not with a catalog)
     * to the extension directory. This list can be updated from any thread. Note that this list can take a few seconds
     * to update when a JAR is added or removed
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return extensionFolderManager.getManuallyInstalledJars();
    }

    private void resetCatalogsAndJars(List<Catalog> defaultCatalogs) {
        List<RegistryCatalog> catalogs;
        try {
            catalogs = extensionFolderManager.getSavedRegistry().catalogs();
            this.catalogs.setAll(catalogs.stream()
                    .map(Catalog::new)
                    .toList()
            );
        } catch (Exception e) {
            logger.debug("Cannot retrieve saved registry. Using default catalogs {}", defaultCatalogs, e);

            catalogs = List.of();
            this.catalogs.setAll(defaultCatalogs);
        }

        catalogManagedInstalledJars.clear();
        List<Path> releasePaths = catalogs.stream()
                .flatMap(catalog -> catalog.extensions().stream()
                        .map(extension -> extensionFolderManager.getExtensionDirectoryPath(
                                catalog.name(),
                                extension.name(),
                                extension.installedVersion())
                        )
                )
                .toList();
        for (Path releasePath: releasePaths) {
            updateCatalogManagedInstalledJarsOfDirectory(releasePath, Operation.ADD);
        }
    }

    private void loadJars() {
        addJars(extensionFolderManager.getManuallyInstalledJars());
        extensionFolderManager.getManuallyInstalledJars().addListener((ListChangeListener<? super Path>) change -> {
            while (change.next()) {
                addJars(change.getAddedSubList());
            }
            change.reset();
        });

        addJars(catalogManagedInstalledJarsImmutable);
        catalogManagedInstalledJarsImmutable.addListener((ListChangeListener<? super Path>) change -> {
            while (change.next()) {
                addJars(change.getAddedSubList());
            }
            change.reset();
        });
    }

    private void updateCatalogManagedInstalledJarsOfDirectory(Path directory, Operation operation) {
        if (directory != null && directory.toFile().exists()) {
            try (Stream<Path> files = Files.walk(directory)) {
                List<Path> jars = files.filter(path -> path.toString().endsWith(".jar")).toList();

                switch (operation) {
                    case ADD -> catalogManagedInstalledJars.addAll(jars);
                    case REMOVE -> catalogManagedInstalledJars.removeAll(jars);
                }
            } catch (IOException e) {
                logger.debug(
                        "Error while finding jars located in {}. None will be {}",
                        directory,
                        switch (operation) {
                            case ADD -> "added";
                            case REMOVE -> "removed";
                        },
                        e
                );
            }
        }
    }

    private List<UriFileName> getDownloadUrlsToFilePaths(
            String catalogName,
            String extensionName,
            Release release,
            boolean installOptionalDependencies,
            boolean createDirectories
    ) throws IOException {
        List<UriFileName> downloadUrlToFilePaths = new ArrayList<>();

        downloadUrlToFilePaths.add(new UriFileName(
                release.getMainUrl(),
                Paths.get(
                        extensionFolderManager.getExtensionPath(
                                catalogName,
                                extensionName,
                                release.getVersion().toString(),
                                ExtensionFolderManager.FileType.MAIN_JAR,
                                createDirectories
                        ).toString(),
                        FileTools.getFileNameFromURI(release.getMainUrl())
                )
        ));

        for (URI javadocUri: release.getJavadocUrls()) {
            downloadUrlToFilePaths.add(new UriFileName(
                    javadocUri,
                    Paths.get(
                            extensionFolderManager.getExtensionPath(
                                    catalogName,
                                    extensionName,
                                    release.getVersion().toString(),
                                    ExtensionFolderManager.FileType.JAVADOCS,
                                    createDirectories
                            ).toString(),
                            FileTools.getFileNameFromURI(javadocUri)
                    )
            ));
        }

        for (URI requiredDependencyUri: release.getRequiredDependencyUrls()) {
            downloadUrlToFilePaths.add(new UriFileName(
                    requiredDependencyUri,
                    Paths.get(
                            extensionFolderManager.getExtensionPath(
                                    catalogName,
                                    extensionName,
                                    release.getVersion().toString(),
                                    ExtensionFolderManager.FileType.REQUIRED_DEPENDENCIES,
                                    createDirectories
                            ).toString(),
                            FileTools.getFileNameFromURI(requiredDependencyUri)
                    )
            ));
        }

        if (installOptionalDependencies) {
            for (URI optionalDependencyUri: release.getOptionalDependencyUrls()) {
                downloadUrlToFilePaths.add(new UriFileName(
                        optionalDependencyUri,
                        Paths.get(
                                extensionFolderManager.getExtensionPath(
                                        catalogName,
                                        extensionName,
                                        release.getVersion().toString(),
                                        ExtensionFolderManager.FileType.OPTIONAL_DEPENDENCIES,
                                        createDirectories
                                ).toString(),
                                FileTools.getFileNameFromURI(optionalDependencyUri)
                        )
                ));
            }
        }

        return downloadUrlToFilePaths;
    }

    private void downloadAndExtractLinks(
            List<UriFileName> downloadUrlToFilePath,
            Consumer<Float> onProgress,
            BiConsumer<InstallationStep, String> onStatusChanged
    ) throws IOException, InterruptedException {
        int i = 0;
        for (var uriFileName: downloadUrlToFilePath) {
            float progressOffset = (float) i / downloadUrlToFilePath.size();
            boolean downloadingZipArchive = uriFileName.filePath().toString().endsWith(".zip");

            if (downloadingZipArchive) {
                logger.debug("Downloading and extracting {} to {}", uriFileName.uri(), uriFileName.filePath());
            } else {
                logger.debug("Downloading {} to {}", uriFileName.uri(), uriFileName.filePath());
            }

            float step = downloadingZipArchive ?
                    (float) 1 / (2 * downloadUrlToFilePath.size()) :
                    (float) 1 / downloadUrlToFilePath.size();

            onStatusChanged.accept(InstallationStep.DOWNLOADING, uriFileName.uri().toString());
            FileDownloader.downloadFile(
                    uriFileName.uri(),
                    uriFileName.filePath(),
                    progress -> onProgress.accept(progressOffset + progress * step)
            );

            if (downloadingZipArchive) {
                onStatusChanged.accept(InstallationStep.EXTRACTING_ZIP, uriFileName.filePath().toString());
                ZipExtractor.extractZipToFolder(
                        uriFileName.filePath(),
                        uriFileName.filePath().getParent(),
                        progress -> onProgress.accept(progressOffset + step + progress * step)
                );
            }

            i++;
        }
    }

    private void addJars(List<? extends Path> jarPaths) {
        for (Path path: jarPaths) {
            try {
                extensionClassLoader.addJar(path);
            } catch (IOError | MalformedURLException e) {
                logger.error("Cannot load extension {}", path, e);
            }
        }
    }
}
