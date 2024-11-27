package qupath.ext.extensionmanager.gui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.extensionmanager.core.ExtensionIndexManager;
import qupath.ext.extensionmanager.core.savedentities.SavedIndex;
import qupath.ext.extensionmanager.gui.index.IndexPane;

import java.io.IOException;
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
    }

    @FXML
    private void onOpenExtensionDirectory(ActionEvent ignored) {
        String folder = extensionIndexManager.getExtensionFolderPath().get();

        UiUtils.openFolderInFileExplorer(folder).exceptionally(error -> {
            logger.error(String.format("Error while opening QuPath extension directory %s", folder), error);

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
}
