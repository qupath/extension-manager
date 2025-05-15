package qupath.ext.extensionmanager.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.catalog.CatalogFetcher;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.core.savedentities.UpdateAvailable;
import qupath.ext.extensionmanager.core.tools.FileDownloader;
import qupath.ext.extensionmanager.core.tools.FileTools;
import qupath.ext.extensionmanager.core.tools.ZipExtractor;
import qupath.ext.extensionmanager.core.catalog.Extension;
import qupath.ext.extensionmanager.core.catalog.Release;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.Registry;

import java.io.IOError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
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
import java.util.stream.Collectors;

/**
 * A manager for catalogs and extensions. It can be used to get access to all saved catalogs,
 * add or remove a catalog, get access to all installed extensions, and install or delete an extension.
 * Manually installed extensions are automatically detected.
 * <p>
 * It also automatically loads extension classes with a custom ClassLoader (see {@link #getExtensionClassLoader()}).
 * Note that removed extensions are not unloaded from the class loader.
 * <p>
 * The list of active catalogs and installed extensions is determined by this class. It is internally saved
 * in a registry JSON file located in the extension directory (see {@link Registry}).
 * <p>
 * This class is thread-safe.
 * <p>
 * This manager must be {@link #close() closed} once no longer used.
 */
public class ExtensionCatalogManager implements AutoCloseable{

    private static final Logger logger = LoggerFactory.getLogger(ExtensionCatalogManager.class);
    private final ObservableList<SavedCatalog> savedCatalogs = FXCollections.observableList(new CopyOnWriteArrayList<>());
    private final ObservableList<SavedCatalog> savedCatalogsImmutable = FXCollections.unmodifiableObservableList(savedCatalogs);
    private final Map<CatalogExtension, ObjectProperty<Optional<InstalledExtension>>> installedExtensions = new ConcurrentHashMap<>();
    private final ExtensionFolderManager extensionFolderManager;
    private final ExtensionClassLoader extensionClassLoader;
    private final String version;
    private final Registry defaultRegistry;
    private record CatalogExtension(SavedCatalog savedCatalog, Extension extension) {}
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

    /**
     * Create the extension catalog manager.
     *
     * @param extensionDirectoryPath a read-only property pointing to the path the extension directory should have. The
     *                               path can be null or invalid (but not the property). If this property is changed,
     *                               catalogs and extensions will be set to the content of the new value of the property
     *                               (so will be reset if the new path is empty)
     * @param parentClassLoader the class loader that should be the parent of the extension class loader. Can be null to use
     *                          the bootstrap class loader
     * @param version a text describing the release of the current software with the form "v[MAJOR].[MINOR].[PATCH]"
     *                or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]". It will determine which extensions are
     *                compatible
     * @param defaultRegistry the default registry to use when the saved one cannot be used. Can be null
     * @throws IllegalArgumentException if the provided version doesn't meet the specified requirements
     * @throws SecurityException if the user doesn't have enough rights to create the extension class loader
     * @throws NullPointerException if extensionDirectoryPath or version is null
     */
    public ExtensionCatalogManager(
            ReadOnlyObjectProperty<Path> extensionDirectoryPath,
            ClassLoader parentClassLoader,
            String version,
            Registry defaultRegistry
    ) {
        Version.isValid(version, true);

        this.extensionFolderManager = new ExtensionFolderManager(extensionDirectoryPath);
        this.extensionClassLoader = new ExtensionClassLoader(parentClassLoader);
        this.version = version;
        this.defaultRegistry = defaultRegistry;

        setCatalogsFromRegistry();
        extensionDirectoryPath.addListener((p, o, n) -> {
            setCatalogsFromRegistry();

            synchronized (this) {
                for (CatalogExtension catalogExtension : installedExtensions.keySet()) {
                    installedExtensions.get(catalogExtension).set(getInstalledExtension(catalogExtension));
                }
            }
        });

        loadJars();
    }

    @Override
    public void close() throws Exception {
        this.extensionFolderManager.close();
        this.extensionClassLoader.close();
    }

    /**
     * @return a read only property containing the path to the extension folder.
     * It may be updated from any thread and the path (but not the property) can
     * be null or invalid
     */
    public ReadOnlyObjectProperty<Path> getExtensionDirectoryPath() {
        return extensionFolderManager.getExtensionDirectoryPath();
    }

    /**
     * @return a text describing the release of the current software with the form "v[MAJOR].[MINOR].[PATCH]"
     * or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the path to the directory containing the provided catalog. This will also create the
     * directory containing all catalogs and installed extensions if it doesn't already exist
     * (but the returned directory is not guaranteed to exist).
     *
     * @param savedCatalog the catalog to retrieve
     * @return the path of the directory containing the provided catalog
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws InvalidPathException if the path cannot be created
     * @throws SecurityException if the user doesn't have enough rights to create the directory
     * @throws NullPointerException if the provided catalog is null or if the path contained in
     * {@link #getExtensionDirectoryPath()} is null
     */
    public Path getCatalogDirectory(SavedCatalog savedCatalog) throws IOException {
        return extensionFolderManager.getCatalogDirectoryPath(savedCatalog);
    }

