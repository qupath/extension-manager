package qupath.ext.extensionmanager.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.index.Extension;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The model of the UI elements of this project. This is basically a wrapper around {@link ExtensionIndexManager}
 * where listenable properties are propagated to the JavaFX Application Thread.
 */
public class ExtensionIndexModel {

    private final ObservableList<SavedIndex> savedIndices = FXCollections.observableArrayList();
    private final ObservableList<SavedIndex> indexesImmutable = FXCollections.unmodifiableObservableList(savedIndices);
    private static final Map<IndexExtension, ObjectProperty<Optional<InstalledExtension>>> installedExtensions = new HashMap<>();
    private final ObservableList<Path> manuallyInstalledJars = FXCollections.observableArrayList();
    private final ObservableList<Path> manuallyInstalledJarsImmutable = FXCollections.unmodifiableObservableList(manuallyInstalledJars);
    private final ExtensionIndexManager extensionIndexManager;
    private record IndexExtension(SavedIndex savedIndex, Extension extension) {}

    /**
     * Create the model.
     *
     * @param extensionIndexManager the extension index manager to listen to
     */
    public ExtensionIndexModel(ExtensionIndexManager extensionIndexManager) {
        this.extensionIndexManager = extensionIndexManager;

        UiUtils.bindListInUIThread(savedIndices, extensionIndexManager.getIndexes());
        UiUtils.bindListInUIThread(manuallyInstalledJars, extensionIndexManager.getManuallyInstalledJars());
    }

    /**
     * @return a read-only observable list of all saved indexes. This list will always be updated from the JavaFX
     * Application Thread
     */
    public ObservableList<SavedIndex> getIndexes() {
        return indexesImmutable;
    }

    /**
     * Get installed information of an extension.
     *
     * @param savedIndex the index owning the extension
     * @param extension the extension to get installed information on
     * @return a read-only object property containing an Optional of an installed extension.
     * If the Optional is empty, then it means the extension is not installed. This property
     * will always be updated from the JavaFX Application Thread
     */
    public ReadOnlyObjectProperty<Optional<InstalledExtension>> getInstalledExtension(SavedIndex savedIndex, Extension extension) {
        return installedExtensions.computeIfAbsent(
                new IndexExtension(savedIndex, extension),
                e -> {
                    SimpleObjectProperty<Optional<InstalledExtension>> installedExtension = new SimpleObjectProperty<>(Optional.empty());

                    UiUtils.bindPropertyInUIThread(installedExtension, extensionIndexManager.getInstalledExtension(e.savedIndex, e.extension));

                    return installedExtension;
                }
        );
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were manually added
     * (i.e. not with an index) to the extension directory. This list will always be updated from the
     * JavaFX Application Thread
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return manuallyInstalledJarsImmutable;
    }
}
