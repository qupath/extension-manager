package qupath.ext.extensionmanager.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.model.ExtensionModel;
import qupath.ext.extensionmanager.core.savedentities.InstalledExtension;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The model of the UI elements of this project. This is basically a wrapper around {@link ExtensionCatalogManager}
 * where listenable properties are propagated to the JavaFX Application Thread.
 */
public class ExtensionCatalogModel {

    private final ObservableList<SavedCatalog> catalogs = FXCollections.observableArrayList();
    private final ObservableList<SavedCatalog> catalogsImmutable = FXCollections.unmodifiableObservableList(catalogs);
    private static final Map<CatalogExtension, ObjectProperty<Optional<InstalledExtension>>> installedExtensions = new HashMap<>();
    private final ObservableList<Path> manuallyInstalledJars = FXCollections.observableArrayList();
    private final ObservableList<Path> manuallyInstalledJarsImmutable = FXCollections.unmodifiableObservableList(manuallyInstalledJars);
    private final ExtensionCatalogManager extensionCatalogManager;
    private record CatalogExtension(SavedCatalog savedCatalog, ExtensionModel extension) {}

    /**
     * Create the model.
     *
     * @param extensionCatalogManager the extension catalog manager to listen to
     */
    public ExtensionCatalogModel(ExtensionCatalogManager extensionCatalogManager) {
        this.extensionCatalogManager = extensionCatalogManager;

        UiUtils.bindListInUIThread(catalogs, extensionCatalogManager.getCatalogs());
        UiUtils.bindListInUIThread(manuallyInstalledJars, extensionCatalogManager.getManuallyInstalledJars());
    }

    /**
     * @return a read-only observable list of all saved catalogs. This list will always be updated from the JavaFX
     * Application Thread
     */
    public ObservableList<SavedCatalog> getCatalogs() {
        return catalogsImmutable;
    }

    /**
     * Get installed information of an extension.
     *
     * @param savedCatalog the catalog owning the extension
     * @param extension the extension to get installed information on
     * @return a read-only object property containing an Optional of an installed extension.
     * If the Optional is empty, then it means the extension is not installed. This property
     * will always be updated from the JavaFX Application Thread
     */
    public ReadOnlyObjectProperty<Optional<InstalledExtension>> getInstalledExtension(SavedCatalog savedCatalog, ExtensionModel extension) {
        return installedExtensions.computeIfAbsent(
                new CatalogExtension(savedCatalog, extension),
                e -> {
                    SimpleObjectProperty<Optional<InstalledExtension>> installedExtension = new SimpleObjectProperty<>(Optional.empty());

                    UiUtils.bindPropertyInUIThread(installedExtension, extensionCatalogManager.getInstalledExtension(e.savedCatalog, e.extension));

                    return installedExtension;
                }
        );
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were manually added
     * (i.e. not with a catalog) to the extension directory. This list will always be updated from the
     * JavaFX Application Thread
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return manuallyInstalledJarsImmutable;
    }
}