    /**
     * Add catalogs to the available list. This will save them to the registry.
     * Catalogs with the same name as an already existing catalog will not be added.
     * No check will be performed concerning whether the provided catalogs point to
     * valid catalogs.
     * <p>
     * If an exception occurs (see below), the provided catalogs are not added.
     *
     * @param savedCatalogs the catalogs to add. They must have different names
     * @throws IOException if an I/O error occurs while saving the registry file. In that case,
     * the provided catalogs are not added
     * @throws SecurityException if the user doesn't have sufficient rights to save the registry file
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null,
     * if the provided list of catalogs is null or if one of the provided catalog is null
     * @throws IllegalArgumentException if at least two of the provided catalogs have the same name
     */
    public void addCatalog(List<SavedCatalog> savedCatalogs) throws IOException {
        if (savedCatalogs.stream().map(SavedCatalog::name).collect(Collectors.toSet()).size() < savedCatalogs.size()) {
            throw new IllegalArgumentException(String.format(
                    "Two of the provided catalogs %s have the same name",
                    savedCatalogs
            ));
        }
        if (getExtensionDirectoryPath().get() == null) {
            throw new NullPointerException("The extension directory path is null");
        }

        List<SavedCatalog> catalogsToAdd;
        synchronized (this) {
            catalogsToAdd = savedCatalogs.stream()
                    .filter(savedCatalog -> {
                        if (this.savedCatalogs.stream().noneMatch(catalog -> catalog.name().equals(savedCatalog.name()))) {
                            return true;
                        } else {
                            logger.warn("{} has the same name as an existing catalog and will not be added", savedCatalog.name());
                            return false;
                        }
                    })
                    .toList();

            if (catalogsToAdd.isEmpty()) {
                logger.debug("No catalog to add");
                return;
            }

            this.savedCatalogs.addAll(catalogsToAdd);
        }

        try {
            extensionFolderManager.saveRegistry(new Registry(this.savedCatalogs));
        } catch (IOException | SecurityException | NullPointerException e) {
            this.savedCatalogs.removeAll(catalogsToAdd);

            throw e;
        }

        logger.info("Catalogs {} added", catalogsToAdd.stream().map(SavedCatalog::name).toList());
    }

    /**
     * Get the catalogs added or removed with {@link #addCatalog(List)} and {@link #removeCatalogs(List, boolean)}.
     * This list may be updated from any thread and won't contain null elements.
     *
     * @return a read-only observable list of all saved catalogs
     */
    public ObservableList<SavedCatalog> getCatalogs() {
        return savedCatalogsImmutable;
    }

    /**
     * Remove catalogs from the available list. This will remove them from the
     * saved registry and may delete any installed extension belonging to these catalogs.
     * <p>
     * Catalogs that are not deletable (see {@link SavedCatalog#deletable()}) won't be
     * deleted.
     * <p>
     * If an exception occurs (see below), the provided catalogs are not added.
     * <p>
     * Warning: this will move the directory returned by {@link #getCatalogDirectory(SavedCatalog)} to
     * trash if supported by this platform or recursively delete it if extension are asked to be removed.
     *
     * @param savedCatalogs the catalogs to remove
     * @param removeExtensions whether to remove extensions belonging to the catalogs to remove
     * @throws IOException if an I/O error occurs while saving the registry file. In that case,
     * the provided catalogs are not added
     * @throws SecurityException if the user doesn't have sufficient rights to save the registry file
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null,
     * if the provided list of catalogs is null or if one of the provided catalog is null
     */
    public void removeCatalogs(List<SavedCatalog> savedCatalogs, boolean removeExtensions) throws IOException {
        if (getExtensionDirectoryPath().get() == null) {
            throw new NullPointerException("The extension directory path is null");
        }

        List<SavedCatalog> catalogsToRemove = savedCatalogs.stream()
                .filter(savedCatalog -> {
                    if (savedCatalog.deletable()) {
                        return true;
                    } else {
                        logger.warn("{} is not deletable and won't be deleted", savedCatalog.name());
                        return false;
                    }
                })
                .toList();
        if (catalogsToRemove.isEmpty()) {
            logger.debug("No catalog to remove");
            return;
        }

        this.savedCatalogs.removeAll(catalogsToRemove);

        try {
            extensionFolderManager.saveRegistry(new Registry(this.savedCatalogs));
        } catch (IOException | SecurityException | NullPointerException e) {
            this.savedCatalogs.addAll(catalogsToRemove);

            throw e;
        }

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

        logger.info("Catalogs {} removed", catalogsToRemove.stream().map(SavedCatalog::name).toList());
    }

