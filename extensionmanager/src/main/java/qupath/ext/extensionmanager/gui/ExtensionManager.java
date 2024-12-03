package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A window that displays information and controls regarding indexes and their extensions
 */
public class ExtensionManager extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionManager.class);
    private final ExtensionIndexManager extensionIndexManager;
    private final ExtensionIndexModel model;
    private IndexManager indexManager;
    @FXML
    private VBox indexes;
    @FXML
    private TitledPane manuallyInstalledExtensionsPane;
    @FXML
    private VBox manuallyInstalledExtensions;

    /**
     * Create the window.
     *
     * @param extensionIndexManager the extension index manager this window should use
     * @throws IOException when an error occurs while creating the container
     */
    public ExtensionManager(ExtensionIndexManager extensionIndexManager) throws IOException {
        this.extensionIndexManager = extensionIndexManager;
        this.model = new ExtensionIndexModel(extensionIndexManager);

        UiUtils.loadFXML(this, ExtensionManager.class.getResource("extension_manager.fxml"));

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
    }

    /**
     * Copy the provided files to the extension directory. Some error dialogs will be shown to
     * the user if errors occurs.
     * If at least a destination file already exists, the user is prompted for confirmation.
     * No confirmation dialog is prompted on success.
     *
     * @param filesToCopy the files to copy. No check is performed on those files
     */
    public void promptToCopyFilesToExtensionDirectory(List<File> filesToCopy) {
        String extensionFolder = extensionIndexManager.getExtensionFolderPath().get();
        if (extensionFolder == null) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Cannot copy files: extension folder not set."
            ).show();
            return;
        }

        Map<File, File> sourceToDestinationFiles = new HashMap<>();
        for (File file : filesToCopy) {
            try {
                sourceToDestinationFiles.put(
                        file,
                        Paths.get(extensionFolder).resolve(file.toPath().getFileName()).toFile()
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
                    String.format(
                            "%s already exist. Continue?",
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
                    String.format(
                            "Some error occurred while copying files:\n%s",
                            errors.stream()
                                    .map(Throwable::getLocalizedMessage)
                                    .collect(Collectors.joining("\n"))
                    )
            ).show();
        }
    }

    @FXML
    private void onOpenExtensionDirectory(ActionEvent ignored) {
        String folder = extensionIndexManager.getExtensionFolderPath().get();

        if (folder == null) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Cannot open the extension folder: its path was not set"
            ).show();
            return;
        }

        UiUtils.openFolderInFileExplorer(folder).exceptionally(error -> {
            logger.error("Error while opening QuPath extension directory {}", folder, error);

            Platform.runLater(() -> new Alert(
                    Alert.AlertType.ERROR,
                    String.format("Cannot open '%s':\n%s", folder, error.getLocalizedMessage())
            ).show());

            return null;
        });
    }

    @FXML
    private void onManageIndexesClicked(ActionEvent ignored) {
        if (indexManager == null) {
            try {
                indexManager = new IndexManager(extensionIndexManager, model);
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
        manuallyInstalledExtensions.getChildren().setAll(model.getManuallyInstalledJars().stream()
                .map(jarPath -> {
                    try {
                        return new ManuallyInstalledExtensionLine(jarPath);
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
