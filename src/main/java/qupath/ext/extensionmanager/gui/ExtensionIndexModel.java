package qupath.ext.extensionmanager.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.IndexMetadata;
import qupath.ext.extensionmanager.core.ExtensionInstallationInformation;
import qupath.ext.extensionmanager.core.indexmodel.Extension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The model of the UI elements of this project. This is basically a wrapper around {@link ExtensionIndexManager}
 * where listenable properties are propagated to the JavaFX Application Thread.
 */
public class ExtensionIndexModel {

    private final ObservableList<IndexMetadata> indexes = FXCollections.observableArrayList();
    private final ObservableList<IndexMetadata> indexesImmutable = FXCollections.unmodifiableObservableList(indexes);
    private static final Map<Extension, ObjectProperty<Optional<ExtensionInstallationInformation>>> installedExtensions = new HashMap<>();

    /**
     * Create the model.
     */
    public ExtensionIndexModel() {
        UiUtils.bindListInUIThread(indexes, ExtensionIndexManager.getIndexes());
    }

    /**
     * @return a read-only observable list of all saved indexes. This list will always be updated from the JavaFX
     * Application Thread
     */
    public ObservableList<IndexMetadata> getIndexes() {
        return indexesImmutable;
    }

    /**
     * Get installed information of an extension.
     *
     * @param extension the extension to get installed information on
     * @return a read-only object property containing an Optional of an installed extension.
     * If the Optional is empty, then it means the extension is not installed. This property
     * will always be updated from the JavaFX Application Thread
     */
    public ReadOnlyObjectProperty<Optional<ExtensionInstallationInformation>> getInstalledExtension(Extension extension) {
        return installedExtensions.computeIfAbsent(
                extension,
                e -> {
                    SimpleObjectProperty<Optional<ExtensionInstallationInformation>> installedExtension = new SimpleObjectProperty<>(Optional.empty());

                    UiUtils.bindPropertyInUIThread(installedExtension, ExtensionIndexManager.getInstalledExtension(e));

                    return installedExtension;
                }
        );
    }
}
