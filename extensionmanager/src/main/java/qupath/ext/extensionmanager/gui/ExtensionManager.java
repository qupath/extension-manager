package qupath.ext.extensionmanager.gui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionCatalogManager;
import qupath.ext.extensionmanager.core.savedentities.SavedCatalog;
import qupath.ext.extensionmanager.gui.catalog.CatalogPane;
import qupath.fx.dialogs.Dialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A window that displays information and controls regarding catalogs and their extensions.
 */
public class ExtensionManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionManager.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private final ExtensionCatalogManager extensionCatalogManager;
    private final ExtensionCatalogModel model;
    private final Runnable onInvalidExtensionDirectory;
    private CatalogManager catalogManager;
    @FXML
    private VBox catalogs;
    @FXML
    private TitledPane manuallyInstalledExtensionsPane;
    @FXML
    private VBox manuallyInstalledExtensions;
    @FXML
    private Label noCatalogOrExtension;

    /**
     * Create the window.
     *
     * @param extensionCatalogManager the extension catalog manager this window should use
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionCatalogManager#getExtensionDirectoryPath()})
     *                                    but this directory is currently invalid. It lets the possibility to the user to
     *                                    define and create a valid directory before performing the operation (which would
     *                                    fail if the directory is invalid). This function is guaranteed to be called from
     *                                    the JavaFX Application Thread
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionManager(
            ExtensionCatalogManager extensionCatalogManager,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionCatalogManager = extensionCatalogManager;
        this.model = new ExtensionCatalogModel(extensionCatalogManager);
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;

        UiUtils.loadFXML(this, ExtensionManager.class.getResource("extension_manager.fxml"));

        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        setCatalogs();
        model.getCatalogs().addListener((ListChangeListener<? super SavedCatalog>) change ->
                setCatalogs()
        );

        manuallyInstalledExtensionsPane.visibleProperty().bind(Bindings.isNotEmpty(manuallyInstalledExtensions.getChildren()));
        manuallyInstalledExtensionsPane.managedProperty().bind(manuallyInstalledExtensionsPane.visibleProperty());

        setManuallyInstalledExtensions();
        model.getManuallyInstalledJars().addListener((ListChangeListener<? super Path>) change ->
                setManuallyInstalledExtensions()
        );

        noCatalogOrExtension.visibleProperty().bind(
                manuallyInstalledExtensionsPane.visibleProperty().not().and(
                Bindings.isEmpty(catalogs.getChildren()))
        );
        noCatalogOrExtension.managedProperty().bind(noCatalogOrExtension.visibleProperty());
    }

    /**
     * Copy the provided files to the provided extension directory. Some error dialogs will be shown to
     * the user if some errors occurs.
     * If at least a destination file already exists, the user is prompted for confirmation.
     * No confirmation dialog is prompted on success.
     *
     * @param filesToCopy the files to copy. No check is performed on those files
     * @param extensionDirectoryPath the path to the extension directory
     * @param onInvalidExtensionDirectory a function that will be called if the provided extension
     *                                    directory is invalid. It lets the possibility to the user to
     *                                    define and create a valid directory before copying the files
     */
    public static void promptToCopyFilesToExtensionDirectory(
            List<File> filesToCopy,
            ReadOnlyObjectProperty<Path> extensionDirectoryPath,
            Runnable onInvalidExtensionDirectory
    ) {
        UiUtils.promptExtensionDirectory(extensionDirectoryPath, onInvalidExtensionDirectory);

        Path extensionFolder = extensionDirectoryPath.get();
        if (extensionFolder == null) {
            Dialogs.showErrorMessage(
                    resources.getString("ExtensionManager.error"),
                    resources.getString("ExtensionManager.cannotCopyFiles")
            );
            return;
        }

        Map<File, File> sourceToDestinationFiles = new HashMap<>();
        for (File file : filesToCopy) {
            try {
                sourceToDestinationFiles.put(
                        file,
                        extensionFolder.resolve(file.toPath().getFileName()).toFile()
                );
            } catch (InvalidPathException | NullPointerException e) {
                logger.debug(String.format(
                        "Error while resolving path of %s in extension folder %s ",
                        file,
                        extensionFolder
                ), e);
            }
        }

        List<File> destinationFilesAlreadyExisting = sourceToDestinationFiles.values().stream()
                .filter(file -> {
                    try {
                        return file.exists();
                    } catch (SecurityException e) {
                        logger.debug("Cannot check if {} exists", file, e);
                        return false;
                    }
                })
                .toList();
        if (!destinationFilesAlreadyExisting.isEmpty()) {
            var confirmation = Dialogs.showConfirmDialog(
                    resources.getString("ExtensionManager.copyFiles"),
                    MessageFormat.format(
                            resources.getString("ExtensionManager.alreadyExist"),
                            destinationFilesAlreadyExisting.size() == 1 ?
                                    destinationFilesAlreadyExisting.getFirst() :
                                    destinationFilesAlreadyExisting
                    )
            );

            if (!confirmation) {
                return;
            }
        }

        List<Throwable> errors = new ArrayList<>();
        for (var entry : sourceToDestinationFiles.entrySet()) {
            try {
                Files.copy(entry.getKey().toPath(), entry.getValue().toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | InvalidPathException | SecurityException e) {
                logger.error("Cannot copy {} to {}", entry.getKey(), entry.getValue(), e);
                errors.add(e);
            }
        }

        if (!errors.isEmpty()) {
            Dialogs.showErrorMessage(
                    resources.getString("ExtensionManager.copyError"),
                    MessageFormat.format(
                            resources.getString("ExtensionManager.errorWhileCopying"),
                            errors.stream()
                                    .map(Throwable::getLocalizedMessage)
                                    .collect(Collectors.joining("\n"))
                    )
            );
        }
    }

    @FXML
    private void onOpenExtensionDirectory(ActionEvent ignored) {
        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        Path extensionDirectory = extensionCatalogManager.getExtensionDirectoryPath().get();
        if (extensionDirectory == null) {
            Dialogs.showErrorMessage(
                    resources.getString("ExtensionManager.error"),
                    resources.getString("ExtensionManager.cannotOpenExtensionDirectory")
            );
            return;
        }

        UiUtils.openFolderInFileExplorer(extensionDirectory).exceptionally(error -> {
            logger.error("Error while opening QuPath extension directory {}", extensionDirectory, error);

            Dialogs.showErrorMessage(
                    resources.getString("ExtensionManager.error"),
                    MessageFormat.format(
                            resources.getString("ExtensionManager.cannotOpen"),
                            extensionDirectory,
                            error.getLocalizedMessage()
                    )
            );

            return null;
        });
    }

    @FXML
    private void onManageCatalogsClicked(ActionEvent ignored) {
        UiUtils.promptExtensionDirectory(extensionCatalogManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        if (catalogManager == null) {
            try {
                catalogManager = new CatalogManager(extensionCatalogManager, model, onInvalidExtensionDirectory);
                catalogManager.initOwner(this);
                catalogManager.show();
            } catch (IOException e) {
                logger.error("Error while creating catalog manager window", e);
            }
        }

        if (catalogManager != null) {
            catalogManager.show();
            catalogManager.requestFocus();
        }
    }

    private void setCatalogs() {
        catalogs.getChildren().setAll(model.getCatalogs().stream()
                .map(catalog -> {
                    try {
                        return new CatalogPane(extensionCatalogManager, catalog, model);
                    } catch (IOException e) {
                        logger.error("Error while creating catalog pane", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList()
        );
    }

    private void setManuallyInstalledExtensions() {
        manuallyInstalledExtensions.getChildren().setAll(IntStream.range(0, model.getManuallyInstalledJars().size())
                .mapToObj(i -> {
                    try {
                        ManuallyInstalledExtensionLine manuallyInstalledExtensionLine = new ManuallyInstalledExtensionLine(
                                model.getManuallyInstalledJars().get(i)
                        );

                        if (i % 2 == 0) {
                            manuallyInstalledExtensionLine.getStyleClass().add(UiUtils.getClassName(UiUtils.CssClass.ODD_ROW));
                        }

                        return manuallyInstalledExtensionLine;
                    } catch (IOException e) {
                        logger.error("Error while manually installed extensionLine", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList()
        );
    }
}
