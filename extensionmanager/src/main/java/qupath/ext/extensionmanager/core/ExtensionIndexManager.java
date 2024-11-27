package qupath.ext.extensionmanager.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.filetools.FileDownloader;
import qupath.ext.extensionmanager.core.filetools.ZipExtractor;
import qupath.ext.extensionmanager.core.index.model.Extension;
import qupath.ext.extensionmanager.core.index.model.Release;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.core.savedentities.Registry;

import java.io.IOException;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A manager for indexes and extensions. It can be used to get access to all saved indexes,
 * add or remove an index, get access to all installed extensions, and install or delete an extension.
 * <p>
 * The list of active indexes and installed extensions is determined by this class. It is internally saved
 * in a registry JSON file located in the extension directory (see {@link Registry}).
 * <p>
 * This class is thread-safe.
 */
public class ExtensionIndexManager {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionIndexManager.class);
    private final ObservableList<SavedIndex> savedIndexes = FXCollections.observableArrayList();
    private final ObservableList<SavedIndex> savedIndexesImmutable = FXCollections.unmodifiableObservableList(savedIndexes);
    private final Map<IndexExtension, ObjectProperty<Optional<InstalledExtension>>> installedExtensions = new ConcurrentHashMap<>();
    private final ExtensionFolderManager extensionFolderManager;
    private final String quPathVersion;
    private record IndexExtension(SavedIndex savedIndex, Extension extension) {}

    /**
     * Create the extension index manager.
     *
     * @param extensionFolderPath a string property pointing to the path the extension folder should have
     * @param quPathVersion a text describing the QuPath release with the form "v[MAJOR].[MINOR].[PATCH]"
     *                      or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     * @param defaultRegistry the default registry to use when the saved one cannot be used
     * @throws IllegalArgumentException if the provided QuPath version doesn't meet the specified requirements
     */
    public ExtensionIndexManager(StringProperty extensionFolderPath, String quPathVersion, Registry defaultRegistry) {
        Version.isValid(quPathVersion);

        this.extensionFolderManager = new ExtensionFolderManager(extensionFolderPath);
        this.quPathVersion = quPathVersion;

        try {
            this.savedIndexes.addAll(extensionFolderManager.getSavedRegistry().getIndexes());
        } catch (Exception e) {
            logger.debug("Error while retrieving saved registry. Using default one.", e);
            this.savedIndexes.addAll(defaultRegistry.getIndexes());
        }
    }

    /**
     * @return the path to the extension folder. It may be updated from any thread
     */
    public StringProperty getExtensionFolderPath() {
        return extensionFolderManager.getExtensionFolderPath();
    }

    /**
     * @return a text describing the QuPath release with the form "v[MAJOR].[MINOR].[PATCH]"
     * or "v[MAJOR].[MINOR].[PATCH]-rc[RELEASE_CANDIDATE]"
     */
    public String getQuPathVersion() {
        return quPathVersion;
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
     *
     * @param savedIndexes the indexes to add
     * @throws IOException if an I/O error occurs while saving the registry file. In that case,
     * the provided indexes are not added
     */
    public void addIndex(List<SavedIndex> savedIndexes) throws IOException {
        synchronized (this) {
            for (SavedIndex savedIndex: savedIndexes) {
                if (this.savedIndexes.stream().anyMatch(index -> index.name().equals(savedIndex.name()))) {
                    logger.warn(String.format("Index %s has the same name as an existing index and will not be added", savedIndex));
                } else {
                    this.savedIndexes.add(savedIndex);
                }
            }
        }

        try {
            extensionFolderManager.saveRegistry(new Registry(this.savedIndexes));
        } catch (IOException e) {
            synchronized (this) {
                this.savedIndexes.removeAll(savedIndexes);
            }

            throw e;
        }
    }

    /**
     * Remove indexes from the available list. This will remove them from the
     * saved registry and delete any installed extension belonging to these indexes.
     *
     * @param savedIndexes the indexes to remove
     * @throws IOException if an I/O error occurs while saving the registry file. In that case,
     * the provided indexes are not removed
     */
    public void removeIndexes(List<SavedIndex> savedIndexes) throws IOException {
        synchronized (this) {
            this.savedIndexes.removeAll(savedIndexes);
        }

        try {
            extensionFolderManager.saveRegistry(new Registry(this.savedIndexes));
        } catch (IOException e) {
            synchronized (this) {
                this.savedIndexes.addAll(savedIndexes);
            }

            throw e;
        }

        for (SavedIndex savedIndex: savedIndexes) {
            try {
                extensionFolderManager.deleteIndex(savedIndex);
            } catch (IOException | SecurityException | InvalidPathException e) {
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
                indexExtension -> {
                    try {
                        return new SimpleObjectProperty<>(extensionFolderManager.getInstalledExtension(
                                indexExtension.savedIndex,
                                indexExtension.extension
                        ));
                    } catch (IOException | InvalidPathException | SecurityException e) {
                        logger.debug(
                                String.format("Error while retrieving extension %s installation information", extension),
                                e
                        );
                        return new SimpleObjectProperty<>(Optional.empty());
                    }
                }
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
     * @param onStatusChanged a function that will be called at different steps during the installation. Its parameter
     *                        will be a String describing the installation step currently happening
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
            Consumer<String> onStatusChanged,
            Consumer<Throwable> onComplete
    ) {
        var extensionProperty = installedExtensions.computeIfAbsent(
                new IndexExtension(savedIndex, extension),
                e -> new SimpleObjectProperty<>()
        );

        try {
            extensionFolderManager.deleteExtension(savedIndex, extension);
            synchronized (this) {
                extensionProperty.set(Optional.empty());
            }

            Map<URI, Path> downloadUrlToFileName = getDownloadUrlsToFileNames(savedIndex, extension, installationInformation);
            int i = 0;
            for (var entry: downloadUrlToFileName.entrySet()) {
                float progressOffset = (float) i / downloadUrlToFileName.size();
                boolean downloadingZipArchive = entry.getValue().toString().endsWith(".zip");

                float step = downloadingZipArchive ?
                        (float) 1 / (2 * downloadUrlToFileName.size()) :
                        (float) 1 / downloadUrlToFileName.size();

                onStatusChanged.accept(String.format("Downloading %s...", entry.getKey()));
                FileDownloader.downloadFile(
                        entry.getKey(),
                        entry.getValue(),
                        progress -> onProgress.accept(progressOffset + progress * step)
                );

                if (downloadingZipArchive) {
                    onStatusChanged.accept(String.format("Extracting %s...", entry.getValue()));
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

            onComplete.accept(null);
        } catch (IOException | InterruptedException | IllegalArgumentException | SecurityException e) {
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
    }

    private Map<URI, Path> getDownloadUrlsToFileNames(
            SavedIndex savedIndex,
            Extension extension,
            InstalledExtension installationInformation
    ) throws IOException {
        Map<URI, Path> downloadUrlToFileName = new HashMap<>();

        Optional<Release> release = extension.versions().stream()
                .filter(r -> r.name().equals(installationInformation.releaseName()))
                .findAny();

        if (release.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "The provided release name %s is not present in the extension releases %s",
                    installationInformation.releaseName(),
                    extension.versions()
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
                        getFileNameFromURI(release.get().mainUrl())
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
                            getFileNameFromURI(javadocUri)
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
                            getFileNameFromURI(requiredDependencyUri)
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
                                getFileNameFromURI(optionalDependencyUri)
                        )
                );
            }
        }

        return downloadUrlToFileName;
    }

    private static String getFileNameFromURI(URI uri) {
        return Paths.get(uri.getPath()).getFileName().toString();
    }
}
