package qupath.ext.extensionmanager.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.index.IndexFetcher;
import qupath.ext.extensionmanager.core.savedentities.UpdateAvailable;
import qupath.ext.extensionmanager.core.tools.FileDownloader;
import qupath.ext.extensionmanager.core.tools.FileTools;
import qupath.ext.extensionmanager.core.tools.ZipExtractor;
import qupath.ext.extensionmanager.core.index.Extension;
import qupath.ext.extensionmanager.core.index.Release;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.core.savedentities.Registry;

import java.io.IOError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A manager for indexes and extensions. It can be used to get access to all saved indexes,
 * add or remove an index, get access to all installed extensions, and install or delete an extension.
 * Manually installed extensions are automatically detected.
 * <p>
 * It also automatically loads extension classes with a custom ClassLoader (see {@link #getClassLoader()}).
 * Note that removed extensions are not unloaded from the class loader.
 * <p>
 * The list of active indexes and installed extensions is determined by this class. It is internally saved
 * in a registry JSON file located in the extension directory (see {@link Registry}).
 * <p>
 * This class is thread-safe.
 * <p>
 * This manager must be {@link #close() closed} once no longer used.
 */
public class ExtensionIndexManager implements AutoCloseable{

    private static final Logger logger = LoggerFactory.getLogger(ExtensionIndexManager.class);
    private final ObservableList<SavedIndex> savedIndexes = FXCollections.observableArrayList();
    private final ObservableList<SavedIndex> savedIndexesImmutable = FXCollections.unmodifiableObservableList(savedIndexes);
    private final Map<IndexExtension, ObjectProperty<Optional<InstalledExtension>>> installedExtensions = new ConcurrentHashMap<>();
    private final ExtensionFolderManager extensionFolderManager;
    private final ExtensionClassLoader extensionClassLoader;
    private final String version;
    private final Registry defaultRegistry;
    private record IndexExtension(SavedIndex savedIndex, Extension extension) {}
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
     * Create the extension index manager.
     *
     * @param extensionDirectoryPath a read-only property pointing to the path the extension directory should have. The
     *                               path can be null or invalid (but not the property). If this property is changed,
     *                               indexes and extensions will be set to the content of the new value of the property
     *                               (so will be reset if the new path is empty)
     * @param parentClassLoader the class loader that should be the parent of the extension class loader
     * @param version a text describing the release of the current software with the form "v[MAJOR].[MINOR].[PATCH]"
     *                or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]". It will determine which extensions are
     *                compatible
     * @param defaultRegistry the default registry to use when the saved one cannot be used. Can be null
     * @throws IllegalArgumentException if the provided version doesn't meet the specified requirements
     * @throws SecurityException if the user doesn't have enough rights to create the extension class loader
     */
    public ExtensionIndexManager(
            ReadOnlyObjectProperty<Path> extensionDirectoryPath,
            ClassLoader parentClassLoader,
            String version,
            Registry defaultRegistry
    ) {
        Version.isValid(version);

        this.extensionFolderManager = new ExtensionFolderManager(extensionDirectoryPath);
        this.extensionClassLoader = new ExtensionClassLoader(parentClassLoader);
        this.version = version;
        this.defaultRegistry = defaultRegistry;

        setIndexesFromRegistry();
        extensionDirectoryPath.addListener((p, o, n) -> {
            setIndexesFromRegistry();

            synchronized (this) {
                for (IndexExtension indexExtension: installedExtensions.keySet()) {
                    installedExtensions.replace(indexExtension, getInstalledExtension(indexExtension));
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
     * Extension classes are automatically loaded with a custom class loader.
     * This function returns it.
     *
     * @return the class loader user to load extensions
     */
    public ClassLoader getClassLoader() {
        return extensionClassLoader;
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
     * @return a read-only observable list of all saved indexes. This list may be updated from any thread
     */
    public ObservableList<SavedIndex> getIndexes() {
        return savedIndexesImmutable;
    }

    /**
     * Add indexes to the available list. This will save them to the registry.
     * Indexes with the same name as an already existing index will not be added.
     * No check will be performed concerning whether the provided indexes point to
     * valid indexes.
     * <p>
     * If an exception occurs (see below), the provided indexes are not added.
     *
     * @param savedIndexes the indexes to add
     * @throws IOException if an I/O error occurs while saving the registry file. In that case,
     * the provided indexes are not added
     * @throws SecurityException if the user doesn't have sufficient rights to save the registry file
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     */
    public void addIndex(List<SavedIndex> savedIndexes) throws IOException {
        List<SavedIndex> indexesToAdd;
        synchronized (this) {
            indexesToAdd = savedIndexes.stream()
                    .filter(savedIndex -> {
                        if (this.savedIndexes.stream().noneMatch(index -> index.name().equals(savedIndex.name()))) {
                            return true;
                        } else {
                            logger.warn("Index {} has the same name as an existing index and will not be added", savedIndex);
                            return false;
                        }
                    })
                    .toList();

            if (indexesToAdd.isEmpty()) {
                logger.debug("No index to add");
                return;
            }

            this.savedIndexes.addAll(indexesToAdd);
        }

        try {
            extensionFolderManager.saveRegistry(new Registry(this.savedIndexes));
        } catch (IOException | SecurityException | NullPointerException e) {
            synchronized (this) {
                this.savedIndexes.removeAll(indexesToAdd);
            }

            throw e;
        }

        logger.info("Indexes {} added", indexesToAdd);
    }

    /**
     * Remove indexes from the available list. This will remove them from the
     * saved registry and delete any installed extension belonging to these indexes.
     * <p>
     * If an exception occurs (see below), the provided indexes are not added.
     *
     * @param savedIndexes the indexes to remove
     * @throws IOException if an I/O error occurs while saving the registry file. In that case,
     * the provided indexes are not added
     * @throws SecurityException if the user doesn't have sufficient rights to save the registry file
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     */
    public void removeIndexes(List<SavedIndex> savedIndexes) throws IOException {
        synchronized (this) {
            this.savedIndexes.removeAll(savedIndexes);
        }

        try {
            extensionFolderManager.saveRegistry(new Registry(this.savedIndexes));
        } catch (IOException | SecurityException | NullPointerException e) {
            synchronized (this) {
                this.savedIndexes.addAll(savedIndexes);
            }

            throw e;
        }

        for (SavedIndex savedIndex: savedIndexes) {
            try {
                extensionFolderManager.deleteExtensionsFromIndex(savedIndex);
            } catch (IOException | SecurityException | InvalidPathException | NullPointerException e) {
                logger.debug(String.format("Could not delete index %s", savedIndex), e);
            }
        }

        for (var entry: installedExtensions.entrySet()) {
            if (savedIndexes.contains(entry.getKey().savedIndex)) {
                synchronized (this) {
                    entry.getValue().set(Optional.empty());
                }
            }
        }

        logger.info("Indexes {} removed", savedIndexes);
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * manually added (i.e. not with an index) to the extension directory. This list
     * can be updated from any thread
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return extensionFolderManager.getManuallyInstalledJars();
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were
     * added with indexes to the extension directory. This list can be updated from
     * any thread
     */
    public ObservableList<Path> getIndexedManagedInstalledJars() {
        return extensionFolderManager.getIndexedManagedInstalledJars();
    }


    /**
     * Get a list of updates available on the currently installed extensions (with indexes, extensions
     * manually installed are not considered).
     *
     * @return a CompletableFuture with a list of available updates, or a failed CompletableFuture
     * if the update query failed
     */
    public CompletableFuture<List<UpdateAvailable>> getAvailableUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            List<SavedIndex> savedIndexes;
            synchronized (this) {
                // Prevent modifications to savedIndexes while iterating
                savedIndexes = new ArrayList<>(this.savedIndexes);
            }

            return savedIndexes.stream()
                    .map(savedIndex -> IndexFetcher.getIndex(savedIndex.rawUri()).join().extensions().stream()
                            .map(extension -> getUpdateAvailable(savedIndex, extension))
                            .filter(Objects::nonNull)
                            .toList()
                    )
                    .flatMap(List::stream)
                    .toList();
        });
    }

    /**
     * Indicate whether an extension belonging to an index is installed.
     *
     * @param savedIndex the index owning the extension to find
     * @param extension the extension to get installed information on
     * @return a read-only object property containing an Optional of an installed extension. If the Optional
     * is empty, then it means the extension is not installed. This property may be updated from any thread
     */
    public ReadOnlyObjectProperty<Optional<InstalledExtension>> getInstalledExtension(SavedIndex savedIndex, Extension extension) {
        return installedExtensions.computeIfAbsent(
                new IndexExtension(savedIndex, extension),
                this::getInstalledExtension
        );
    }

    /**
     * Install (or update if it already exists) an extension. This may take a lot of time depending on the
     * internet connection and the size of the extension.
     * <p>
     * If the extension already exists, it will be deleted before downloading the provided version of the extension.
     *
     * @param savedIndex the index owning the extension to install/update
     * @param extension the extension to install/update
     * @param installationInformation what to install/update on the extension
     * @param onProgress a function that will be called at different steps during the installation. Its parameter
     *                   will be a float between 0 and 1 indicating the progress of the installation (0: beginning,
     *                   1: finished). This function will be called from the calling thread
     * @param onStatusChanged a function that will be called at different steps during the installation. Its first parameter
     *                        will be the step currently happening, and its second parameter a text describing the resource
     *                        on which the step is happening (for example, a link if the step is a download). This function
     *                        will be called from the calling thread
     * @param onComplete a function that will be called when the installation is complete. Its parameter is a Throwable
     *                   indicating the error if the operation failed and null if the operation succeeded. This function
     *                   is guaranteed to be called at the end of the operation. This function will be called from the
     *                   calling thread
     */
    public void installOrUpdateExtension(
            SavedIndex savedIndex,
            Extension extension,
            InstalledExtension installationInformation,
            Consumer<Float> onProgress,
            BiConsumer<InstallationStep, String> onStatusChanged,
            Consumer<Throwable> onComplete
    ) {
        var extensionProperty = installedExtensions.computeIfAbsent(
                new IndexExtension(savedIndex, extension),
                e -> new SimpleObjectProperty<>()
        );

        try {
            logger.debug("Deleting files of {} before installing or updating it", extension);
            extensionFolderManager.deleteExtension(savedIndex, extension);
            synchronized (this) {
                extensionProperty.set(Optional.empty());
            }

            Map<URI, Path> downloadUrlToFileName = getDownloadUrlsToFileNames(savedIndex, extension, installationInformation);

            int i = 0;
            for (var entry: downloadUrlToFileName.entrySet()) {
                float progressOffset = (float) i / downloadUrlToFileName.size();
                boolean downloadingZipArchive = entry.getValue().toString().endsWith(".zip");

                if (downloadingZipArchive) {
                    logger.debug("Downloading and extracting {} to {}", entry.getKey(), entry.getValue());
                } else {
                    logger.debug("Downloading {} to {}", entry.getKey(), entry.getValue());
                }

                float step = downloadingZipArchive ?
                        (float) 1 / (2 * downloadUrlToFileName.size()) :
                        (float) 1 / downloadUrlToFileName.size();

                onStatusChanged.accept(InstallationStep.DOWNLOADING, entry.getKey().toString());
                FileDownloader.downloadFile(
                        entry.getKey(),
                        entry.getValue(),
                        progress -> onProgress.accept(progressOffset + progress * step)
                );

                if (downloadingZipArchive) {
                    onStatusChanged.accept(InstallationStep.EXTRACTING_ZIP, entry.getValue().toString());
                    ZipExtractor.extractZipToFolder(
                            entry.getValue(),
                            entry.getValue().getParent(),
                            progress -> onProgress.accept(progressOffset + step + progress * step)
                    );
                }

                i++;
            }

            synchronized (this) {
                extensionProperty.set(Optional.of(installationInformation));
            }

            logger.info("Extension {} of {} installed", extension, savedIndex);
            onComplete.accept(null);
        } catch (IOException | InterruptedException | IllegalArgumentException | SecurityException | NullPointerException e) {
            synchronized (this) {
                extensionProperty.set(Optional.empty());
            }

            onComplete.accept(e);

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Uninstall an extension by removing its files. This can take some time depending on the number
     * of files to delete and the speed of the disk.
     *
     * @param savedIndex the index owning the extension to uninstall
     * @param extension the extension to uninstall
     * @throws IOException if an I/O error occurs while deleting the folder
     * @throws java.nio.file.InvalidPathException if the path of the extension folder cannot be created,
     * for example because the extension name contain invalid characters
     * @throws SecurityException if the user doesn't have sufficient rights to delete the extension files
     * @throws NullPointerException if the path contained in {@link #getExtensionDirectoryPath()} is null
     */
    public void removeExtension(SavedIndex savedIndex, Extension extension) throws IOException {
        var extensionProperty = installedExtensions.computeIfAbsent(
                new IndexExtension(savedIndex, extension),
                e -> new SimpleObjectProperty<>()
        );

        extensionFolderManager.deleteExtension(savedIndex, extension);
        synchronized (this) {
            extensionProperty.set(Optional.empty());
        }

        logger.info("Extension {} of {} removed", extension, savedIndex);
    }

    private synchronized void setIndexesFromRegistry() {
        this.savedIndexes.clear();

        try {
            this.savedIndexes.addAll(extensionFolderManager.getSavedRegistry().indexes());
            logger.debug("Indexes set from saved registry");
        } catch (Exception e) {
            logger.debug("Error while retrieving saved registry. Using default one {} if not null", defaultRegistry, e);

            if (defaultRegistry != null) {
                this.savedIndexes.addAll(defaultRegistry.indexes());
                logger.debug("Indexes set from default registry");
            }
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

        addJars(extensionFolderManager.getIndexedManagedInstalledJars());
        extensionFolderManager.getIndexedManagedInstalledJars().addListener((ListChangeListener<? super Path>) change -> {
            while (change.next()) {
                addJars(change.getAddedSubList());
            }
            change.reset();
        });
    }

    private ObjectProperty<Optional<InstalledExtension>> getInstalledExtension(IndexExtension indexExtension) {
        try {
            return new SimpleObjectProperty<>(extensionFolderManager.getInstalledExtension(
                    indexExtension.savedIndex,
                    indexExtension.extension
            ));
        } catch (IOException | InvalidPathException | SecurityException | NullPointerException e) {
            logger.debug(
                    String.format("Error while retrieving extension %s installation information", indexExtension.extension),
                    e
            );
            return new SimpleObjectProperty<>(Optional.empty());
        }
    }

    private UpdateAvailable getUpdateAvailable(SavedIndex savedIndex, Extension extension) {
        Optional<InstalledExtension> installedExtension = getInstalledExtension(
                new IndexExtension(savedIndex, extension)
        ).get();

        if (installedExtension.isPresent()) {
            String installedRelease = installedExtension.get().releaseName();
            Optional<Release> maxCompatibleRelease = extension.getMaxCompatibleRelease(version);

            if (maxCompatibleRelease.isPresent() &&
                    new Version(maxCompatibleRelease.get().name()).compareTo(new Version(installedRelease)) > 0
            ) {
                logger.debug(
                        "Extension {} installed and updatable to {}",
                        extension,
                        maxCompatibleRelease.get().name()
                );
                return new UpdateAvailable(
                        extension.name(),
                        installedExtension.get().releaseName(),
                        maxCompatibleRelease.get().name()
                );
            } else {
                logger.debug(
                        "Extension {} installed but no compatible update found",
                        extension
                );
                return null;
            }
        } else {
            logger.debug("Extension {} not installed, so no update available", extension);
            return null;
        }
    }

    private Map<URI, Path> getDownloadUrlsToFileNames(
            SavedIndex savedIndex,
            Extension extension,
            InstalledExtension installationInformation
    ) throws IOException {
        Map<URI, Path> downloadUrlToFileName = new HashMap<>();

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

        downloadUrlToFileName.put(
                release.get().mainUrl(),
                Paths.get(
                        extensionFolderManager.createAndGetExtensionPath(
                                savedIndex,
                                extension,
                                release.get().name(),
                                ExtensionFolderManager.FileType.MAIN_JAR
                        ).toString(),
                        FileTools.getFileNameFromURI(release.get().mainUrl())
                )
        );

        for (URI javadocUri: release.get().javadocsUrls()) {
            downloadUrlToFileName.put(
                    javadocUri,
                    Paths.get(
                            extensionFolderManager.createAndGetExtensionPath(
                                    savedIndex,
                                    extension,
                                    release.get().name(),
                                    ExtensionFolderManager.FileType.JAVADOCS
                            ).toString(),
                            FileTools.getFileNameFromURI(javadocUri)
                    )
            );
        }

        for (URI requiredDependencyUri: release.get().requiredDependencyUrls()) {
            downloadUrlToFileName.put(
                    requiredDependencyUri,
                    Paths.get(
                            extensionFolderManager.createAndGetExtensionPath(
                                    savedIndex,
                                    extension,
                                    release.get().name(),
                                    ExtensionFolderManager.FileType.REQUIRED_DEPENDENCIES
                            ).toString(),
                            FileTools.getFileNameFromURI(requiredDependencyUri)
                    )
            );
        }

        if (installationInformation.optionalDependenciesInstalled()) {
            for (URI optionalDependencyUri: release.get().optionalDependencyUrls()) {
                downloadUrlToFileName.put(
                        optionalDependencyUri,
                        Paths.get(
                                extensionFolderManager.createAndGetExtensionPath(
                                        savedIndex,
                                        extension,
                                        release.get().name(),
                                        ExtensionFolderManager.FileType.OPTIONAL_DEPENDENCIES
                                ).toString(),
                                FileTools.getFileNameFromURI(optionalDependencyUri)
                        )
                );
            }
        }

        return downloadUrlToFileName;
    }

    private void addJars(List<? extends Path> jarPaths) {
        for (Path path: jarPaths) {
            try {
                extensionClassLoader.addJar(path);
            } catch (IOError | SecurityException | MalformedURLException e) {
                logger.error(String.format("Cannot load extension %s", path), e);
            }
        }
    }
}
