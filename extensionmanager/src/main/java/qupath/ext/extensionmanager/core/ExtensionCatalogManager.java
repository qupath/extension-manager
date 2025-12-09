package qupath.ext.extensionmanager.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.catalog.Catalog;
import qupath.ext.extensionmanager.core.model.CatalogModelFetcher;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.core.savedentities.UpdateAvailable;
import qupath.ext.extensionmanager.core.registry.RegistryCatalog;
import qupath.ext.extensionmanager.core.registry.RegistryManager;
import qupath.ext.extensionmanager.core.tools.FileDownloader;
import qupath.ext.extensionmanager.core.tools.FileTools;
import qupath.ext.extensionmanager.core.tools.ZipExtractor;
import qupath.ext.extensionmanager.core.model.ExtensionModel;
import qupath.ext.extensionmanager.core.model.ReleaseModel;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;

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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final Map<CatalogExtension, ObjectProperty<Optional<InstalledExtension>>> installedExtensions = new ConcurrentHashMap<>();
    private final ObservableList<Path> catalogManagedInstalledJars = FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<Path> catalogManagedInstalledJarsImmutable = FXCollections.unmodifiableObservableList(catalogManagedInstalledJars);
    private final ExtensionFolderManager extensionFolderManager;
    private final RegistryManager registryManager;
    private final ExtensionClassLoader extensionClassLoader;
    private final Version version;
    private record CatalogExtension(String catalogName, String extensionName) {}
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
     * @param extensionDirectoryPath a read-only property pointing to the path the extension directory should have. The
     *                               path can be null or invalid (but not the property). If this property is changed,
     *                               catalogs and extensions will be set to the content of the new value of the property
     *                               (so will be reset if the new path is empty)
     * @param parentClassLoader the class loader that should be the parent of the extension class loader. Can be null to use
     *                          the bootstrap class loader
     * @param version a text describing the release of the current software with the form "v[MAJOR].[MINOR].[PATCH]" or
     *                "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]". It will determine which extensions are compatible
     * @param defaultCatalogs a list of catalogs this manager should use by default, i.e. when no catalog or extension is
     *                        installed
     * @throws IllegalArgumentException if the provided version doesn't meet the specified requirements
     * @throws SecurityException if the user doesn't have enough rights to create the extension class loader
     * @throws NullPointerException if one of the parameters (except the class loader) is null
     */
    public ExtensionCatalogManager(
            ReadOnlyObjectProperty<Path> extensionDirectoryPath,
            ClassLoader parentClassLoader,
            String version,
            List<Catalog> defaultCatalogs
    ) {
        this.extensionFolderManager = new ExtensionFolderManager(extensionDirectoryPath);
        this.registryManager = new RegistryManager(extensionFolderManager.getCatalogsDirectoryPath());
        this.extensionClassLoader = new ExtensionClassLoader(parentClassLoader);
        this.version = new Version(version);

        extensionDirectoryPath.addListener((p, o, n) -> {
            synchronized (this) {
                // TODO: update catalogs with the content of the new registry
                this.catalogs.setAll(registryManager.getSavedCatalogs());

                for (CatalogExtension catalogExtension : installedExtensions.keySet()) {
                    installedExtensions.get(catalogExtension).set(getInstalledExtension(catalogExtension));
                }
            }
        });

        updateCatalogManagedInstalledJarsOfDirectory(extensionFolderManager.getCatalogsDirectoryPath().getValue(), Operation.ADD);
        extensionFolderManager.getCatalogsDirectoryPath().addListener((p, o, n) -> {
            catalogManagedInstalledJars.clear();

            updateCatalogManagedInstalledJarsOfDirectory(n, Operation.ADD);
        });

        loadJars();
    }

    @Override
    public void close() throws Exception {
        this.extensionClassLoader.close();
        this.registryManager.close();
        this.extensionFolderManager.close();
    }

    /**
     * @return the version of the current software, as given in {@link #ExtensionCatalogManager(ReadOnlyObjectProperty, ClassLoader, String, List)}
     */
    public Version getVersion() {
        return version;
    }

    /**
     * @return a read only property containing the path to the extension folder. It may be updated from any thread and the
     * path (but not the property) can be null or invalid
     */
    public ReadOnlyObjectProperty<Path> getExtensionDirectory() {
        return extensionFolderManager.getExtensionDirectoryPath();
    }

    /**
     * Get the path to the directory containing the provided catalog.
     *
     * @param catalogName the name of the catalog to retrieve
     * @return the path of the directory containing the provided catalog
     * @throws InvalidPathException if the path cannot be created
     * @throws NullPointerException if the provided catalog is null or if the path contained in {@link #getExtensionDirectory()}
     * is null
     */
    public Path getCatalogDirectory(String catalogName) {
        return extensionFolderManager.getCatalogDirectoryPath(catalogName);
    }

    /**
     * Add and save a catalog.
     * <p>
     * No check will be performed concerning whether the provided catalog points to a valid catalog.
     *
     * @param catalog the catalog to add. It must have a different name from the ones returned by {@link #getCatalogs()}
     * @throws IllegalArgumentException if a catalog with the same name already exists
     * @throws IOException if an I/O error occurs while saving the catalogs to disk
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectory()} is null or if the provided
     * catalog is null
     */
    public void addCatalog(RegistryCatalog catalog) throws IOException {
        registryManager.addCatalog(catalog);

        logger.info("Catalog {} added", catalog.name());
    }

    /**
     * Get the catalogs added or removed with {@link #addCatalog(RegistryCatalog)} and {@link #removeCatalogs(List, boolean)}.
     * This list may be updated from any thread and won't contain null elements.
     *
     * @return a read-only observable list of all saved catalogs
     */
    public ObservableList<RegistryCatalog> getCatalogs() {
        return registryManager.getCatalogs();
    }

    /**
     * Remove the provided catalog from the list of saved catalogs.
     * <p>
     * Warning: this will move the directory returned by {@link #getCatalogDirectory(SavedCatalog)} to trash if supported
     * by this platform or recursively delete it if extension are asked to be removed.
     *
     * @param catalog the catalog to remove
     * @throws IllegalArgumentException if the provided catalog is not {@link RegistryCatalog#deletable()}
     * @throws IOException if an I/O error occurs while removing the catalog from disk
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectory()} is null or if the provided
     * catalog is null
     */
    public void removeCatalogs(RegistryCatalog catalog) throws IOException {
        registryManager.removeCatalog(catalog);

        if (removeExtensions) {
            for (SavedCatalog savedCatalog : catalogsToRemove) {
                try {
                    extensionFolderManager.deleteExtensionsFromCatalog(savedCatalog);
                } catch (IOException | SecurityException | InvalidPathException | NullPointerException e) {
                    logger.debug("Could not delete {}", savedCatalog.name(), e);
                }
            }

            for (var entry: installedExtensions.entrySet()) {
                if (catalogsToRemove.contains(entry.getKey().savedCatalog)) {
                    synchronized (this) {
                        entry.getValue().set(Optional.empty());
                    }
                }
            }
        }

        logger.info("Catalog {} removed", catalog.name());
    }

    /**
     * Get the path to the directory containing the provided extension of the provided catalog. This will also create the
     * directory containing all installed extensions of the provided catalog if it doesn't already exist (but the returned
     * directory is not guaranteed to be created).
     *
     * @param catalogName the name of the catalog owning the extension
     * @param extensionName the name of the extension to retrieve
     * @return the path to the folder containing the provided extension
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws InvalidPathException if the path cannot be created
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionDirectory()}
     * is null
     */
    public Path getExtensionDirectory(String catalogName, String extensionName) throws IOException {
        return extensionFolderManager.getExtensionDirectoryPath(catalogName, extensionName);
    }

    /**
     * Get the list of links the {@link #installOrUpdateExtension(SavedCatalog, ExtensionModel, InstalledExtension, Consumer, BiConsumer)}
     * function will download to install the provided extension.
     *
     * @param savedCatalog the catalog owning the extension to install
     * @param extension the extension to install
     * @param installationInformation what to install on the extension
     * @return the list URIs that will be downloaded to install the extension with the provided parameters
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionDirectory()}
     * is null
     * @throws IOException if an I/O error occurred while deleting, downloading or installing the extension
     * @throws InvalidPathException if a path cannot be created, for example because the extensions folder path contain
     * invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to install or update the extension
     * @throws IllegalArgumentException if the release name of the provided installation information cannot be found in
     * the releases of the provided extension
     */
    public List<URI> getDownloadLinks(SavedCatalog savedCatalog, ExtensionModel extension, InstalledExtension installationInformation) throws IOException {
        return getDownloadUrlsToFilePaths(savedCatalog, extension, installationInformation, false).stream()
                .map(UriFileName::uri)
                .toList();
    }

    /**
     * Install (or update if it already exists) an extension. This may take a lot of time depending on the internet connection
     * and the size of the extension, but this operation is cancellable.
     * <p>
     * If the extension already exists, it will be deleted before downloading the provided version of the extension.
     * <p>
     * Warning: this will move to trash the directory returned by {@link #getExtensionDirectory(SavedCatalog, ExtensionModel)}
     * or recursively delete it if moving files to trash is not supported.
     *
     * @param savedCatalog the catalog owning the extension to install/update
     * @param extension the extension to install/update
     * @param installationInformation what to install/update on the extension
     * @param onProgress a function that will be called at different steps during the installation. Its parameter
     *                   will be a float between 0 and 1 indicating the progress of the installation (0: beginning,
     *                   1: finished). This function will be called from the calling thread
     * @param onStatusChanged a function that will be called at different steps during the installation. Its first parameter
     *                        will be the step currently happening, and its second parameter a text describing the resource
     *                        on which the step is happening (for example, a link if the step is a download). This function
     *                        will be called from the calling thread
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionDirectory()}
     * is null
     * @throws IOException if an I/O error occurred while deleting, downloading or installing the extension
     * @throws InvalidPathException if a path cannot be created, for example because the extensions folder path contain
     * invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to install or update the extension
     * @throws IllegalArgumentException if the release name of the provided installation information cannot be found in
     * the releases of the provided extension
     * @throws InterruptedException if the calling thread is interrupted
     */
    public void installOrUpdateExtension(
            SavedCatalog savedCatalog,
            ExtensionModel extension,
            InstalledExtension installationInformation,
            Consumer<Float> onProgress,
            BiConsumer<InstallationStep, String> onStatusChanged
    ) throws IOException, InterruptedException {
        var extensionProperty = installedExtensions.computeIfAbsent(
                new CatalogExtension(savedCatalog, extension),
                e -> new SimpleObjectProperty<>()
        );

        logger.debug("Deleting files of {} before installing or updating it", extension.name());
        updateCatalogManagedInstalledJarsOfDirectory(
                extensionFolderManager.getExtensionDirectoryPath(savedCatalog, extension),
                Operation.REMOVE
        );
        extensionFolderManager.deleteExtension(savedCatalog, extension);
        synchronized (this) {
            extensionProperty.set(Optional.empty());
        }

        try {
            downloadAndExtractLinks(
                    getDownloadUrlsToFilePaths(savedCatalog, extension, installationInformation, true),
                    onProgress,
                    onStatusChanged
            );
        } catch (Exception e) {
            logger.debug("Installation of {} failed. Clearing extension files", extension.name());
            extensionFolderManager.deleteExtension(savedCatalog, extension);
            throw e;
        }
        updateCatalogManagedInstalledJarsOfDirectory(
                extensionFolderManager.getExtensionDirectoryPath(savedCatalog, extension),
                Operation.ADD
        );

        synchronized (this) {
            extensionProperty.set(Optional.of(installationInformation));
        }

        logger.info("{} of {} installed", extension.name(), savedCatalog.name());
    }

    /**
     * Indicate whether an extension belonging to a catalog is installed.
     *
     * @param catalogName the name of the catalog owning the extension to find
     * @param extensionName the name of the extension to get installed information on
     * @return a read-only object property containing an Optional of an installed extension. If the Optional is empty,
     * then it means the extension is not installed. This property may be updated from any thread
     */
    public ReadOnlyObjectProperty<Optional<InstalledExtension>> getInstalledExtension(String catalogName, String extensionName) {
        return installedExtensions.computeIfAbsent(
                new CatalogExtension(catalogName, extensionName),
                catalogExtension -> new SimpleObjectProperty<>(getInstalledExtension(catalogExtension))
        );
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were added with catalogs to the extension
     * directory. This list can be updated from any thread. Note that this list can take a few seconds to update when a
     * JAR is added or removed
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
    public CompletableFuture<List<UpdateAvailable>> getAvailableUpdates() {
        return CompletableFuture.supplyAsync(() -> catalogs.stream()
                .map(savedCatalog -> CatalogModelFetcher.getCatalog(savedCatalog.rawUri()).join().extensions().stream()
                        .map(extension -> getUpdateAvailable(savedCatalog, extension))
                        .filter(Objects::nonNull)
                        .toList()
                )
                .flatMap(List::stream)
                .toList());
    }

    /**
     * Uninstall an extension by removing its files. This can take some time depending on the number of files to delete
     * and the speed of the disk.
     * <p>
     * Warning: this will move the directory returned by {@link #getExtensionDirectory(SavedCatalog, ExtensionModel)} to
     * trash or recursively delete it if moving files to trash is not supported by this platform.
     *
     * @param savedCatalog the catalog owning the extension to uninstall
     * @param extension the extension to uninstall
     * @throws IOException if an I/O error occurs while deleting the folder
     * @throws InvalidPathException if the path of the extension folder cannot be created, for example because the extension
     * name contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to delete the extension files
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectory()} is null, or if one of
     * the parameters is null
     */
    public void removeExtension(SavedCatalog savedCatalog, ExtensionModel extension) throws IOException {
        var extensionProperty = installedExtensions.computeIfAbsent(
                new CatalogExtension(savedCatalog, extension),
                e -> new SimpleObjectProperty<>()
        );

        updateCatalogManagedInstalledJarsOfDirectory(
                extensionFolderManager.getExtensionDirectoryPath(savedCatalog, extension),
                Operation.REMOVE
        );
        extensionFolderManager.deleteExtension(savedCatalog, extension);
        synchronized (this) {
            extensionProperty.set(Optional.empty());
        }

        logger.info("{} of {} removed", extension.name(), savedCatalog.name());
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were manually added (i.e. not with a catalog)
     * to the extension directory. This list can be updated from any thread. Note that this list can take a few seconds
     * to update when a JAR is added or removed
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return extensionFolderManager.getManuallyInstalledJars();
    }

    private void updateCatalogManagedInstalledJarsOfDirectory(Path directory, Operation operation) {
        if (directory != null) {
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

    private Optional<InstalledExtension> getInstalledExtension(CatalogExtension catalogExtension) {
        try {
            return extensionFolderManager.getInstalledExtension(
                    catalogExtension.catalogName(),
                    catalogExtension.extensionName()
            );
        } catch (IOException | InvalidPathException | SecurityException | NullPointerException e) {
            logger.debug("Error while retrieving {} installation information", catalogExtension.extensionName(), e);
            return Optional.empty();
        }
    }

    private void loadJars() {
        addJars(extensionFolderManager.getManuallyInstalledJars());
        extensionFolderManager.getManuallyInstalledJars().addListener((ListChangeListener<? super Path>) change -> {
            while (change.next()) {
                addJars(change.getAddedSubList());
                removeJars(change.getRemoved());
            }
            change.reset();
        });

        addJars(catalogManagedInstalledJarsImmutable);
        catalogManagedInstalledJarsImmutable.addListener((ListChangeListener<? super Path>) change -> {
            while (change.next()) {
                addJars(change.getAddedSubList());
                removeJars(change.getRemoved());
            }
            change.reset();
        });
    }

    private List<UriFileName> getDownloadUrlsToFilePaths(
            SavedCatalog savedCatalog,
            ExtensionModel extension,
            InstalledExtension installationInformation,
            boolean createFolders
    ) throws IOException {
        List<UriFileName> downloadUrlToFilePaths = new ArrayList<>();

        Optional<ReleaseModel> release = extension.releases().stream()
                .filter(r -> r.name().equals(installationInformation.releaseName()))
                .findAny();

        if (release.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "The provided release name %s is not present in the extension releases %s",
                    installationInformation.releaseName(),
                    extension.releases()
            ));
        }

        downloadUrlToFilePaths.add(new UriFileName(
                release.get().mainUrl(),
                Paths.get(
                        extensionFolderManager.getExtensionPath(
                                savedCatalog,
                                extension,
                                release.get().name(),
                                ExtensionFolderManager.FileType.MAIN_JAR,
                                createFolders
                        ).toString(),
                        FileTools.getFileNameFromURI(release.get().mainUrl())
                )
        ));

        for (URI javadocUri: release.get().javadocUrls()) {
            downloadUrlToFilePaths.add(new UriFileName(
                    javadocUri,
                    Paths.get(
                            extensionFolderManager.getExtensionPath(
                                    savedCatalog,
                                    extension,
                                    release.get().name(),
                                    ExtensionFolderManager.FileType.JAVADOCS,
                                    createFolders
                            ).toString(),
                            FileTools.getFileNameFromURI(javadocUri)
                    )
            ));
        }

        for (URI requiredDependencyUri: release.get().requiredDependencyUrls()) {
            downloadUrlToFilePaths.add(new UriFileName(
                    requiredDependencyUri,
                    Paths.get(
                            extensionFolderManager.getExtensionPath(
                                    savedCatalog,
                                    extension,
                                    release.get().name(),
                                    ExtensionFolderManager.FileType.REQUIRED_DEPENDENCIES,
                                    createFolders
                            ).toString(),
                            FileTools.getFileNameFromURI(requiredDependencyUri)
                    )
            ));
        }

        if (installationInformation.optionalDependenciesInstalled()) {
            for (URI optionalDependencyUri: release.get().optionalDependencyUrls()) {
                downloadUrlToFilePaths.add(new UriFileName(
                        optionalDependencyUri,
                        Paths.get(
                                extensionFolderManager.getExtensionPath(
                                        savedCatalog,
                                        extension,
                                        release.get().name(),
                                        ExtensionFolderManager.FileType.OPTIONAL_DEPENDENCIES,
                                        createFolders
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

    private UpdateAvailable getUpdateAvailable(SavedCatalog savedCatalog, ExtensionModel extension) {
        Optional<InstalledExtension> installedExtension = getInstalledExtension(
                new CatalogExtension(savedCatalog, extension)
        );

        if (installedExtension.isPresent()) {
            String installedRelease = installedExtension.get().releaseName();
            Optional<ReleaseModel> maxCompatibleRelease = extension.getMaxCompatibleRelease(version);

            if (maxCompatibleRelease.isPresent() &&
                    new Version(maxCompatibleRelease.get().name()).compareTo(new Version(installedRelease)) > 0
            ) {
                logger.debug(
                        "{} installed and updatable to {}",
                        extension.name(),
                        maxCompatibleRelease.get().name()
                );
                return new UpdateAvailable(
                        extension.name(),
                        installedExtension.get().releaseName(),
                        maxCompatibleRelease.get().name()
                );
            } else {
                logger.debug(
                        "{} installed but no compatible update found",
                        extension.name()
                );
                return null;
            }
        } else {
            logger.debug("{} not installed, so no update available", extension.name());
            return null;
        }
    }

    private void addJars(List<? extends Path> jarPaths) {
        for (Path path: jarPaths) {
            try {
                extensionClassLoader.addJar(path);
            } catch (IOError | SecurityException | MalformedURLException e) {
                logger.error("Cannot load extension {}", path, e);
            }
        }
    }

    private void removeJars(List<? extends Path> jarPaths) {
        for (Path path: jarPaths) {
            extensionClassLoader.removeJar(path);
        }
    }
}