    /**
     * Get the path to the directory containing the provided extension of the provided
     * catalog. This will also create the directory containing all installed extensions
     * of the provided catalog if it doesn't already exist (but the returned directory is
     * not guaranteed to be created).
     *
     * @param savedCatalog the catalog owning the extension
     * @param extension the extension to retrieve
     * @return the path to the folder containing the provided extension
     * @throws IOException if an I/O error occurs while creating the directory
     * @throws InvalidPathException if the path cannot be created
     * @throws SecurityException if the user doesn't have enough rights to create the directory
     * @throws NullPointerException if one of the parameters is null or if the path contained in
     * {@link #getExtensionDirectoryPath()} is null
     */
    public Path getExtensionDirectory(SavedCatalog savedCatalog, Extension extension) throws IOException {
        return extensionFolderManager.getExtensionDirectoryPath(savedCatalog, extension);
    }

    /**
     * Get the list of links the {@link #installOrUpdateExtension(SavedCatalog, Extension, InstalledExtension, Consumer, BiConsumer)} function
     * will download to install the provided extension.
     *
     * @param savedCatalog the catalog owning the extension to install
     * @param extension the extension to install
     * @param installationInformation what to install on the extension
     * @return the list URIs that will be downloaded to install the extension with the provided parameters
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionDirectoryPath()} is null
     * @throws IOException if an I/O error occurred while deleting, downloading or installing the extension
     * @throws InvalidPathException if a path cannot be created, for example because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to install or update the extension
     * @throws IllegalArgumentException if the release name of the provided installation information cannot be found in the releases
     * of the provided extension
     */
    public List<URI> getDownloadLinks(SavedCatalog savedCatalog, Extension extension, InstalledExtension installationInformation) throws IOException {
        return getDownloadUrlsToFilePaths(savedCatalog, extension, installationInformation, false).stream()
                .map(UriFileName::uri)
                .toList();
    }

    /**
     * Install (or update if it already exists) an extension. This may take a lot of time depending on the
     * internet connection and the size of the extension, but this operation is cancellable.
     * <p>
     * If the extension already exists, it will be deleted before downloading the provided version of the extension.
     * <p>
     * Warning: this will move to trash the directory returned by {@link #getExtensionDirectory(SavedCatalog, Extension)}
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
     * @throws NullPointerException if one of the parameters is null or if the path contained in {@link #getExtensionDirectoryPath()} is null
     * @throws IOException if an I/O error occurred while deleting, downloading or installing the extension
     * @throws InvalidPathException if a path cannot be created, for example because the extensions folder path contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to install or update the extension
     * @throws IllegalArgumentException if the release name of the provided installation information cannot be found in the releases
     * of the provided extension
     * @throws InterruptedException if the calling thread is interrupted
     */
    public void installOrUpdateExtension(
            SavedCatalog savedCatalog,
            Extension extension,
            InstalledExtension installationInformation,
            Consumer<Float> onProgress,
            BiConsumer<InstallationStep, String> onStatusChanged
    ) throws IOException, InterruptedException {
        var extensionProperty = installedExtensions.computeIfAbsent(
                new CatalogExtension(savedCatalog, extension),
                e -> new SimpleObjectProperty<>()
        );

        logger.debug("Deleting files of {} before installing or updating it", extension.name());
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

        synchronized (this) {
            extensionProperty.set(Optional.of(installationInformation));
        }

        logger.info("{} of {} installed", extension.name(), savedCatalog.name());
    }

