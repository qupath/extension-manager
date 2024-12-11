package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.index.IndexPane;

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
 * A window that displays information and controls regarding indexes and their extensions.
 */
public class ExtensionManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionManager.class);
    private static final ResourceBundle resources = UiUtils.getResources();
    private final ExtensionIndexManager extensionIndexManager;
    private final ExtensionIndexModel model;
    private final Runnable onInvalidExtensionDirectory;
    private IndexManager indexManager;
    @FXML
    private VBox indexes;
    @FXML
    private TitledPane manuallyInstalledExtensionsPane;
    @FXML
    private VBox manuallyInstalledExtensions;
    @FXML
    private Label noIndexOrExtension;

    /**
     * Create the window.
     *
     * @param extensionIndexManager the extension index manager this window should use
     * @param onInvalidExtensionDirectory a function that will be called if an operation needs to access the extension
     *                                    directory (see {@link ExtensionIndexManager#getExtensionDirectoryPath()})
     *                                    but this directory is currently invalid. It lets the possibility to the user to
     *                                    define and create a valid directory before performing the operation (which would
     *                                    fail if the directory is invalid). This function is guaranteed to be called from
     *                                    the JavaFX Application Thread
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionManager(
            ExtensionIndexManager extensionIndexManager,
            Runnable onInvalidExtensionDirectory
    ) throws IOException {
        this.extensionIndexManager = extensionIndexManager;
        this.model = new ExtensionIndexModel(extensionIndexManager);
        this.onInvalidExtensionDirectory = onInvalidExtensionDirectory;

        UiUtils.loadFXML(this, ExtensionManager.class.getResource("extension_manager.fxml"));

        UiUtils.promptExtensionDirectory(extensionIndexManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        setIndexes();
        model.getIndexes().addListener((ListChangeListener<? super SavedIndex>) change ->
                setIndexes()
        );

        manuallyInstalledExtensionsPane.visibleProperty().bind(Bindings.isNotEmpty(manuallyInstalledExtensions.getChildren()));
        manuallyInstalledExtensionsPane.managedProperty().bind(manuallyInstalledExtensionsPane.visibleProperty());

        setManuallyInstalledExtensions();
        model.getManuallyInstalledJars().addListener((ListChangeListener<? super Path>) change ->
                setManuallyInstalledExtensions()
        );

        noIndexOrExtension.visibleProperty().bind(
                manuallyInstalledExtensionsPane.visibleProperty().not().and(
                Bindings.isEmpty(indexes.getChildren()))
        );
        noIndexOrExtension.managedProperty().bind(noIndexOrExtension.visibleProperty());
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
            new Alert(
                    Alert.AlertType.ERROR,
                    resources.getString("ExtensionManager.cannotCopyFiles")
            ).show();
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
            var confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    MessageFormat.format(
                            resources.getString("ExtensionManager.alreadyExist"),
                            destinationFilesAlreadyExisting
                    )
            ).showAndWait();

            if (confirmation.isEmpty() || !confirmation.get().equals(ButtonType.OK)) {
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
            new Alert(
                    Alert.AlertType.ERROR,
                    MessageFormat.format(
                            resources.getString("ExtensionManager.errorWhileCopying"),
                            errors.stream()
                                    .map(Throwable::getLocalizedMessage)
                                    .collect(Collectors.joining("\n"))
                    )
            ).show();
        }
    }

    @FXML
    private void onOpenExtensionDirectory(ActionEvent ignored) {
        UiUtils.promptExtensionDirectory(extensionIndexManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        Path extensionDirectory = extensionIndexManager.getExtensionDirectoryPath().get();
        if (extensionDirectory == null) {
            new Alert(
                    Alert.AlertType.ERROR,
                    resources.getString("ExtensionManager.cannotOpenExtensionDirectory")
            ).show();
            return;
        }

        UiUtils.openFolderInFileExplorer(extensionDirectory).exceptionally(error -> {
            logger.error("Error while opening QuPath extension directory {}", extensionDirectory, error);

            Platform.runLater(() -> new Alert(
                    Alert.AlertType.ERROR,
                    MessageFormat.format(
                            resources.getString("ExtensionManager.cannotOpen"),
                            extensionDirectory,
                            error.getLocalizedMessage()
                    )
            ).show());

            return null;
        });
    }

    @FXML
    private void onManageIndexesClicked(ActionEvent ignored) {
        UiUtils.promptExtensionDirectory(extensionIndexManager.getExtensionDirectoryPath(), onInvalidExtensionDirectory);

        if (indexManager == null) {
            try {
                indexManager = new IndexManager(extensionIndexManager, model, onInvalidExtensionDirectory);
                indexManager.initOwner(this);
                indexManager.show();
            } catch (IOException e) {
                logger.error("Error while creating index manager window", e);
            }
        }

        if (indexManager != null) {
            indexManager.show();
            indexManager.requestFocus();
        }
    }

    private void setIndexes() {
        indexes.getChildren().setAll(model.getIndexes().stream()
                .map(index -> {
                    try {
                        return new IndexPane(extensionIndexManager, index, model);
                    } catch (IOException e) {
                        logger.error("Error while creating index pane", e);
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
