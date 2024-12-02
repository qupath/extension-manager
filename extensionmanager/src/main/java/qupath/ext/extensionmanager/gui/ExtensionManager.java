package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.index.IndexPane;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

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