    /**
     * Indicate whether an extension belonging to a catalog is installed.
     *
     * @param savedCatalog the catalog owning the extension to find
     * @param extension the extension to get installed information on
     * @return a read-only object property containing an Optional of an installed extension. If the Optional
     * is empty, then it means the extension is not installed. This property may be updated from any thread
     */
    public ReadOnlyObjectProperty<Optional<InstalledExtension>> getInstalledExtension(SavedCatalog savedCatalog, Extension extension) {
        return installedExtensions.computeIfAbsent(
                new CatalogExtension(savedCatalog, extension),
                catalogExtension -> new SimpleObjectProperty<>(getInstalledExtension(catalogExtension))
        );
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * added with catalogs to the extension directory. This list can be updated from
     * any thread. Note that this list can take a few seconds to update when a JAR is
     * added or removed
     */
    public ObservableList<Path> getCatalogManagedInstalledJars() {
        return extensionFolderManager.getCatalogManagedInstalledJars();
    }

    /**
     * Extension classes are automatically loaded with a custom class loader.
     * This function returns it.
     *
     * @return the class loader user to load extensions
     */
    public ClassLoader getExtensionClassLoader() {
        return extensionClassLoader;
    }

    /**
     * Set a runnable to be called each time a JAR file is loaded by {@link #getExtensionClassLoader()}. The call may
     * happen from any thread.
     * Note that the runnable may be called a few seconds after a JAR is added.
     *
     * @param runnable the runnable to run when a JAR file is loaded
     * @throws NullPointerException if the provided path is null
     */
    public void addOnJarLoadedRunnable(Runnable runnable) {
        extensionClassLoader.addOnJarLoadedRunnable(runnable);
    }

    /**
     * Get a list of updates available on the currently installed extensions (with catalogs, extensions
     * manually installed are not considered).
     *
     * @return a CompletableFuture with a list of available updates, or a failed CompletableFuture
     * if the update query failed
     */
    public CompletableFuture<List<UpdateAvailable>> getAvailableUpdates() {
        return CompletableFuture.supplyAsync(() -> savedCatalogs.stream()
                .map(savedCatalog -> CatalogFetcher.getCatalog(savedCatalog.rawUri()).join().extensions().stream()
                        .map(extension -> getUpdateAvailable(savedCatalog, extension))
                        .filter(Objects::nonNull)
                        .toList()
                )
                .flatMap(List::stream)
                .toList());
    }

    /**
     * Uninstall an extension by removing its files. This can take some time depending on the number
     * of files to delete and the speed of the disk.
     * <p>
     * Warning: this will move the directory returned by {@link #getExtensionDirectory(SavedCatalog, Extension)}
     * to trash or recursively delete it if moving files to trash is not supported by this platform.
     *
     * @param savedCatalog the catalog owning the extension to uninstall
     * @param extension the extension to uninstall
     * @throws IOException if an I/O error occurs while deleting the folder
     * @throws java.nio.file.InvalidPathException if the path of the extension folder cannot be created,
     * for example because the extension name contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to delete the extension files
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null,
     * or if one of the parameters is null
     */
    public void removeExtension(SavedCatalog savedCatalog, Extension extension) throws IOException {
        var extensionProperty = installedExtensions.computeIfAbsent(
                new CatalogExtension(savedCatalog, extension),
                e -> new SimpleObjectProperty<>()
        );

        extensionFolderManager.deleteExtension(savedCatalog, extension);
        synchronized (this) {
            extensionProperty.set(Optional.empty());
        }

        logger.info("{} of {} removed", extension.name(), savedCatalog.name());
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * manually added (i.e. not with a catalog) to the extension directory. This list
     * can be updated from any thread. Note that this list can take a few seconds to
     * update when a JAR is added or removed
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return extensionFolderManager.getManuallyInstalledJars();
    }

    private synchronized void setCatalogsFromRegistry() {
        this.savedCatalogs.clear();

        try {
            this.savedCatalogs.addAll(extensionFolderManager.getSavedRegistry().catalogs());
            logger.debug("Catalogs set from saved registry");
        } catch (Exception e) {
            logger.debug("Error while retrieving saved registry. Using default one", e);

            if (defaultRegistry != null) {
                this.savedCatalogs.addAll(defaultRegistry.catalogs());
                logger.debug(
                        "Catalogs {} set from default registry",
                        defaultRegistry.catalogs().stream().map(SavedCatalog::name).toList()
                );
            }
        }
    }

    private Optional<InstalledExtension> getInstalledExtension(CatalogExtension catalogExtension) {
        try {
            return extensionFolderManager.getInstalledExtension(
                    catalogExtension.savedCatalog,
                    catalogExtension.extension
            );
        } catch (IOException | InvalidPathException | SecurityException | NullPointerException e) {
            logger.debug("Error while retrieving {} installation information", catalogExtension.extension.name(), e);
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

        addJars(extensionFolderManager.getCatalogManagedInstalledJars());
        extensionFolderManager.getCatalogManagedInstalledJars().addListener((ListChangeListener<? super Path>) change -> {
            while (change.next()) {
                addJars(change.getAddedSubList());
                removeJars(change.getRemoved());
            }
            change.reset();
        });
    }

    private List<UriFileName> getDownloadUrlsToFilePaths(
            SavedCatalog savedCatalog,
            Extension extension,
            InstalledExtension installationInformation,
            boolean createFolders
    ) throws IOException {
        List<UriFileName> downloadUrlToFilePaths = new ArrayList<>();

        Optional<Release> release = extension.releases().stream()
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

    private UpdateAvailable getUpdateAvailable(SavedCatalog savedCatalog, Extension extension) {
        Optional<InstalledExtension> installedExtension = getInstalledExtension(
                new CatalogExtension(savedCatalog, extension)
        );

        if (installedExtension.isPresent()) {
            String installedRelease = installedExtension.get().releaseName();
            Optional<Release> maxCompatibleRelease = extension.getMaxCompatibleRelease(version);

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
