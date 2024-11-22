package qupath.ext.extensionmanager.core;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.ext.extensionmanager.core.indexmodel.Extension;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A manager for indexes and extensions. It can be used to get access to all saved indexes,
 * add or remove an index, get access to all installed extensions, and install or delete an extension.
 * <p>
 * This class is thread-safe.
 */
public class ExtensionIndexManager {

    private static final ObservableList<IndexMetadata> indexes = FXCollections.observableArrayList(
            //TODO: get from storage
            List.of(
                    new IndexMetadata(
                            "QuPath index",
                            "Extensions maintained by the QuPath team",
                            URI.create("https://raw.githubusercontent.com/Rylern/test-index/refs/heads/main/index.json")
                    )
            )
    );
    private static final ObservableList<IndexMetadata> indexesImmutable = FXCollections.unmodifiableObservableList(indexes);
    private static final Map<Extension, ObjectProperty<Optional<ExtensionInstallationInformation>>> installedExtensions = new ConcurrentHashMap<>();

    /**
     * @return a read-only observable list of all saved indexes. This list may be updated from any thread
     */
    public static ObservableList<IndexMetadata> getIndexes() {
        return indexesImmutable;
    }

    /**
     * Save an index.
     *
     * @param index the index to save
     */
    public static synchronized void addIndex(IndexMetadata index) {
        //TODO: save indexes

        indexes.add(index);
    }

    /**
     * Remove an index.
     *
     * @param indexes the index to remove
     */
    public static synchronized void removeIndexes(List<IndexMetadata> indexes) {
        //TODO: remove indexes from storage

        ExtensionIndexManager.indexes.removeAll(indexes);
    }

    /**
     * Get installed information of an extension.
     *
     * @param extension the extension to get installed information on
     * @return a read-only object property containing an Optional of an installed extension. If the Optional
     * is empty, then it means the extension is not installed. This property may be updated from any thread
     */
    public static ReadOnlyObjectProperty<Optional<ExtensionInstallationInformation>> getInstalledExtension(Extension extension) {
        return installedExtensions.computeIfAbsent(
                extension,
                e -> new SimpleObjectProperty<>(Optional.empty()) //TODO: check if extension installed
        );
    }

    /**
     * Install (or update if it already exists) an extension. This may take a lot of time depending on the
     * internet connection and the size of the extension.
     *
     * @param extension the extension to install/update
     * @param extensionToInstall what to install/update on the extension
     * @param onProgress a function that will be called at different steps during the installation. Its parameter
     *                   will be a float between 0 and 1 indicating the progress of the installation (0: beginning,
     *                   1: finished). This function may be called from any thread
     * @param onComplete a function that will be called when the installation is complete. It is guaranteed that this function
     *                   will be called (even if an error occurs). This function may be called from any thread
     */
    public static void installOrUpdateExtension(Extension extension, ExtensionInstallationInformation extensionToInstall, Consumer<Float> onProgress, Runnable onComplete) {
        //TODO: uninstall current version if already installed and current version != demanded version
        //TODO: install extension if current version != demanded version
        //TODO: install or remove optional dependencies (with same method as above)

        var extensionProperty = installedExtensions.computeIfAbsent(extension, e -> new SimpleObjectProperty<>());
        synchronized (ExtensionIndexManager.class) {
            extensionProperty.set(Optional.of(extensionToInstall));
        }

        onComplete.run();
    }

    /**
     * Uninstall an extension.
     *
     * @param extension the extension to uninstall
     */
    public static synchronized void removeExtension(Extension extension) {
        //TODO: uninstall extension

        installedExtensions.get(extension).set(Optional.empty());
    }
}
