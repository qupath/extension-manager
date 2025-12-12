package qupath.ext.extensionmanager.gui;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.catalog.Catalog;

import java.nio.file.Path;

/**
 * The model of the UI elements of this project. This is basically a wrapper around {@link ExtensionCatalogManager} where
 * listenable properties are propagated to the JavaFX Application Thread.
 * <p>
 * An instance of this class must be {@link #close() closed} once no longer used.
 */
class ExtensionCatalogModel implements AutoCloseable {

    private final ObservableList<Catalog> catalogs = FXCollections.observableArrayList();
    private final ObservableList<Catalog> catalogsImmutable = FXCollections.unmodifiableObservableList(catalogs);
    private final ObservableList<Path> manuallyInstalledJars = FXCollections.observableArrayList();
    private final ObservableList<Path> manuallyInstalledJarsImmutable = FXCollections.unmodifiableObservableList(manuallyInstalledJars);
    private final ExtensionCatalogManager extensionCatalogManager;
    private final ListChangeListener<? super Catalog> catalogsListener;
    private final ListChangeListener<? super Path> manuallyInstalledJarsListener;

    /**
     * Create the model.
     *
     * @param extensionCatalogManager the extension catalog manager to listen to
     */
    public ExtensionCatalogModel(ExtensionCatalogManager extensionCatalogManager) {
        this.extensionCatalogManager = extensionCatalogManager;

        this.catalogsListener = UiUtils.bindListInUIThread(catalogs, extensionCatalogManager.getCatalogs());
        this.manuallyInstalledJarsListener = UiUtils.bindListInUIThread(manuallyInstalledJars, extensionCatalogManager.getManuallyInstalledJars());
    }

    @Override
    public void close() {
        extensionCatalogManager.getCatalogs().removeListener(catalogsListener);
        extensionCatalogManager.getManuallyInstalledJars().removeListener(manuallyInstalledJarsListener);
    }

    /**
     * @return a read-only observable list of all catalogs. This list will always be updated from the JavaFX Application
     * Thread
     */
    public ObservableList<Catalog> getCatalogs() {
        return catalogsImmutable;
    }

    /**
     * @return a read-only observable list of paths pointing to JAR files that were manually added (i.e. not with a catalog)
     * to the extension directory. This list will always be updated from the JavaFX Application Thread
     */
    public ObservableList<Path> getManuallyInstalledJars() {
        return manuallyInstalledJarsImmutable;
    }
}
